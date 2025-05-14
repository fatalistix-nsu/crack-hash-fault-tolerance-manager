package com.github.fatalistix.domain.model

import kotlinx.coroutines.CompletableDeferred

data class CompletedTask(
    val id: String,
    val requestId: String,
    val start: ULong,
    val end: ULong,
    val data: List<String>,
    val completableDeferred: CompletableDeferred<Unit>,
)
