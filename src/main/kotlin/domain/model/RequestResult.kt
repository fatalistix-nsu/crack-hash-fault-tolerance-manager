package com.github.fatalistix.domain.model

data class RequestResult(
    val requestId: String,
    var status: RequestStatus = RequestStatus.IN_PROGRESS,
    val data: MutableSet<String> = mutableSetOf(),
)