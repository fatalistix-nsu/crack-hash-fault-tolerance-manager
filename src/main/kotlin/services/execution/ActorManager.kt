package com.github.fatalistix.services.execution

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.mongo.repository.RequestResultRepository
import com.github.fatalistix.mongo.repository.TaskRepository
import com.github.fatalistix.rabbit.producer.RabbitTaskProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.slf4j.LoggerFactory

class ActorManager(
    private val producer: RabbitTaskProducer,
    private val taskRepository: TaskRepository,
    private val requestResultRepository: RequestResultRepository,
    private val taskSize: Long,
    actorCount: Int,
) {
    companion object {
        private val log = LoggerFactory.getLogger(ActorManager::class.java)!!
    }

    private val semaphore = Semaphore(actorCount)
    private val requestIdToActor = ConcurrentMutableMap<String, Actor>()

    init {
        val allReady = requestResultRepository.getAllReady()
        allReady.forEach { rr -> taskRepository.deleteAll(rr.request.id) }

        val allInProgress = requestResultRepository.getAllInProgress()
        if (allInProgress.isNotEmpty()) {
            val scope = CoroutineScope(Dispatchers.IO)
            val jobs = mutableListOf<Job>()
            allInProgress.forEach { rr ->
                jobs += scope.launch {
                    execute(rr.request)
                }
            }

            scope.launch {
                jobs.forEach { it.join() }
                scope.cancel()
            }
        }
    }

    suspend fun execute(request: Request) {
        semaphore.withPermit {
            log.info("Creating actor for request [{}]", request.id)

            val actor = Actor(
                producer,
                taskRepository,
                requestResultRepository,
                request,
                taskSize,
            )

            requestIdToActor[request.id] = actor
            actor.process()

            requestIdToActor.remove(request.id)
        }
    }

    suspend fun notifyCompleted(completedTask: CompletedTask) {
        val actor = requestIdToActor[completedTask.requestId]
        if (actor == null) {
            log.error("Actor for request [{}] and task [{}] not found", completedTask.requestId, completedTask.id)
            return
        }

        actor.notifyCompleted(completedTask)
    }
}