package com.github.fatalistix.domain.model

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class RequestResult(
    @BsonId
    val requestId: ObjectId = ObjectId(),
    var status: RequestStatus = RequestStatus.IN_PROGRESS,
    val data: MutableSet<String> = mutableSetOf(),
)