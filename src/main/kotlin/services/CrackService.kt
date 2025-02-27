package com.github.fatalistix.services

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.model.Result
import com.github.fatalistix.domain.model.Task
import com.github.fatalistix.services.execution.ActorManager
import kotlinx.coroutines.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class CrackService(
    private val actorManager: ActorManager,
) {
    private val _requests = ConcurrentMutableMap<String, Result>()

    val requests: MutableMap<String, Result> = _requests

    private val scope = CoroutineScope(Dispatchers.IO)

    @OptIn(ExperimentalUuidApi::class)
    fun startCrack(alphabet: String, hash: String, maxLength: ULong): String {
        val requestId = Uuid.random().toString()
        val task = Task(requestId, alphabet, hash, maxLength)
        _requests[requestId] = Result()

        scope.launch {
            executeTask(task, requestId)
        }

        return requestId
    }

    private suspend fun executeTask(task: Task, requestId: String) {
        val result = actorManager.execute(task)
        result.onSuccess { data ->
            _requests.computeIfPresent(requestId) { _, v ->
                v.complete(data)
            }
        }
    }
}