package com.github.fatalistix.domain.extension

fun ULong.pow(exp: ULong): ULong {
    var result = 1UL
    var base = this
    var expTmp = exp
    while (expTmp != 0UL) {
        if (expTmp and 1UL != 0UL) {
            result *= base
        }
        expTmp = expTmp shr 1
        base *= base
    }
    return result
}
