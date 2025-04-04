package com.github.fatalistix.http.routes.external

import com.github.fatalistix.services.CrackService
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Application.registerCrackRoutes() {
    val service: CrackService = get()

    routing {
        route("/api/hash") {
            postCrack(service)
            getStatus(service)
        }
    }
}