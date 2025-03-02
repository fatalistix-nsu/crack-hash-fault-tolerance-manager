package com.github.fatalistix.routes.external

import com.github.fatalistix.domain.model.Result
import com.github.fatalistix.services.CrackService
import io.ktor.server.plugins.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

data class GetStatusResponse(
    val status: String,
    val data: List<String>?,
)

fun Route.getStatus(service: CrackService) {

    get("/status") {
        val requestId = call.queryParameters["requestId"]
        val info = service.requests[requestId] ?: throw NotFoundException("Request not found: $requestId")
        val response = info.toResponse()
        call.respond(response)
    }
}

private fun Result.toResponse(): GetStatusResponse {
    val status = isCompleted.toStatus()
    return if (isCompleted) {
        GetStatusResponse(status, data)
    } else {
        GetStatusResponse(status, null)
    }
}

private fun Boolean.toStatus(): String = if (this) "READY" else "IN_PROGRESS"