package com.github.fatalistix

import com.github.fatalistix.http.exception.ConflictException
import com.github.fatalistix.http.exception.UnprocessableEntityException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureHTTP() {
    install(DefaultHeaders) {
        header("X-Engine", "Ktor") // will send this header with each response
    }
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(HttpStatusCode.UnprocessableEntity, cause.reasons)
        }
        exception<NotFoundException> { call, cause ->
            call.respondText(status = HttpStatusCode.NotFound, text = cause.message.orEmpty())
        }
        exception<ConflictException> { call, cause ->
            call.respondText(status = HttpStatusCode.Conflict, text = cause.message)
        }
        exception<UnprocessableEntityException> { call, cause ->
            call.respondText(status = HttpStatusCode.UnprocessableEntity, text = cause.message)
        }
    }
}
