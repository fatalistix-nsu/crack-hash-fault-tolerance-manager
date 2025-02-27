package com.github.fatalistix.domain.model

import com.github.fatalistix.domain.extension.pow

data class Task(
    val id: String,
    val alphabet: String,
    val hash: String,
    val maxLength: ULong,
) {
    val size = lazy {
        var result = 0UL
        val length = alphabet.length.toULong()
        for (i in 0UL..<maxLength) {
            result += length.pow(i+1UL)
        }

        result
    }
}