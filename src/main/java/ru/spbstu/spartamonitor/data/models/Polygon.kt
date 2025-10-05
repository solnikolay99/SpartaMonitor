package ru.spbstu.spartamonitor.data.models

import java.awt.geom.Line2D

data class Polygon(
    val points: MutableList<Point> = mutableListOf(),
    val lines: MutableList<Line2D> = mutableListOf()
) {
    fun addPoint(point: Point) {
        if (!points.isEmpty()) {
            lines.add(
                Line2D.Float(
                    points.last().x,
                    points.last().y,
                    point.x,
                    point.y
                )
            )
        }
        points.add(point)
    }

    val borderPoints: FloatArray
        get() {
            val borderPoints = floatArrayOf(
                Float.MAX_VALUE,
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                Float.MIN_VALUE
            )
            points.forEach { point ->
                if (point.x < borderPoints[0]) {
                    borderPoints[0] = point.x
                } else if (point.x > borderPoints[2]) {
                    borderPoints[2] = point.x
                }

                if (point.y < borderPoints[1]) {
                    borderPoints[1] = point.y
                } else if (point.y > borderPoints[3]) {
                    borderPoints[3] = point.y
                }
            }
            return borderPoints
        }
}
