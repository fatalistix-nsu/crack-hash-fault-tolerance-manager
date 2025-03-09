package com.github.fatalistix.services.execution

import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.Worker

internal class TaskGenerator (
    private val originalTask: Task,
) {
    constructor(request: Request) : this(request.toTask(0UL, request.size.value))

    private var currentOffset = originalTask.start
    private val size = originalTask.end

    fun hasNext(): Boolean = currentOffset < size

    fun next(worker: Worker): Task {
        check(hasNext()) { "Generator has generated all tasks" }
        val performance = worker.performance
        val start = currentOffset
        currentOffset += performance
        val end = if (currentOffset < size) currentOffset else size
        val task = originalTask.cut(start, end)
        return task
    }
}

private fun Request.toTask(start: ULong, end: ULong) = Task(
    id, alphabet, hash, maxLength, start, end
)

private fun Task.cut(start: ULong, end: ULong) = Task(
    requestId, alphabet, hash, maxLength, start, end
)