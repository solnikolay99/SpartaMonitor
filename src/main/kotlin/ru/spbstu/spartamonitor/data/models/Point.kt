package ru.spbstu.spartamonitor.data.models

import java.io.Serializable

data class Point(
    val x: Float,
    val y: Float
) : Serializable, Cloneable {
    public override fun clone(): Point {
        try {
            return super.clone() as Point
        } catch (e: CloneNotSupportedException) {
            throw AssertionError(e)
        }
    }
}
