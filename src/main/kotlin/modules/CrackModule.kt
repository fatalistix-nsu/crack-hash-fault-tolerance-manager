package com.github.fatalistix.modules

import com.github.fatalistix.services.CrackService
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import io.ktor.server.config.*
import io.ktor.util.logging.*
import org.koin.dsl.module
import kotlin.time.Duration

fun crackModule(config: ApplicationConfig, log: Logger) = module {
    val actorCount = config.property("application.actor.count").getString().toInt()
    val workerResponseTimeout = Duration.parse(config.property("application.worker.response.timeout").getString())
    val workerAcquireTimeout = Duration.parse(config.property("application.worker.acquire.timeout").getString())

    single<CrackService> {
        CrackService(
            actorManager = get()
        )
    }

    single<WorkerPool> { WorkerPool() }

    single<ActorManager> {
        ActorManager(
            workerPool = get(),
            count = actorCount,
            workerResponseTimeout = workerResponseTimeout,
            workerAcquireTimeout = workerAcquireTimeout,
            log = log,
        )
    }
}