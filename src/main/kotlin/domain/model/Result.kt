package com.github.fatalistix.domain.model

class Result private constructor(
    val isCompleted: Boolean,
    val data: List<String>,
) {
    constructor() : this(false, listOf())

    fun complete(data: List<String>) = Result(true, data)
}