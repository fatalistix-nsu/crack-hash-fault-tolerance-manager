package com.github.fatalistix

import com.github.fatalistix.routes.external.registerCrackRoutes
import com.github.fatalistix.routes.internal.registerWorkerRoutes
import io.ktor.server.application.*
import io.ktor.server.plugins.requestvalidation.*

fun Application.configureRouting() {
    install(RequestValidation) {

    }

    registerCrackRoutes()
    registerWorkerRoutes()
}
