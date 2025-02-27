package com.github.fatalistix.routes.external

import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.services.CrackService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private data class StartCrackRequest(
    val hash: String,
    val maxLength: ULong,
)

private data class StartCrackResponse(
    val requestId: String,
)

fun Route.startCrack(service: CrackService) {

    val alphabet = environment.config.property("application.alphabet").getString()

    post("/crack") {
        val request = call.receive<StartCrackRequest>()
        val requestId = service.startCrack(alphabet, request.hash, request.maxLength)
        val response = StartCrackResponse(requestId);
        call.respond(response)
    }
}