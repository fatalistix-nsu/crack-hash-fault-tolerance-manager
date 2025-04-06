package com.github.fatalistix.mongo.repository

import com.github.fatalistix.domain.model.RequestResult
import com.github.fatalistix.domain.model.RequestStatus
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.addEachToSet
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoDatabase
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

private data class RequestResultEntity(
    @BsonId
    val requestId: ObjectId = ObjectId(),
    var status: RequestStatus = RequestStatus.IN_PROGRESS,
    val data: MutableSet<String> = mutableSetOf(),
)

class RequestResultRepository(
    database: MongoDatabase,
) {
    private val collection = database.getCollection<RequestResultEntity>("request_results")

    fun insert(): RequestResult {
        val entity = RequestResultEntity()
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
}

private fun RequestResultEntity.toModel() = RequestResult(
    requestId.toHexString()!!, status, data.toMutableSet()
)