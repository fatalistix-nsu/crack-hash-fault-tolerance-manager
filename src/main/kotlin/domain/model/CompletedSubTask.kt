package com.github.fatalistix.domain.model

data class CompletedSubTask(
    val id: String,
    val subTaskId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
    val worker: Worker,
)
