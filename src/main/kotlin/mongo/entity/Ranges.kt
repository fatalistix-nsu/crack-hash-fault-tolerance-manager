package com.github.fatalistix.mongo.entity

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.*

data class Ranges(
    @BsonId
    val id: ObjectId = ObjectId(),
    val ranges: MutableSet<Pair<ULong, ULong>> = TreeSet<Pair<ULong, ULong>>(compareBy { it.first })
)