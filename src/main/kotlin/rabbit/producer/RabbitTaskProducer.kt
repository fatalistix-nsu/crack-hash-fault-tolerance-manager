package com.github.fatalistix.rabbit.producer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.rabbit.RabbitConnectionManager
import com.rabbitmq.client.AMQP.BasicProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.slf4j.LoggerFactory
import kotlin.time.Duration

private data class RabbitTaskMessage(
    val id: String,
    val requestId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: Long,
    val start: Long,
    val end: Long,
)

class RabbitTaskProducer(
    manager: RabbitConnectionManager,
    private val publishTimeout: Duration,
    private val exchangeName: String,
) {
    companion object {
        private val log = LoggerFactory.getLogger(RabbitTaskProducer::class.java)!!
    }

    private val channelFlow = manager.channelFlow

    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(kotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

    suspend fun produce(task: Task) = runCatching {
        val message = task.toMessage()
        val jsonMessage = objectMapper.writeValueAsString(message)
        log.debug("Sending message: {}", jsonMessage)

        withTimeout(publishTimeout) {
            while (true) {
                val channel = channelFlow.first() ?: continue
                try {
                    withContext(Dispatchers.IO) {
                        channel.basicPublish(exchangeName, "", null, jsonMessage.toByteArray())
                    }
                    return@withTimeout
                } catch (e: Exception) {
                    log.error("Error during sending task [{}]", task.id, e)
                }
            }
        }
    }
}

private fun Task.toMessage() = RabbitTaskMessage(
    id, requestId, alphabet, hash, maxLength, start, end,
)