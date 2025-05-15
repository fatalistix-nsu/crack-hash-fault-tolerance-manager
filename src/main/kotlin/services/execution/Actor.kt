package com.github.fatalistix.services.execution

import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.RequestStatus
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.mongo.repository.RequestResultRepository
import com.github.fatalistix.mongo.repository.TaskRepository
import com.github.fatalistix.rabbit.producer.RabbitTaskProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class Actor(
    private val producer: RabbitTaskProducer,
    private val taskRepository: TaskRepository,
    private val requestResultRepository: RequestResultRepository,
    private val request: Request,
    private val taskSize: Long,
) {
    companion object {
        private val log = LoggerFactory.getLogger(Actor::class.java)!!
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    private val completedTasksChan = Channel<CompletedTask>(Channel.UNLIMITED)

    suspend fun process() {
        if (taskRepository.containsByRequestId(request.id)) {
            if (!taskRepository.containsUncompletedByRequestId(request.id)) {
                requestResultRepository.setStatus(request.id, RequestStatus.READY)
                scope.cancel()
                taskRepository.deleteAll(request.id)
                return
            }
            log.info("Recovered [{}]", request.id)
        } else {
            val result = generateAndSaveTasks()
            if (result.isFailure) {
                scope.cancel()
                requestResultRepository.setStatus(request.id, RequestStatus.ERROR)
                return
            }
        }

        val unsentTasks = taskRepository.getAllUnsent(request.id)
        val result = sendTasks(unsentTasks)
        if (result.isFailure) {
            scope.cancel()
            requestResultRepository.setStatus(request.id, RequestStatus.ERROR)
            return
        }

        val completedTaskJob = scope.launch { listenCompletedTasks() }

        log.info("Waiting for tasks to complete for request [{}]", request.id)

        completedTaskJob.join()

        log.info("Completed tasks listener joined for request [{}]", request.id)

        scope.cancel()

        log.info("Actor for request [{}] is closed", request.id)
    }

    private fun generateAndSaveTasks() = runCatching {
        val tasks = TaskGenerator(request, taskSize).generateAll()
        taskRepository.insertAll(tasks)
    }

    private suspend fun sendTasks(tasks: List<Task>) = runCatching {
        for (task in tasks) {
            val result = producer.produce(task)
            if (result.isFailure) {
                log.error("Failed to produce task ${task.id}", result.exceptionOrNull()!!)
                throw result.exceptionOrNull()!!
            }

            taskRepository.markAsSent(task.id)
        }
    }

    private suspend fun listenCompletedTasks() {
        while (true) {
            val completedTask = completedTasksChan.receive()
            requestResultRepository.addData(completedTask.requestId, completedTask.data)
            taskRepository.markAsCompleted(completedTask.id)

            completedTask.completableDeferred.complete(Unit)

            if (!taskRepository.containsUncompletedByRequestId(request.id)) {
                break
            }
        }

        log.info("Stopped to listen completed tasks for request [{}]", request.id)

        requestResultRepository.setStatus(request.id, RequestStatus.READY)
        log.info("Status for request [{}] is set to ready", request.id)

        taskRepository.deleteAll(request.id)
        log.info("Tasks are cleaned for request [{}]", request.id)
    }

    suspend fun notifyCompleted(completedTask: CompletedTask) {
        completedTasksChan.send(completedTask)
    }
}
