package com.github.fatalistix.rabbit

import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.ktor.util.logging.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.time.Duration

class RabbitConnectionManager(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
    private val reconnectDelay: Duration,
    private val log: Logger,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val connectionFactory = ConnectionFactory().apply {
        host = this@RabbitConnectionManager.host
        port = this@RabbitConnectionManager.port
        username = this@RabbitConnectionManager.username
        password = this@RabbitConnectionManager.password
    }

    private val _channelFlow = MutableStateFlow<Channel?>(null)

    val channelFlow = _channelFlow.asStateFlow()

    fun start() {
        scope.launch {
            reconnectLoop()
        }
    }

    fun stop() {
        scope.cancel()
    }

    private suspend fun reconnectLoop() {
        while (true) {
            val actualChannel = _channelFlow.value
            if (actualChannel != null && actualChannel.isOpen) {
                continue
            }

            log.info("Connecting to {}: {}", host, port)
            val result = reconnect()
            if (result.isFailure) {
                log.warn("RabbitMQ is unavailable at {}:{}", host, port)
            } else {
                log.info("Connected to {}:{}", host, port)
                val channel = result.getOrThrow()
                _channelFlow.value = channel
            }

            delay(reconnectDelay)
        }
    }

    private fun reconnect() = runCatching {
        val connection = connectionFactory.newConnection()!!
        connection.createChannel()!!
    }
}