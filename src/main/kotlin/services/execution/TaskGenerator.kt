package com.github.fatalistix.services.execution

import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.util.generateId

internal class TaskGenerator (
    private val request: Request,
    private val taskSize: Long,
) {
    private var currentOffset = 0L
    private val size = request.size.value

    fun hasNext(): Boolean = currentOffset < size

    fun next(): Task {
        check(hasNext()) { "Generator has generated all tasks" }
        val start = currentOffset
        currentOffset += taskSize
        val end = if (currentOffset < size) currentOffset else size
        val task = request.cutTask(start, end)
        return task
    }

    fun generateAll(): List<Task> {
        val result = mutableListOf<Task>()
        while (hasNext()) {
            result += next()
        }
        return result
    }
}

private fun Request.cutTask(start: Long, end: Long) = Task(
    generateId(), id, alphabet, hash, maxLength, start, end
)