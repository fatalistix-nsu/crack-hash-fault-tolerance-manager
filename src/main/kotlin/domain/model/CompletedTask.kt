package com.github.fatalistix.domain.model

data class CompletedTask(
    val requestId: String,
    val taskId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
    val worker: Worker,
)
