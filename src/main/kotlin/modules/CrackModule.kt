package com.github.fatalistix.modules

import com.github.fatalistix.mongo.repository.RequestResultRepository
import com.github.fatalistix.mongo.repository.TaskRepository
import com.github.fatalistix.rabbit.RabbitConnectionManager
import com.github.fatalistix.rabbit.consumer.RabbitResultConsumer
import com.github.fatalistix.rabbit.producer.RabbitTaskProducer
import com.github.fatalistix.services.CrackService
import com.github.fatalistix.services.execution.ActorManager
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import com.mongodb.kotlin.client.MongoClient
import io.ktor.server.config.*
import org.koin.dsl.module
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

fun crackModule(config: ApplicationConfig) = module {
    val actorCount = config.property("application.actor.count").getString().toInt()
    val taskSize = config.property("application.task.size").getString().toLong()
    val mongoConnection = config.property("application.mongo.connection").getString()
    val mongoDatabase = config.property("application.mongo.database").getString()
    val mongoWriteTimeoutDuration = Duration.parse(config.property("application.mongo.write.timeout").getString())
    val mongoWriteTimeoutNanos = mongoWriteTimeoutDuration.inWholeNanoseconds
    val rabbitMqHost = config.property("application.rabbitmq.host").getString()
    val rabbitMqPort = config.property("application.rabbitmq.port").getString().toInt()
    val rabbitMqUsername = config.property("application.rabbitmq.username").getString()
    val rabbitMqPassword = config.property("application.rabbitmq.password").getString()
    val rabbitMqPublishTimeout = Duration.parse(config.property("application.rabbitmq.publish.timeout").getString())
    val rabbitMqReconnectDelay = Duration.parse(config.property("application.rabbitmq.reconnect.delay").getString())
    val rabbitMqTaskExchangeName = config.property("application.rabbitmq.task.exchange.name").getString()
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

    single<RequestResultRepository> {
        RequestResultRepository(
            database = database,
        )
    }

    single<TaskRepository> {
        TaskRepository(
            database = database,
        )
    }

    single<CrackService> {
        CrackService(
            actorManager = get(),
            repository = get(),
        )
    }
    single<ActorManager> {
        ActorManager(
            producer = get(),
            taskRepository = get(),
            requestResultRepository = get(),
            taskSize = taskSize,
            actorCount = actorCount,
        )
    }

    single<RabbitConnectionManager> {
        RabbitConnectionManager(
            host = rabbitMqHost,
            port = rabbitMqPort,
            username = rabbitMqUsername,
            password = rabbitMqPassword,
            reconnectDelay = rabbitMqReconnectDelay,
        )
    }

    single<RabbitTaskProducer> {
        RabbitTaskProducer(
            manager = get(),
            publishTimeout = rabbitMqPublishTimeout,
            exchangeName = rabbitMqTaskExchangeName,
        )
    }

    single<RabbitResultConsumer> {
        RabbitResultConsumer(
            connectionManager = get(),
            actorManager = get(),
            exchangeName = rabbitMqResultExchangeName,
        )
    }
}