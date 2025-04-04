package com.github.fatalistix.services

import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.RequestResult
import com.github.fatalistix.domain.model.RequestStatus
import com.github.fatalistix.services.execution.ActorManager
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.*
import com.mongodb.kotlin.client.MongoCollection
import com.mongodb.kotlin.client.MongoDatabase
import io.ktor.util.logging.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

class CrackService(
    private val actorManager: ActorManager,
    private val log: Logger,
    database: MongoDatabase,
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val collection: MongoCollection<RequestResult> = database.getCollection<RequestResult>("request_results")

    fun startCrack(alphabet: String, hash: String, maxLength: ULong): String {
        val result = RequestResult()
        val requestId = result.requestId.toHexString()
        val request = Request(requestId, alphabet, hash, maxLength)
        collection.insertOne(result)

        scope.launch {
            executeRequest(request)
        }

        return requestId
    }

    fun getResult(requestId: String): RequestResult? {
        val iterable = collection.find(
            eq("_id", ObjectId(requestId)),
        )

        return iterable.firstOrNull()
    }

    private suspend fun executeRequest(request: Request) {
        val chan = actorManager.execute(request)

        while (true) {
            val result = chan.receiveCatching()
            result.onSuccess { data ->
                collection.updateOne(
                    eq("_id", ObjectId(request.id)),
                    addEachToSet("data", data),
                )
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

                collection.updateOne(
                    eq("_id", ObjectId(request.id)),
                    set("status", status),
                )
                return
            }
        }
    }
}