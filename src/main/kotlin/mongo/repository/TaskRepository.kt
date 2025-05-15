package com.github.fatalistix.mongo.repository

import com.github.fatalistix.domain.model.Task
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

enum class TaskStatusEntity {
    CREATED,
    SENT,
    COMPLETED,
}

data class TaskEntity(
    @BsonId
    val id: ObjectId = ObjectId(),
    val taskId: String,
    val requestId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: Long,
    val start: Long,
    val end: Long,
    val status: TaskStatusEntity,
)

class TaskRepository(
    database: MongoDatabase,
) {
    private val collection = database.getCollection<TaskEntity>("tasks")

    fun insertAll(tasks: List<Task>) {
        val taskEntities = tasks.map { it.toEntity() }
        collection.insertMany(taskEntities)
    }

    fun deleteAll(requestId: String) {
        collection.deleteMany(eq("requestId", requestId))
    }

    fun containsByRequestId(requestId: String): Boolean {
        return collection.find(eq("requestId", requestId)).firstOrNull() != null
    }

    fun containsUncompletedByRequestId(requestId: String): Boolean {
        return collection.find(
            and(
                eq("requestId", requestId),
                not(eq("status", TaskStatusEntity.COMPLETED)),
            )
        ).firstOrNull() != null
    }

    fun markAsCompleted(taskId: String) {
        collection.updateOne(
            eq("taskId", taskId),
            set("status", TaskStatusEntity.COMPLETED),
        )
    }

    fun markAsSent(taskId: String) {
        collection.updateOne(
            eq("taskId", taskId),
            set("status", TaskStatusEntity.SENT),
        )
    }

    fun getAllUnsent(requestId: String): List<Task> {
        return collection.find(
            and(
                eq("requestId", requestId),
                eq("status", TaskStatusEntity.CREATED),
            )
        ).map { it.toModel() }.toList()
    }
}

private fun Task.toEntity() = TaskEntity(
    ObjectId(), id, requestId, alphabet, hash, maxLength, start, end, TaskStatusEntity.CREATED
)

private fun TaskEntity.toModel() = Task(
    taskId, requestId, alphabet, hash, maxLength, start, end
)
