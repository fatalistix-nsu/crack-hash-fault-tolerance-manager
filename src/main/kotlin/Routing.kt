package com.github.fatalistix

import com.github.fatalistix.http.routes.external.PostCrackRequest
import com.github.fatalistix.http.routes.external.registerCrackRoutes
import com.github.fatalistix.validators.validateRequest
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    install(RequestValidation) {
        validate<PostCrackRequest> { request -> validateRequest(request).toValidationResult() }
    }

    registerCrackRoutes()
    routing {
        swaggerUI(path = "openapi")
    }
}

fun <T> Result<T>.toValidationResult(): ValidationResult {
    return if (this.isSuccess) {
        ValidationResult.Valid
    } else {
        val message = this.exceptionOrNull()!!.message ?: ""
        ValidationResult.Invalid(message)
    }
}
