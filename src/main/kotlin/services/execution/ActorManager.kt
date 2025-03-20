package com.github.fatalistix.services.execution

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.services.WorkerPool
import io.ktor.util.logging.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.sync.Semaphore
import kotlin.time.Duration

class ActorManager(
    private val workerPool: WorkerPool,
    count: Int,
    private val workerResponseTimeout: Duration,
    private val workerAcquireTimeout: Duration,
    private val log: Logger,
) {
    private val semaphore = Semaphore(count)
    private val requestIdToActor = ConcurrentMutableMap<String, Actor>()

    suspend fun execute(request: Request): Channel<List<String>> {
        semaphore.acquire()
        val actor = Actor(workerPool, request, workerResponseTimeout, workerAcquireTimeout, log)
        requestIdToActor[request.id] = actor
        val chan = actor.processAsync()
        chan.invokeOnClose {
            semaphore.release()
            requestIdToActor.remove(request.id)
        }
        return chan
    }

    suspend fun notifyCompleted(completedTask: CompletedTask) {
        requestIdToActor[completedTask.requestId]?.notifyCompleted(completedTask)
    }
}