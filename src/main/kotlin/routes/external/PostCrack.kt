package com.github.fatalistix.routes.external

import com.github.fatalistix.services.CrackService
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class PostCrackRequest(
    val hash: String,
    val maxLength: ULong,
)

data class PostCrackResponse(
    val requestId: String,
)

fun Route.postCrack(service: CrackService) {

    val alphabet = environment.config.property("application.alphabet").getString()

    post("/crack") {
        val request = call.receive<PostCrackRequest>()
        val requestId = service.startCrack(alphabet, request.hash, request.maxLength)
        val response = PostCrackResponse(requestId)
        call.respond(response)
    }
}