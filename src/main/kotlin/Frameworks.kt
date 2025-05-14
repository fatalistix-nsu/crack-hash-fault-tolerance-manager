package com.github.fatalistix

import com.github.fatalistix.modules.crackModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureFrameworks() {
    install(Koin) {
        slf4jLogger()
        modules(crackModule(environment.config))
    }
}
