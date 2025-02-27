package com.github.fatalistix.services.execution

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.fatalistix.domain.model.SubTask
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.services.execution.SubTaskSender.Companion.URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

private data class TaskRequest(
    val requestId: String,
    val taskId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: ULong,
    val start: ULong,
    val end: ULong,
)

class SubTaskSender {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                propertyNamingStrategy = PropertyNamingStrategies.SNAKE_CASE
            }
        }
    }

    companion object {
        const val URL = "/internal/api/worker/hash/crack/task"
    }

    suspend fun sendSubTask(subTaskId: String, subTask: SubTask, worker: Worker): Result<Unit> = runCatching {
        val request = subTask.toRequest(subTaskId)

        client.post(fullUrl(worker)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}

private fun fullUrl(worker: Worker): String =
    "http://${worker.address}:${worker.port}$URL"

private fun SubTask.toRequest(subTaskId: String) = TaskRequest(
    id, subTaskId, alphabet, hash, maxLength, start, end,
)