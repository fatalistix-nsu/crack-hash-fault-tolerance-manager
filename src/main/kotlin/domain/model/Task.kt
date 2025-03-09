package com.github.fatalistix.domain.model

data class Task(
    val requestId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: ULong,
    val start: ULong,
    val end: ULong,
)
