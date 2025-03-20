package com.github.fatalistix.services.execution

import co.touchlab.stately.collections.ConcurrentMutableSet
import com.github.fatalistix.domain.exception.NoWorkersAvailableException
import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.util.generateId
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

class Actor(
    private val workerPool: WorkerPool,
    private val request: Request,
    private val workerResponseTimeout: Duration,
    private val workerAcquireTimeout: Duration,
    private val log: Logger,
) {
    private val completedTasksChan = Channel<CompletedTask>(Channel.UNLIMITED)
    private val completedTasksFlow = completedTasksChan.receiveAsFlow()
    private val acceptedData = Channel<List<String>>(Channel.UNLIMITED)
    private val unservedTasks = Channel<Task>(Channel.UNLIMITED)
    private val client = TaskClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val rangeAggregator = RangeAggregator()
    private val rangeAggregatorMutex = Mutex()
    private val result = ConcurrentMutableSet<String>()

    fun processAsync(): Channel<List<String>> {
        val deferred = scope.async { process() }
        deferred.invokeOnCompletion { scope.cancel() }

        return acceptedData
    }

    private suspend fun process() {
        processTasks(TaskGenerator(request))

        while (true) {
            val result = unservedTasks.receiveCatching()
            if (result.isClosed) {
                break
            } else if (result.isFailure) {
                continue
            }

            val uncompletedTask = result.getOrThrow()
            processTasks(TaskGenerator(uncompletedTask))
        }
    }

    private suspend fun processTasks(generator: TaskGenerator) {
        while (generator.hasNext()) {
            val worker = workerPool.takeOrThrow(workerAcquireTimeout)
            log.info("picked worker [{}] for request [{}]", worker.id, request.id)

            val task = generator.next(worker)
            log.info("picked task [{}:{}) for request [{}] and worker [{}]", task.start, task.end, request.id, worker.id)

            scope.launch { serveTask(task, worker) }
        }
    }

    private suspend fun serveTask(task: Task, worker: Worker) {
        val taskId = generateId()

        log.info(
            "sending task [{}:{}) for request [{}] with id [{}] to worker [{}]",
            task.start,
            task.end,
            task.requestId,
            taskId,
            worker.id
        )

        val receiveResult = withTimeoutOrNull(workerResponseTimeout) {
            val deferred = scope.async { completedTasksFlow.filter { it.taskId == taskId }.first() }

            val postResult = client.post(taskId, task, worker)
            if (postResult.isFailure) {
                log.error("failed to post task [{}]", taskId, postResult.exceptionOrNull())
                deferred.cancel()
                null
            } else {
                deferred.await()
            }
        }

        if (receiveResult == null) {
            onFail(task, taskId, worker)
            return
        }

        log.info("successfully processed task [{}]", taskId)

        workerPool.release(worker)

        result.addAll(receiveResult.data)
        acceptedData.send(receiveResult.data)

        val isCompleted = rangeAggregatorMutex.withLock {
            rangeAggregator.addRange(task.start, task.end)
            rangeAggregator.isFullyProcessed(request.size.value)
        }

        if (isCompleted) {
            log.info("request [{}] is completed", request.id)
            unservedTasks.close()
            completedTasksChan.close()
            acceptedData.close()
            return
        }

        log.info("continuing request [{}]", request.id)
    }

    suspend fun notifyCompleted(completedTask: CompletedTask) {
        completedTasksChan.send(completedTask)
    }

    private suspend fun onFail(task: Task, taskId: String, worker: Worker) {
        log.error("failed with timeout or null for taskId [{}]", taskId)
        unservedTasks.send(task)
        workerPool.deregister(worker)
    }

    private suspend fun WorkerPool.takeOrThrow(timeout: Duration): Worker {
        val worker = withTimeoutOrNull(timeout) { take() }
        if (worker == null) {
            log.error("No worker found for request [{}]", request.id)
            throw NoWorkersAvailableException("No workers available")
        }

        return worker
    }
}
