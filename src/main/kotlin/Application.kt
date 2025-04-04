package com.github.fatalistix

import io.ktor.server.application.*
import io.ktor.server.netty.*

fun main(args: Array<String>) {
    EngineMain.main(args)
}

fun Application.module() {
    configureFrameworks()
    configureHTTP()
    configureMonitoring()
    configureSerialization()
    configureRouting()
    configureRabbit()
}
