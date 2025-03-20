package com.github.fatalistix.modules

import com.github.fatalistix.services.CrackService
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import com.mongodb.kotlin.client.MongoClient
import io.ktor.server.config.*
import io.ktor.util.logging.*
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun crackModule(config: ApplicationConfig, log: Logger) = module {
    val actorCount = config.property("application.actor.count").getString().toInt()
    val workerResponseTimeout = Duration.parse(config.property("application.worker.response.timeout").getString())
    val workerAcquireTimeout = Duration.parse(config.property("application.worker.acquire.timeout").getString())
    val mongoConnectionString = config.property("application.mongo.connection").getString()
    val mongoDatabaseString = config.property("application.mongo.database").getString()
    val mongoWriteTimeoutDuration = Duration.parse(config.property("application.mongo.write.timeout").getString())
    val mongoWriteTimeoutNanos = mongoWriteTimeoutDuration.inWholeNanoseconds

    val mongoWriteConcern = WriteConcern.MAJORITY
        .withJournal(true)
        .withWTimeout(mongoWriteTimeoutNanos, TimeUnit.NANOSECONDS)

    val mongoSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongoConnectionString))
        .writeConcern(mongoWriteConcern)
        .build()

    val mongoClient = MongoClient.create(mongoSettings)
    val database = mongoClient.getDatabase(mongoDatabaseString)

    single<CrackService> {
        CrackService(
            actorManager = get(),
            log = log,
            database = database,
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