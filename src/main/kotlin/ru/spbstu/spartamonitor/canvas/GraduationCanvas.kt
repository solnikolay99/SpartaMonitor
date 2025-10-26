package ru.spbstu.spartamonitor.canvas

import javafx.scene.canvas.Canvas
import javafx.scene.paint.Color
import ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema
import ru.spbstu.spartamonitor.colorize.ColorizeType

class GraduationCanvas : Canvas() {
    fun colorize(graduation: ColorizeType) {
        val gc = this.getGraphicsContext2D()
        val colorStep = this.width / colorSchema.size
        val countTextSteps = ((graduation.maxValue - graduation.minValue) / graduation.stepValue).toInt()
        val countSmallTextSteps = ((graduation.maxValue - graduation.minValue) / graduation.smallStepValue).toInt()
        val textStep = colorSchema.size * colorStep / countTextSteps
        val smallTextStep = colorSchema.size * colorStep / countSmallTextSteps

        gc.clearRect(0.0, 0.0, this.width, this.height)

        colorSchema.indices.forEach { i ->
            gc.fill = colorSchema[i]
            gc.fillRect(colorStep * i, 40.0, colorStep, 40.0)
        }

        gc.fill = Color.GRAY
        gc.fillText(graduation.minValue.toString(), 0.0, 25.0)
        gc.fillRect(0.0, 30.0, 2.0, 10.0)
        for (i in 1 until countTextSteps) {
            val text = if (graduation.stepValue > 1e5) {
                (graduation.minValue + graduation.stepValue * i).toString()
            } else {
                String.format("%.0f", graduation.minValue + graduation.stepValue * i)
            }
            gc.fillText(text, i * textStep - 7, 25.0)
            gc.fillRect(i * textStep - 1, 30.0, 2.0, 10.0)
        }
        gc.fillText(graduation.maxValue.toString(), countTextSteps * textStep - 25, 25.0)
        gc.fillRect(colorStep * colorSchema.size - 2, 30.0, 2.0, 10.0)

        for (i in 1 until countSmallTextSteps) {
            gc.fillRect(i * smallTextStep, 34.0, 1.0, 6.0)
        }

        gc.fillText(
            "${graduation.label}, ${graduation.units}",
            (countTextSteps * textStep) / 2 - 40,
            10.0
        )
    }
}
