package com.github.fatalistix.mongo.repository

import com.github.fatalistix.domain.model.Request
import com.github.fatalistix.domain.model.RequestResult
import com.github.fatalistix.domain.model.RequestStatus
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.addEachToSet
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class RequestResultEntity(
    @BsonId
    val id: ObjectId = ObjectId(),
    val status: RequestStatus = RequestStatus.IN_PROGRESS,
    val data: MutableSet<String> = mutableSetOf(),
    val alphabet: String,
    val hash: String,
    val maxLength: Long,
) {
    constructor(
        alphabet: String,
        hash: String,
        maxLength: Long,
    ) : this(
        ObjectId(),
        RequestStatus.IN_PROGRESS,
        mutableSetOf(),
        alphabet,
        hash,
        maxLength,
    )
}

class RequestResultRepository(
    database: MongoDatabase,
) {
    private val collection = database.getCollection<RequestResultEntity>("request_results")

    fun insert(alphabet: String, hash: String, maxLength: Long): RequestResult {
        val entity = RequestResultEntity(alphabet, hash, maxLength)
        collection.insertOne(entity)
        return entity.toModel()
    }

    fun findOrNull(requestId: String): RequestResult? {
        val iterable = collection.find(
            eq("_id", ObjectId(requestId)),
        )

        return iterable.firstOrNull()?.toModel()
    }

    fun addData(requestId: String, data: List<String>) {
        collection.updateOne(
            eq("_id", ObjectId(requestId)),
            addEachToSet("data", data),
        )
    }

    fun setStatus(requestId: String, status: RequestStatus) {
        collection.updateOne(
            eq("_id", ObjectId(requestId)),
            set("status", status),
        )
    }

    fun getAllInProgress(): List<RequestResult> {
        val e = collection.find(
            eq("status", RequestStatus.IN_PROGRESS),
        )

        return e.map { it.toModel() }.toList()
    }

    fun getAllReady(): List<RequestResult> {
        val e = collection.find(
            eq("status", RequestStatus.READY),
        )

        return e.map { it.toModel() }.toList()
    }
}

private fun RequestResultEntity.toModel() = RequestResult(
    status, data.toMutableSet(), this.toRequest()
)

private fun RequestResultEntity.toRequest() = Request(
    id.toHexString()!!, alphabet, hash, maxLength
)