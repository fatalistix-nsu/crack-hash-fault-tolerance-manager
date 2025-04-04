package com.github.fatalistix.modules

import com.github.fatalistix.rabbit.consumer.RabbitResultConsumer
import com.github.fatalistix.rabbit.producer.RabbitTaskProducer
import com.github.fatalistix.services.CrackService
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import com.mongodb.kotlin.client.MongoClient
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.config.*
import io.ktor.util.logging.*
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun crackModule(config: ApplicationConfig, log: Logger) = module {
    val actorCount = config.property("application.actor.count").getString().toInt()
    val workerResponseTimeout = Duration.parse(config.property("application.worker.response.timeout").getString())
    val workerAcquireTimeout = Duration.parse(config.property("application.worker.acquire.timeout").getString())
    val mongoConnection = config.property("application.mongo.connection").getString()
    val mongoDatabase = config.property("application.mongo.database").getString()
    val mongoWriteTimeoutDuration = Duration.parse(config.property("application.mongo.write.timeout").getString())
    val mongoWriteTimeoutNanos = mongoWriteTimeoutDuration.inWholeNanoseconds
    val rabbitMqHost = config.property("application.rabbitmq.host").getString()
    val rabbitMqPort = config.property("application.rabbitmq.port").getString().toInt()
    val rabbitMqUsername = config.property("application.rabbitmq.username").getString()
    val rabbitMqPassword = config.property("application.rabbitmq.password").getString()
    val rabbitMqTaskExchangeName = config.property("application.rabbitmq.task.exchange.name").getString()
    val rabbitMqTaskMessageExpiration = Duration.parse(config.property("application.rabbitmq.task.message.expiration").getString())
    val rabbitMqResultExchangeName = config.property("application.rabbitmq.result.exchange.name").getString()

    val mongoWriteConcern = WriteConcern.MAJORITY
        .withJournal(true)
        .withWTimeout(mongoWriteTimeoutNanos, TimeUnit.NANOSECONDS)

    val mongoSettings = MongoClientSettings.builder()
        .applyConnectionString(ConnectionString(mongoConnection))
        .writeConcern(mongoWriteConcern)
        .build()

    val mongoClient = MongoClient.create(mongoSettings)
    val database = mongoClient.getDatabase(mongoDatabase)

    val rabbitMqFactory = ConnectionFactory().apply {
        host = rabbitMqHost
        port = rabbitMqPort
        username = rabbitMqUsername
        password = rabbitMqPassword
    }
    val rabbitMqConnection = rabbitMqFactory.newConnection()!!
    val rabbitMqChannel = rabbitMqConnection.createChannel()!!

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
            producer = get(),
            workerPool = get(),
            count = actorCount,
            workerResponseTimeout = workerResponseTimeout,
            workerAcquireTimeout = workerAcquireTimeout,
            log = log,
        )
    }

    single<RabbitTaskProducer> {
        RabbitTaskProducer(
            channel = rabbitMqChannel,
            exchangeName = rabbitMqTaskExchangeName,
            messageExpiration = rabbitMqTaskMessageExpiration,
            log = log,
        )
    }

    single<RabbitResultConsumer> {
        RabbitResultConsumer(
            channel = rabbitMqChannel,
            exchangeName = rabbitMqResultExchangeName,
            actorManager = get(),
            workerPool = get(),
        )
    }
}