package ru.spbstu.spartamonitor.data

import config.Config
import ru.spbstu.spartamonitor.data.models.GridCell
import ru.spbstu.spartamonitor.data.models.Polygon
import ru.spbstu.spartamonitor.data.models.Timeframe
import ru.spbstu.spartamonitor.logger.Logger
import java.awt.Rectangle
import java.io.BufferedWriter
import java.io.FileWriter
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.Volatile

class FrameGenerator : Runnable {

    var isRunning = false
    var showOneIteration = false

    private val lock = Any()

    private val parser = Parser()

    @Volatile
    var isAlive = true

    @Volatile
    private var flgPreload = false
    private var startFrame = 0
    private var endFrame = 0

    var timeframes: MutableList<Timeframe?> = Collections.synchronizedList<Timeframe?>(mutableListOf<Timeframe?>())
    var curFrame: Int = 0
    var surfs: MutableMap<String, List<Polygon>> = mutableMapOf()

    companion object {
        @JvmField
        var gridSchema: MutableMap<Int, GridCell> = mutableMapOf()

        @JvmField
        var inSurfSchema: MutableMap<Int, MutableMap<Int, GridCell>> = mutableMapOf()

        @JvmField
        var gridSchemaRevert: MutableMap<Int, MutableMap<Int, Int>> = mutableMapOf()

        @JvmField
        var dulovsPressureData: Map<Int, Float> = mapOf()

        @JvmField
        var dulovsNConcentrationData: Map<Int, Float> = mapOf()

        @JvmField
        var frameGenerator: FrameGenerator = FrameGenerator()
    }

    fun setDumpDir(dumpDir: String) {
        this.parser.setDumpDir(dumpDir)
    }

    override fun run() {
        while (isAlive) {
            if (flgPreload) {
                Logger.startTimer("Get all timeframe data")

                try {
                    this.parser.getAllTimeFrames()
                } catch (_: Exception) {
                    println("\u001B[31m Не смогли разбить файлы дампов на группы \u001B[0m")
                }

                this.timeframes.clear()

                try {
                    this.parser.parsDumps(this.timeframes, startFrame, endFrame)
                } catch (_: Exception) {
                    println("\u001B[31m Не смогли распарсить файлы дампов \u001B[0m")
                }

                Logger.releaseTimer("Get all timeframe data")

                flgPreload = false
            } else {
                try {
                    Thread.sleep(100)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun startOneIteration() {
        this.isRunning = false
        this.showOneIteration = true
    }

    fun showOneIteration() {
        this.showOneIteration = true
    }

    fun startIterations() {
        this.isRunning = true
    }

    fun stopIteration() {
        this.isRunning = false
    }

    fun preloadTimeFrames(startFrame: Int, endFrame: Int) {
        this.startFrame = startFrame
        this.endFrame = endFrame
        flgPreload = true
    }

    data class Frame(
        var timeframe: Timeframe? = Timeframe(),
        var frameNumber: Int = 0
    )

    fun getFrame(countSteps: Int): Frame {
        val frame = Frame()
        curFrame += countSteps
        if (curFrame >= this.timeframes.size) {
            curFrame = 0
        } else if (curFrame < 0) {
            curFrame = this.timeframes.size - 1
        }
        if (this.isRunning || this.showOneIteration || countSteps == 0) {
            synchronized(lock) {
                frame.timeframe = this.timeframes[curFrame]!!
            }
        }
        frame.frameNumber = curFrame
        return frame
    }

    fun loadInFile() {
        curFrame = 0
        this.parser.parsInFile(this.parser.getInFile())
    }

    fun loadSurfs(rootDir: Path) {
        surfs = mutableMapOf()
        for (filePath in Config.surfFiles) {
            val polygons = this.parser.parsSurfFile(Path.of(rootDir.toString(), filePath))
            if (!polygons.isEmpty()) {
                surfs[filePath] = polygons
            }
        }
    }

    fun loadGrid(filePath: Path) {
        loadGridSchema(filePath)
        excludeOutSurfGridCells()
        revertGridSchema()
    }

    private fun loadGridSchema(rootDir: Path) {
        gridSchema = this.parser.parsGridSchema(Path.of(rootDir.toString(), "cells.txt"))
    }

    private fun revertGridSchema() {
        gridSchemaRevert = this.parser.revertGridSchema(gridSchema)
    }

    fun loadDulovsData(rootDir: Path) {
        val xFileName = Path.of(rootDir.toString(), "dulov/xx_Dulov_check.txt")
        val yFileName = Path.of(rootDir.toString(), "dulov/yy_Dulov_check.txt")
        dulovsPressureData = this.parser.parseDulovsData(
            Path.of(rootDir.toString(), "dulov/Dulov_density_check.txt"),
            xFileName, yFileName, gridSchemaRevert
        )
        dulovsNConcentrationData = this.parser.parseDulovsData(
            Path.of(rootDir.toString(), "dulov/Dulov_n_check.txt"),
            xFileName, yFileName, gridSchemaRevert
        )
    }

    private fun excludeOutSurfGridCells() {
        val surfBorders: MutableList<FloatArray> = ArrayList<FloatArray>()
        val excludedAreas: MutableList<java.awt.Polygon> = ArrayList<java.awt.Polygon>()
        for (surf in surfs.values) {
            val borders = floatArrayOf(
                Float.MAX_VALUE,
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                Float.MIN_VALUE
            )
            for (polygon in surf) {
                val polygonBorders = polygon.borderPoints

                if (polygonBorders[0] < borders[0]) {
                    borders[0] = polygonBorders[0]
                }
                if (polygonBorders[1] < borders[1]) {
                    borders[1] = polygonBorders[1]
                }
                if (polygonBorders[2] > borders[2]) {
                    borders[2] = polygonBorders[2]
                }
                if (polygonBorders[3] > borders[3]) {
                    borders[3] = polygonBorders[3]
                }

                val excludedArea = java.awt.Polygon()
                for (point in polygon.points) {
                    excludedArea.addPoint((point.x * 1000).toInt(), (point.y * 1000).toInt())
                }
                excludedAreas.add(excludedArea)
            }
            surfBorders.add(borders)
        }

        for (cellId in gridSchema.keys) {
            val gridCell: GridCell = gridSchema[cellId]!!
            for (borders in surfBorders) {
                if (gridCell.xLo >= borders[0] && gridCell.xLo <= borders[2]
                    && gridCell.yLo >= borders[1] && gridCell.yLo <= borders[3]
                    && gridCell.xHi >= borders[0] && gridCell.xHi <= borders[2]
                    && gridCell.yHi >= borders[1] && gridCell.yHi <= borders[3]
                ) {
                    val gridPolygon = Rectangle(
                        (gridCell.xLo * 1000).toInt(),
                        (gridCell.yLo * 1000).toInt(),
                        ((gridCell.xHi - gridCell.xLo) * 1000).toInt(),
                        ((gridCell.yHi - gridCell.yLo) * 1000).toInt()
                    )

                    var flgGridPolygonInside = false
                    for (excludedArea in excludedAreas) {
                        if (excludedArea.contains(gridPolygon)) {
                            flgGridPolygonInside = true
                            break
                        }
                    }

                    if (flgGridPolygonInside) {
                        continue
                    }

                    if (!inSurfSchema.containsKey((gridCell.xLo * 1000).toInt())) {
                        inSurfSchema[(gridCell.xLo * 1000).toInt()] = mutableMapOf()
                    }
                    inSurfSchema[(gridCell.xLo * 1000).toInt()]!![cellId] = gridSchema[cellId]!!
                }
            }
        }
    }

    fun saveDulovsData(rootDir: Path) {
        val dumpDataFile = Path.of(rootDir.toString(), "Dulovs_data.txt")

        println("Start saving Dulovs dump")

        try {
            BufferedWriter(FileWriter(dumpDataFile.toAbsolutePath().toString())).use { writer ->
                val header =
                    "ITEM: CELLS id proc f_aveGridTemp[1] f_aveGridTemp[2] f_aveGridU[1] f_aveGridU[3] f_aveGridU[4]"
                (0..7).forEach { _ ->
                    writer.write("")
                    writer.newLine()
                }
                writer.write(header)
                writer.newLine()
                for (key in dulovsPressureData.keys) {
                    writer.write(
                        String.format(
                            Locale.ENGLISH,
                            "%d 0 %.1f 0 0 0 0",
                            key,
                            dulovsPressureData[key]
                        )
                    )
                    writer.newLine()
                }
            }
        } catch (_: Exception) {
        }

        println("Dulovs dump saved to ${dumpDataFile.toAbsolutePath()}")
    }
}