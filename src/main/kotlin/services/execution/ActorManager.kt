package com.github.fatalistix.services.execution

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.model.CompletedSubTask
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.services.WorkerPool
import io.ktor.util.logging.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.time.Duration

class ActorManager(
    private val workerPool: WorkerPool,
    count: Int,
    private val responseTimeout: Duration,
    private val log: Logger,
) {
    private val semaphore = Semaphore(count)
    private val taskIdToActor = ConcurrentMutableMap<String, Actor>()

    suspend fun execute(task: Task): Result<List<String>> = runCatching {
        semaphore.withPermit {
            val actor = Actor(workerPool, task, responseTimeout, log)
            taskIdToActor[task.id] = actor
            actor.process()
        }
    }.also { taskIdToActor.remove(task.id) }

    suspend fun notifyCompleted(completedSubTask: CompletedSubTask) {
        taskIdToActor[completedSubTask.id]?.notifyCompleted(completedSubTask)
    }
}