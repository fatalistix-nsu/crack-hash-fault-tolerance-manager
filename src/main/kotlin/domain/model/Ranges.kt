package com.github.fatalistix.domain.model

import java.util.*

data class Ranges(
    val ranges: MutableSet<Pair<ULong, ULong>> = TreeSet<Pair<ULong, ULong>>(compareBy { it.first })
)