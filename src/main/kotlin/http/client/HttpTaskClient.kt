package com.github.fatalistix.http.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.domain.model.Worker
import com.github.fatalistix.http.client.HttpTaskClient.Companion.URL
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

private data class PostTaskRequest(
    val requestId: String,
    val taskId: String,
    val alphabet: String,
    val hash: String,
    val maxLength: ULong,
    val start: ULong,
    val end: ULong,
)

internal class TaskClient {

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

    suspend fun post(taskId: String, task: Task, worker: Worker): Result<Unit> = runCatching {
        val request = task.toRequest(taskId)

        client.post(fullUrl(worker)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}

private fun fullUrl(worker: Worker): String =
    "http://${worker.address}:${worker.port}$URL"

private fun Task.toRequest(taskId: String) = PostTaskRequest(
    requestId, taskId, alphabet, hash, maxLength, start, end,
)