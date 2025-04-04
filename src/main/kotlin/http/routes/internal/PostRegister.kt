package com.github.fatalistix.http.routes.internal

import com.github.fatalistix.http.exception.ConflictException
import com.github.fatalistix.services.WorkerPool
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class PostRegisterRequest(
    val workerPort: UShort,
)

data class PostRegisterResponse(
    val workerId: String,
)

fun Route.postRegister(workerPool: WorkerPool) {

    post("/register") {
        val request = call.receive<PostRegisterRequest>()
        val workerPort = request.workerPort
        val workerAddress = call.request.origin.remoteAddress
        val workerId = workerPool.register(workerAddress, workerPort) ?: throw ConflictException("Worker $workerAddress is already registered")
        val response = PostRegisterResponse(workerId)
        call.respond(HttpStatusCode.Accepted, response)
    }
}