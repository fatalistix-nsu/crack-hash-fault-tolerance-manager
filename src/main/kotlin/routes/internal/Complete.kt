package com.github.fatalistix.routes.internal

import com.github.fatalistix.domain.model.CompletedSubTask
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

private data class CompleteRequest(
    val requestId: String,
    val taskId: String,
    val workerId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
)

fun Route.complete(actorManager: ActorManager, workerPool: WorkerPool) {

    patch("/request") {
        val request = call.receive<CompleteRequest>()
        val worker = workerPool.get(request.workerId) ?: throw BadRequestException("Worker ${request.workerId} not found")
        val completedSubTask = request.toModel(worker)
        actorManager.notifyCompleted(completedSubTask)
        call.respond(HttpStatusCode.Accepted, null)
    }
}

private fun CompleteRequest.toModel(worker: Worker) = CompletedSubTask(
    requestId, taskId, start, end, data, worker
)
