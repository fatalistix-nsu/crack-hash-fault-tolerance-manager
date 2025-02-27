package com.github.fatalistix.domain.model

import com.github.fatalistix.util.generateId

data class Worker(
    val address: String,
    val port: UShort,
) {
    private companion object {
        const val PERFORMANCE = 1_000_000_UL
    }

    val performance = PERFORMANCE

    val id = generateId()
}