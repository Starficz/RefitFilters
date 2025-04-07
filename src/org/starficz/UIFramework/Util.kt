package org.starficz.UIFramework

import com.fs.starfarer.api.Global
import org.lwjgl.opengl.GL11

// Generic Linear Map of a number from an input range to an output range
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

fun playSound(id: String, volume: Float = 1f, pitch: Float = 1f) {
    Global.getSoundPlayer().playUISound(id, pitch, volume)
}

fun drawBorder(x1: Float, y1: Float, x2: Float, y2: Float){
    GL11.glRectf(x1, y1, x2+1, y1-1)
    GL11.glRectf(x2, y1, x2+1, y2+1)
    GL11.glRectf(x1, y2, x1-1, y1-1)
    GL11.glRectf(x2, y2, x1-1, y2+1)
}
