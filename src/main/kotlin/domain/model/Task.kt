package com.github.fatalistix.domain.model

data class Task(
    val id: String,
    val requestId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: Long,
    val start: Long,
    val end: Long,
)
