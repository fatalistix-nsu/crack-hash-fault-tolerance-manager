package com.github.fatalistix.rabbit.consumer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.rabbit.RabbitConnectionManager
import com.github.fatalistix.services.execution.ActorManager
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

private data class RabbitResultMessage(
    val requestId: String,
    val taskId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
)

class RabbitResultConsumer(
    private val connectionManager: RabbitConnectionManager,
    private val actorManager: ActorManager,
    private val exchangeName: String,
) {
    companion object {
        const val QUEUE_NAME = "rabbit-result-queue"
        val log = LoggerFactory.getLogger(RabbitResultConsumer::class.java)!!
    }

    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(kotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            connectionManager.channelFlow.collect { channel ->
                if (channel == null || !channel.isOpen) {
                    log.warn("Received closed channel")
                    return@collect
                }

                log.info("Received new channel, rebinding")

                channel.queueDeclare(QUEUE_NAME, true, false, false, null)
                channel.queueBind(QUEUE_NAME, exchangeName, "")

                val consumer = buildConsumer(channel)

                channel.basicConsume(QUEUE_NAME, false, consumer)
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun buildConsumer(channel: Channel) = object : DefaultConsumer(channel) {

        override fun handleDelivery(
            consumerTag: String,
            envelope: Envelope,
            properties: AMQP.BasicProperties,
            body: ByteArray
        ) {
            val message = objectMapper.readValue<RabbitResultMessage>(body)
            val completedTask = message.toModel()

            log.info("Received completed task [{}]", completedTask)

            scope.launch {
                actorManager.notifyCompleted(completedTask)
                completedTask.completableDeferred.await()
                channel.basicAck(envelope.deliveryTag, false)
            }
        }
    }
}

private fun RabbitResultMessage.toModel() = CompletedTask(
    taskId, requestId, start, end, data, CompletableDeferred()
)
