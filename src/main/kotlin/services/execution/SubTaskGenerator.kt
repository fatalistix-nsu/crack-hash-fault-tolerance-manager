package com.github.fatalistix.services.execution

import com.github.fatalistix.domain.model.SubTask
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Worker

internal class SubTaskGenerator (
    private val originalSubTask: SubTask,
) {
    constructor(task: Task) : this(task.toSubTask(0UL, task.size.value))

    private var currentOffset = originalSubTask.start
    private val size = originalSubTask.end

    fun hasNext(): Boolean = currentOffset < size

    fun next(worker: Worker): SubTask {
        check(hasNext()) { "Generator has generated all subtasks" }
        val performance = worker.performance
        val start = currentOffset
        currentOffset += performance
        val end = if (currentOffset < size) currentOffset else size
        val subTask = originalSubTask.cut(start, end)
        return subTask
    }
}

private fun Task.toSubTask(start: ULong, end: ULong) = SubTask(
    id, alphabet, hash, maxLength, start, end
)

private fun SubTask.cut(start: ULong, end: ULong) = SubTask(
    id, alphabet, hash, maxLength, start, end
)