package com.github.fatalistix.rabbit.consumer

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.fatalistix.domain.model.CompletedTask
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.WorkerPool
import com.github.fatalistix.services.execution.ActorManager
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private data class RabbitResultMessage(
    val requestId: String,
    val taskId: String,
    val workerId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
)

class RabbitResultConsumer(
    private val channel: Channel,
    exchangeName: String,
    actorManager: ActorManager,
    workerPool: WorkerPool,
) {
    private val objectMapper = jacksonObjectMapper().apply {
        registerModule(kotlinModule())
        enable(SerializationFeature.INDENT_OUTPUT)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
    }
    private val scope = CoroutineScope(Dispatchers.IO)
    private val consumer = object : DefaultConsumer(channel) {

        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray
        ) {
            val message = objectMapper.readValue<RabbitResultMessage>(body)
            val worker = workerPool.get(message.workerId) ?: return
            val completedTask = message.toModel(worker)
            scope.launch {
                actorManager.notifyCompleted(completedTask)
            }
        }
    }

    companion object {
        const val QUEUE_NAME = "rabbit-result-queue"
    }

    init {
        channel.queueDeclare(QUEUE_NAME, true, false, false, null)
        channel.queueBind(QUEUE_NAME, exchangeName, "")
    }

    fun start() {
        scope.launch {
            channel.basicConsume(QUEUE_NAME, true, consumer)
        }
    }

    fun stop() {
        scope.cancel()
    }
}

private fun RabbitResultMessage.toModel(worker: Worker) = CompletedTask(
    requestId, taskId, start, end, data, worker
)
