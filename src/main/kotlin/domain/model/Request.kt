package com.github.fatalistix.domain.model

import com.github.fatalistix.domain.extension.pow

data class Request(
    val id: String,
    val alphabet: String,
    val hash: String,
    val maxLength: Long,
) {
    val size = lazy {
        var result = 0L
        val length = alphabet.length.toLong()
        for (i in 0L..<maxLength) {
            result += length.pow(i+1L)
        }

        result
    }
}