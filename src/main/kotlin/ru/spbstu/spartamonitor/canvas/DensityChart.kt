package ru.spbstu.spartamonitor.canvas

import config.Config
import javafx.beans.NamedArg
import javafx.collections.ObservableList
import javafx.scene.chart.Axis
import javafx.scene.chart.LineChart
import ru.spbstu.spartamonitor.calculate.Calculation
import ru.spbstu.spartamonitor.colorize.ColorizeType
import ru.spbstu.spartamonitor.colorize.ColorizeType.*
import ru.spbstu.spartamonitor.data.FrameGenerator
import kotlin.math.max

class DensityChart : LineChart<String, Number> {
    private var curColorizeType: ColorizeType = DENSITY_STATIC

    constructor(
        @NamedArg("xAxis") xAxis: Axis<String>,
        @NamedArg("yAxis") yAxis: Axis<Number>,
        @NamedArg("data") data: ObservableList<Series<String, Number>>
    ) : super(xAxis, yAxis, data)

    constructor(@NamedArg("xAxis") xAxis: Axis<String>, @NamedArg("yAxis") yAxis: Axis<Number>) : super(
        xAxis,
        yAxis
    )

    fun drawIteration(frame: FrameGenerator.Frame, colorizeType: ColorizeType) {
        if (curColorizeType != colorizeType) {
            this.data.clear()
            curColorizeType = colorizeType
        }
        if (colorizeType == DENSITY_STATIC_DIF || colorizeType == DENSITY_DYNAMIC_DIF || colorizeType == NRHO_DIF) {
            showDulovDiffData(frame)
        } else {
            showTargetData(frame)
        }
    }

    private fun getDulovData(cellId: Int): Float? {
        if (curColorizeType == DENSITY_STATIC_DIF || curColorizeType == DENSITY_DYNAMIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                return FrameGenerator.dulovsPressureData[cellId]
            }
        } else if (curColorizeType == NRHO_DIF) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                return FrameGenerator.dulovsNConcentrationData[cellId]
            }
        }
        return null
    }

    private fun getCellValue(frame: FrameGenerator.Frame, cellId: Int): Float {
        return when (curColorizeType) {
            DENSITY_STATIC_DIF -> {
                frame.timeframe!!.grid.cells[cellId]!![0]
            }

            DENSITY_DYNAMIC_DIF -> {
                frame.timeframe!!.grid.cells[cellId]!![7]
            }

            NRHO_DIF -> {
                frame.timeframe!!.grid.cells[cellId]!![6]
            }

            else -> {
                0f
            }
        }
    }

    fun showDulovDiffData(frame: FrameGenerator.Frame) {
        val dulovData = mutableMapOf<Int, Float>()

        val minX = FrameGenerator.gridSchema[FrameGenerator.dulovsPressureData.keys.min()]!!.xLo
        val maxX = FrameGenerator.gridSchema[FrameGenerator.dulovsPressureData.keys.min()]!!.xLo
        if (dulovXLine == null) {
            dulovXLine = (maxX + minX) / 2f
        }

        this.data.clear()

        if (curColorizeType == DENSITY_STATIC_DIF || curColorizeType == DENSITY_DYNAMIC_DIF) {
            for (cellId in frame.timeframe!!.grid.cells.keys) {
                val gridCell = FrameGenerator.gridSchema[cellId]!!
                if (gridCell.xLo !in minX..maxX) {
                    continue
                }
                if (gridCell.yLo <= dulovYLine && dulovYLine < gridCell.xHi) {
                    dulovData[cellId] = gridCell.xLo
                }
            }
        } else if (curColorizeType == NRHO_DIF) {
            for (cellId in frame.timeframe!!.grid.cells.keys) {
                val gridCell = FrameGenerator.gridSchema[cellId]!!
                if (gridCell.xLo <= dulovXLine!! && dulovXLine!! < gridCell.xHi) {
                    dulovData[cellId] = gridCell.yLo
                }
            }
        }

        val series1 = Series<String, Number>()
        val series2 = Series<String, Number>()
        val countSteps = if (curColorizeType == NRHO_DIF) dulovData.size else 100
        val stepSize = if (curColorizeType == NRHO_DIF) 10 else 1
        val dulovCells = dulovData.keys.toList().sorted()
        var i = 0
        while (i < countSteps) {
            val dulovValue = getDulovData(dulovCells[i])
            val originalValue = getCellValue(frame, dulovCells[i])
            System.out.printf(
                "id = %d; xLo = %.4f; yLo = %.4f; dulov = %.3f; actual = %.3f%n", dulovCells[i],
                FrameGenerator.gridSchema[dulovCells[i]]!!.xLo,
                FrameGenerator.gridSchema[dulovCells[i]]!!.yLo,
                dulovValue,
                originalValue
            )
            series1.getData().add(
                Data(
                    dulovData[dulovCells[i]].toString(),
                    dulovValue ?: 0
                )
            )
            series2.getData().add(Data(dulovData[dulovCells[i]].toString(), originalValue))
            i += stepSize
        }
        this.data.add(series1)
        this.data.add(series2)

        if (!dulovData.isEmpty()) {
            when (curColorizeType) {
                DENSITY_STATIC_DIF -> {
                    this.title = String.format("График среза по давлению%n для y = %.4f см", dulovYLine)
                }

                DENSITY_DYNAMIC_DIF -> {
                    this.title = String.format("График среза по полному давлению%n для y = %.4f см", dulovYLine)
                }

                NRHO_DIF -> {
                    this.title = String.format("График среза по концентрации%n для х = %.4f см", dulovXLine)
                }

                else -> {}
            }
        } else {
            this.title = "График невозможно построить из-за отсутствия данных"
        }
    }

    fun showTargetData(frame: FrameGenerator.Frame) {
        var targetPoints = 0
        var totalPoints = 0
        var targetDiameter = 0f
        var outTotalPoints = 0.toBigDecimal()
        var outTargetPoints = 0.toBigDecimal()

        if (this.data.isEmpty()) {
            frame.timeframe!!.target
            val series = Series<String, Number>()

            var maxY = 0
            val diameter = Calculation().calculateTargetDiameter(frame.timeframe!!, 0.5f)

            for (i in frame.timeframe!!.target.indices) {
                series.getData().add(Data((i * 8 / 10).toString(), frame.timeframe!!.target[i]))
                maxY = max(maxY, frame.timeframe!!.target[i])
            }
            this.data.add(series)

            val series2 = Series<String, Number>()
            series2.getData().add(Data((diameter.leftBorder / 10).toString(), 0))
            series2.getData().add(Data((diameter.leftBorder / 10).toString(), maxY))
            this.data.add(series2)

            val series3 = Series<String, Number>()
            series3.getData().add(Data((diameter.rightBorder / 10).toString(), 0))
            series3.getData().add(Data((diameter.rightBorder / 10).toString(), maxY))
            this.data.add(series3)

            targetDiameter = diameter.diameter.toFloat() / 10
        } else {
            if (frame.timeframe != null) {
                var maxY = 0
                val diameter = Calculation().calculateTargetDiameter(frame.timeframe!!, 0.5f)

                frame.timeframe!!.target.indices.forEach { i ->
                    val element = this.data[0].getData()[i]
                    element.setYValue(frame.timeframe!!.target[i])
                    maxY = max(maxY, frame.timeframe!!.target[i])
                    targetPoints += frame.timeframe!!.target[i]
                }

                this.data[1].getData()[0].setXValue((diameter.leftBorder / 10).toString())
                this.data[1].getData()[0].setYValue(0)
                this.data[1].getData()[1].setXValue((diameter.leftBorder / 10).toString())
                this.data[1].getData()[1].setYValue(maxY)
                this.data[2].getData()[0].setXValue((diameter.rightBorder / 10).toString())
                this.data[2].getData()[0].setYValue(0)
                this.data[2].getData()[1].setXValue((diameter.rightBorder / 10).toString())
                this.data[2].getData()[1].setYValue(maxY)

                targetDiameter = diameter.diameter.toFloat() / 10
            } else {
                this.data[0].getData().forEach { element -> element!!.setYValue(0) }
                this.data[1].getData().forEach { element -> element!!.setYValue(0) }
                this.data[2].getData().forEach { element -> element!!.setYValue(0) }
            }
        }

        if (frame.timeframe != null) {
            totalPoints = frame.timeframe!!.countPoints
            outTotalPoints =
                (Config.globalParams["fnum"] ?: "0").toBigDecimal().multiply(totalPoints.toBigDecimal())
            outTargetPoints =
                (Config.globalParams["fnum"] ?: "0").toBigDecimal().multiply(targetPoints.toBigDecimal())
        }

        this.title = String.format(
            "Общее число частиц:%n" +
                    "%.2e (%d)%n" +
                    "Плотность частиц на мишени:%n" +
                    "%.2e (%d)%n" +
                    "Диаметр: %.1f мм",
            outTotalPoints, totalPoints, outTargetPoints, targetPoints, targetDiameter
        )
    }

    companion object {
        @JvmField
        var dulovXLine: Float? = null
        var dulovYLine: Float = 2.0f
    }
}
