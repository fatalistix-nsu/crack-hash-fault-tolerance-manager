package com.github.fatalistix.services

import co.touchlab.stately.collections.ConcurrentMutableMap
import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.Result
import com.github.fatalistix.services.execution.ActorManager
import com.github.fatalistix.util.generateId
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CrackService(
    private val actorManager: ActorManager,
    private val log: Logger,
) {
    private val _results = ConcurrentMutableMap<String, Result>()

    val results: MutableMap<String, Result> = _results

    private val scope = CoroutineScope(Dispatchers.IO)

    fun startCrack(alphabet: String, hash: String, maxLength: ULong): String {
        val requestId = generateId()
        val request = Request(requestId, alphabet, hash, maxLength)
        _results[requestId] = Result()

        scope.launch {
            executeRequest(request, requestId)
        }

        return requestId
    }

    private suspend fun executeRequest(request: Request, requestId: String) {
        val result = actorManager.execute(request)
        result.onSuccess { data ->
            results.computeIfPresent(requestId) { _, v ->
                v.complete(data)
            }
            log.info("Successfully executed request {}", request)
        }
        result.onFailure { error ->
            results.computeIfPresent(requestId) { _, v ->
                v.error()
            }
            log.error("Failed to execute request", error)
        }
    }
}