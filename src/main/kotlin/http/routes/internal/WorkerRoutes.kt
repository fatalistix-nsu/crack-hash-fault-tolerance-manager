package com.github.fatalistix.routes.internal

import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.ktor.ext.get

fun Application.registerWorkerRoutes() {
    val workerPool: WorkerPool = get()
    val actorManager: ActorManager = get()

    routing {
        route("/internal/api/worker/hash/crack") {
            postRegister(workerPool)
            patchRequest(actorManager, workerPool)
        }
    }
}