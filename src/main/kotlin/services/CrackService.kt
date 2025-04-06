package com.github.fatalistix.services

import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.RequestStatus
import com.github.fatalistix.mongo.repository.RequestResultRepository
import com.github.fatalistix.services.execution.ActorManager
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.*
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

class CrackService(
    private val actorManager: ActorManager,
    private val repository: RequestResultRepository,
    private val log: Logger,
) {
    private val scope = CoroutineScope(Dispatchers.IO)


    fun startCrack(alphabet: String, hash: String, maxLength: ULong): String {
        val requestResult = repository.insert()
        val requestId = requestResult.requestId
        val request = Request(requestId, alphabet, hash, maxLength)

        scope.launch {
            executeRequest(request)
        }

        return requestId
    }

    fun getResult(requestId: String) = repository.findOrNull(requestId)

    private suspend fun executeRequest(request: Request) {
        val chan = actorManager.execute(request)

        while (true) {
            val result = chan.receiveCatching()
            result.onSuccess { data ->
                repository.addData(request.id, data)
                log.info("Added result words {} to request {}", data, request)
            }
            result.onClosed { error ->
                val status = if (error == null) {
                    log.info("Finished request {}", request)
                    RequestStatus.READY
                } else {
                    log.error("Failed to execute request {}", request, error)
                    RequestStatus.ERROR
                }

                repository.setStatus(request.id, status)

                return
            }
        }
    }
}
