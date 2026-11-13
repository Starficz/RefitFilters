package org.starficz.refitfilters

/** Maps this number from [minIn]..[maxIn] onto [minOut]..[maxOut], clamped to the output range. */
inline fun <reified T : Number> Number.linMap(minIn: Number, maxIn: Number, minOut: Number, maxOut: Number): T {
    val value = this.toDouble()
    val dMinIn = minIn.toDouble()
    val dMaxIn = maxIn.toDouble()
    val dMinOut = minOut.toDouble()
    val dMaxOut = maxOut.toDouble()

    val result = when {
        value > dMaxIn -> dMaxOut
        value < dMinIn -> dMinOut
        else -> dMinOut + (value - dMinIn) * (dMaxOut - dMinOut) / (dMaxIn - dMinIn)
    }
    return when (T::class) {
        Double::class -> result as T
        Float::class -> result.toFloat() as T
        Long::class -> result.toLong() as T
        Int::class -> result.toInt() as T
        Short::class -> result.toInt().toShort() as T
        Byte::class -> result.toInt().toByte() as T
        else -> throw IllegalArgumentException("Unsupported type")
    }
}
