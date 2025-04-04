package com.github.fatalistix

import com.github.fatalistix.rabbit.consumer.RabbitResultConsumer
import io.ktor.server.application.*
import org.koin.ktor.ext.inject

fun Application.configureRabbit() {
    val resultConsumer: RabbitResultConsumer by inject()

    monitor.subscribe(ApplicationStarted) {
        resultConsumer.start()
    }

    monitor.subscribe(ApplicationStopping) {
        resultConsumer.stop()
    }
}