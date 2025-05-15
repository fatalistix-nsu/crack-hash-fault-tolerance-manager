package com.github.fatalistix.services

import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.mongo.repository.RequestResultRepository
import com.github.fatalistix.services.execution.ActorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CrackService(
    private val actorManager: ActorManager,
    private val repository: RequestResultRepository,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startCrack(alphabet: String, hash: String, maxLength: Long): String {
        val requestResult = repository.insert(alphabet, hash, maxLength)
        val requestId = requestResult.request.id
        val request = Request(requestId, alphabet, hash, maxLength)

        scope.launch {
            executeRequest(request)
        }

        return requestId
    }

    fun getResult(requestId: String) = repository.findOrNull(requestId)

    private suspend fun executeRequest(request: Request) {
        actorManager.execute(request)
    }
}
