package com.github.fatalistix

import com.github.fatalistix.rabbit.RabbitConnectionManager
import com.github.fatalistix.rabbit.consumer.RabbitResultConsumer
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.configureRabbit() {
    val connectionManager: RabbitConnectionManager by inject()
    val resultConsumer: RabbitResultConsumer by inject()

    monitor.subscribe(ApplicationStarted) {
        connectionManager.start()
        resultConsumer.start()
    }

    monitor.subscribe(ApplicationStopping) {
        connectionManager.stop()
        resultConsumer.stop()
    }
}