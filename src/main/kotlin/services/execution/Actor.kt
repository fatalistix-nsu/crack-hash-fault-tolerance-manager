package com.github.fatalistix.services.execution

import co.touchlab.stately.collections.ConcurrentMutableSet
import com.github.fatalistix.domain.model.CompletedSubTask
import com.github.fatalistix.domain.model.SubTask
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.util.generateId
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

class Actor(
    private val workerPool: WorkerPool,
    private val task: Task,
    private val responseTimeout: Duration,
    private val log: Logger,
) {
    private val completedParts = MutableSharedFlow<CompletedSubTask>()
    private val unservedParts = Channel<SubTask>(Channel.UNLIMITED)
    private val sender = SubTaskSender()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val rangeAggregator = RangeAggregator()
    private val rangeAggregatorMutex = Mutex()
    private val result = ConcurrentMutableSet<String>()

    suspend fun process(): List<String> {
        val generator = SubTaskGenerator(task)
        while (generator.hasNext()) {
            val worker = workerPool.take()
            log.info("picked worker [{}] for task [{}]", worker.id, task.id)

            val subTask = generator.next(worker)
            log.info("picked subtask [{}:{}) for task [{}] and worker [{}]", subTask.start, subTask.end, task.id, worker.id)

            scope.launch { serveSubTask(subTask, worker) }
        }

        while (true) {
            val result = unservedParts.receiveCatching()
            if (result.isClosed) {
                break
            } else if (result.isFailure) {
                continue
            }

            val uncompletedSubTask = result.getOrThrow()
            val uncompletedGenerator = SubTaskGenerator(uncompletedSubTask)

            while (uncompletedGenerator.hasNext()) {
                val worker = workerPool.take()
                val subTask = uncompletedGenerator.next(worker)
                scope.launch { serveSubTask(subTask, worker) }
            }
        }

        scope.cancel()

        return result.toList()
    }

    private suspend fun serveSubTask(subTask: SubTask, worker: Worker) {
        val subTaskId = generateId()

        log.info(
            "sending subtask [{}:{}) with task_id [{}] and subtask_id [{}] to worker [{}]",
            subTask.start,
            subTask.end,
            subTask.id,
            subTaskId,
            worker.id
        )

        val receiveResult = withTimeoutOrNull(responseTimeout) {
            val deferred = async { completedParts.filter { it.subTaskId == subTaskId }.first() }

            val sendResult = sender.sendSubTask(subTaskId, subTask, worker)
            if (sendResult.isFailure) {
                log.error("failed to send subtask [{}]", subTaskId, sendResult.exceptionOrNull())
                deferred.cancel()
                null
            } else {
                deferred.await()
            }
        }

        if (receiveResult == null) {
            onFail(subTask, subTaskId, worker)
            return
        }

        log.info("successfully processed subtask with subTaskId [{}]", subTaskId)

        workerPool.release(worker)

        result.addAll(receiveResult.data)

        val isCompleted = rangeAggregatorMutex.withLock {
            rangeAggregator.addRange(subTask.start, subTask.end)
            rangeAggregator.isFullyProcessed(task.size.value)
        }

        if (isCompleted) {
            log.info("task [{}] is completed", task.id)
            unservedParts.close()
            return
        }

        log.info("continuing task [{}]", task.id)
    }

    suspend fun notifyCompleted(completedSubTask: CompletedSubTask) {
        completedParts.emit(completedSubTask)
    }

    private suspend fun onFail(subTask: SubTask, subTaskId: String, worker: Worker) {
        log.error("failed with timeout or null for subTaskId [{}]", subTaskId)
        unservedParts.send(subTask)
        workerPool.deregister(worker)
    }
}
