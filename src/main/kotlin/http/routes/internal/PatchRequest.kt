package com.github.fatalistix.http.routes.internal

import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

data class PatchRequestRequest(
    val requestId: String,
    val taskId: String,
    val workerId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
)

fun Route.patchRequest(actorManager: ActorManager, workerPool: WorkerPool) {

    patch("/request") {
        val request = call.receive<PatchRequestRequest>()
        val worker = workerPool.get(request.workerId) ?: throw NotFoundException("Worker ${request.workerId} not found")
        val completedTask = request.toModel(worker)
        actorManager.notifyCompleted(completedTask)
        call.respond(HttpStatusCode.Accepted, null)
    }
}

private fun PatchRequestRequest.toModel(worker: Worker) = CompletedTask(
    requestId, taskId, start, end, data, worker
)
