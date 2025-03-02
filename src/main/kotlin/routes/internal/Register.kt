package com.github.fatalistix.routes.internal

import com.github.fatalistix.services.WorkerPool
import io.ktor.http.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class RegisterRequest(
    val workerPort: UShort,
)

data class RegisterResponse(
    val workerId: String,
)

fun Route.register(workerPool: WorkerPool) {

    post("/register") {
        val request = call.receive<RegisterRequest>()
        val workerPort = request.workerPort
        val workerAddress = call.request.origin.remoteAddress
        val workerId = workerPool.register(workerAddress, workerPort) ?: throw BadRequestException("Worker $workerAddress not found")
        val response = RegisterResponse(workerId)
        call.respond(HttpStatusCode.Accepted, response)
    }
}