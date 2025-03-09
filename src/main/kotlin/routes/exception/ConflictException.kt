package com.github.fatalistix.routes.exception

import io.ktor.server.plugins.*

class ConflictException(
    override val message: String,
    override val cause: Throwable? = null
) : BadRequestException(message, cause)