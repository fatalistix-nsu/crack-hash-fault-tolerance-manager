package com.github.fatalistix.validators

import com.github.fatalistix.http.routes.external.PostCrackRequest

fun validateRequest(request: PostCrackRequest) = runCatching {
    if (request.maxLength <= 0L) {
        throw IllegalArgumentException("max length must be greater than 0")
    }

    val md5Regex = Regex("^[a-fA-F0-9]{32}$")
    if (!md5Regex.matches(request.hash)) {
        throw IllegalArgumentException("received string is not md5 hash")
    }
}