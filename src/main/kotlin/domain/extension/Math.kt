package com.github.fatalistix.domain.extension

fun Long.pow(exp: Long): Long {
    var result = 1L
    var base = this
    var expTmp = exp
    while (expTmp != 0L) {
        if (expTmp and 1L != 0L) {
            result *= base
        }
        expTmp = expTmp shr 1
        base *= base
    }
    return result
}
