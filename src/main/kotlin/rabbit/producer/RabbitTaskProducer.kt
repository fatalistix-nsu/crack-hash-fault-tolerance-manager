package com.github.fatalistix.rabbit.producer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Worker
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.Channel
import io.ktor.util.logging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration

private data class RabbitTaskMessage(
    val requestId: String,
    val taskId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: ULong,
    val start: ULong,
    val end: ULong,
)

class RabbitTaskProducer(
    private val channel: Channel,
    private val exchangeName: String,
    private val messageExpiration: Duration,
    private val log: Logger,
) {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(kotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

    suspend fun send(taskId: String, task: Task, worker: Worker): Result<Unit> {
        val message = task.toMessage(taskId)
        val jsonMessage = objectMapper.writeValueAsString(message)
        log.debug("Sending message: {}", jsonMessage)

        val props = BasicProperties.Builder().apply {
            expiration(messageExpiration.inWholeMilliseconds.toString())
        }.build()

        return runCatching {
            withContext(Dispatchers.IO) {
                channel.basicPublish(exchangeName, worker.id, props, jsonMessage.toByteArray())
            }
        }
    }
}

private fun Task.toMessage(taskId: String) = RabbitTaskMessage(
    requestId, taskId, alphabet, hash, maxLength, start, end,
)