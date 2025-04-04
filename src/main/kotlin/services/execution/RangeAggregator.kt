package com.github.fatalistix.services.execution

import java.util.TreeSet

internal class RangeAggregator(
    private val ranges: MutableSet<Pair<ULong, ULong>> = TreeSet<Pair<ULong, ULong>>(compareBy { it.first })
) {
    fun addRange(start: ULong, end: ULong) {
        var newStart = start
        var newEnd = end

        val toRemove = mutableSetOf<Pair<ULong, ULong>>()
        for ((s, e) in ranges) {
            if (e < newStart || newEnd < s) {
                continue
            }
            newStart = minOf(newStart, s)
            newEnd = maxOf(newEnd, e)
            toRemove.add(s to e)
        }
        ranges.removeAll(toRemove)
        ranges.add(newStart to newEnd)
    }

    fun isFullyProcessed(size: ULong): Boolean {
        return ranges.size == 1 && ranges.first() == (0uL to size)
    }
}