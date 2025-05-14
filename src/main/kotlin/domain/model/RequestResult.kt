package com.github.fatalistix.domain.model

data class RequestResult(
    var status: RequestStatus = RequestStatus.IN_PROGRESS,
    val data: MutableSet<String> = mutableSetOf(),
    val request: Request,
)