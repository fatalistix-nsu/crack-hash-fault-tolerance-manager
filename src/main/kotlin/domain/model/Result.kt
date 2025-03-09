package com.github.fatalistix.domain.model

class Result private constructor(
    val status: RequestStatus,
    val data: List<String>,
) {
    constructor() : this(RequestStatus.IN_PROGRESS, listOf())

    fun complete(data: List<String>) = Result(RequestStatus.READY, data)

    fun error() = Result(RequestStatus.ERROR, data)
}