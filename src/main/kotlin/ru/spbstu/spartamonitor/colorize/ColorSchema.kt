package ru.spbstu.spartamonitor.colorize

import javafx.scene.paint.Color

object ColorSchema {
    var colorSchema = mutableListOf<Color>()

    init {
        colorSchema.add(Color.RED)
        colorSchema.add(Color.GREENYELLOW)
        colorSchema.add(Color.BLUE)
        colorSchema.add(Color.GOLD)
        colorSchema.addAll(linearGradient(Color.BLUEVIOLET, Color.MEDIUMSPRINGGREEN, 116))
        colorSchema.addAll(linearGradient(Color.MEDIUMSPRINGGREEN, Color.ORANGE, 120))
        colorSchema.addAll(linearGradient(Color.ORANGE, Color.MAGENTA, 120))
    }

    private fun linearGradient(startColor: Color, endColor: Color, countColors: Int): MutableList<Color> {
        val sRed = (startColor.red * 255).toInt()
        val eRed = (endColor.red * 255).toInt()
        val sGreen = (startColor.green * 255).toInt()
        val eGreen = (endColor.green * 255).toInt()
        val sBlue = (startColor.blue * 255).toInt()
        val eBlue = (endColor.blue * 255).toInt()

        return (0..countColors).map { i ->
            var red = (sRed + (i.toFloat() / (countColors - 1)) * (eRed - sRed)).toInt()
            var green = (sGreen + (i.toFloat() / (countColors - 1)) * (eGreen - sGreen)).toInt()
            var blue = (sBlue + (i.toFloat() / (countColors - 1)) * (eBlue - sBlue)).toInt()
            if (red < 0) red = 0
            if (green < 0) green = 0
            if (blue < 0) blue = 0
            if (red > 255) red = 255
            if (green > 255) green = 255
            if (blue > 255) blue = 255
            Color.rgb(red, green, blue)
        }.toMutableList()
    }
}
