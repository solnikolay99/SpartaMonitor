package ru.spbstu.spartamonitor.screener

import javafx.embed.swing.SwingFXUtils
import javafx.scene.canvas.Canvas
import javafx.scene.chart.Chart
import javafx.scene.image.WritableImage
import java.awt.Color
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import kotlin.math.max

object Screener {
    @JvmStatic
    fun combineFullScene(
        mainImage: RenderedImage,
        graduationImage: RenderedImage,
        chartImage: RenderedImage
    ): BufferedImage {
        val xOffset = 20
        val yOffset = 20
        val width = max(mainImage.width, mainImage.width) + 3 * xOffset + chartImage.width
        val height = max(mainImage.height + graduationImage.height, chartImage.height) + 3 * yOffset

        val outImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val gc = outImage.createGraphics()
        val oldColor = gc.color
        gc.paint = Color.LIGHT_GRAY
        gc.fillRect(0, 0, width, height)
        gc.color = oldColor
        gc.drawImage(mainImage as BufferedImage, null, xOffset, yOffset)
        gc.drawImage(graduationImage as BufferedImage, null, xOffset, mainImage.height + 2 * yOffset)
        gc.drawImage(
            chartImage as BufferedImage, null, width - chartImage.width - xOffset,
            (height - chartImage.height - (1.5 * yOffset).toInt()) / 2
        )
        gc.dispose()

        return outImage
    }

    @JvmStatic
    fun getImageFromCanvas(canvas: Canvas): RenderedImage {
        val writableImage = WritableImage(canvas.width.toInt(), canvas.height.toInt())
        canvas.snapshot(null, writableImage)
        return SwingFXUtils.fromFXImage(writableImage, null)
    }

    @JvmStatic
    fun getImageFromCanvas(chart: Chart): RenderedImage {
        val writableImage = WritableImage(chart.width.toInt(), chart.height.toInt())
        chart.snapshot(null, writableImage)
        return SwingFXUtils.fromFXImage(writableImage, null)
    }
}
