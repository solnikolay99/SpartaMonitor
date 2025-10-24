package ru.spbstu.spartamonitor.data

import config.Config
import config.Config.Companion.PARSE_POINTS
import config.MAX_BOX_X
import config.MAX_BOX_Y
import ru.spbstu.spartamonitor.data.models.*
import ru.spbstu.spartamonitor.eventbus.EventBusFactory
import ru.spbstu.spartamonitor.events.ParserEvent
import ru.spbstu.spartamonitor.logger.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.pathString
import kotlin.math.*

class Parser {

    private var dumpDir: String = ""
    private var dumpFiles: MutableList<Path> = mutableListOf()
    private val inFilePattern: String = "(.*)(in)(.*)(.step)"
    private val dumpFilePattern: String = "(dump.)([0-9]+)(.txt)"
    private val gridFilePattern: String = "(.*grid.)([0-9]+)(.txt)"
    private val influxFilePattern: String = "(.*influx_sum.)([0-9]+)(.txt)"
    private val targetFilePattern: String = "(.*target_sum.)([0-9]+)(.txt)"
    private val allFrames: MutableMap<Int, Array<Path?>> = mutableMapOf()

    /**
     * Constants
     */
    private val gamma: Float = 5f / 3f  // показатель адиабаты
    private val rR: Float = 2077f       // универсальная газовая постоянная для He (в Дж / (кг * К))

    fun setDumpDir(dumpDir: String) {
        this.dumpDir = dumpDir
        this.storeAllDumpFiles()
    }

    private fun getListFiles(dumpDir: String): MutableList<Path> {
        return Files.walk(Paths.get(dumpDir)).sorted().toList()
    }

    fun getInFile(): Path {
        return getListFiles(dumpDir).find { x -> x.pathString.matches(inFilePattern.toRegex()) }!!
    }

    private fun storeAllDumpFiles() {
        dumpFiles = getListFiles(dumpDir)
    }

    private fun getTimeFrames(filePattern: String): Map<Int, Path> {
        return dumpFiles
            .filter { it.pathString.matches(filePattern.toRegex()) }
            .associateBy({ it.pathString.replace(filePattern.toRegex(), "$2").toInt() }, { it })
    }

    fun getAllTimeFrames() {
        val dumpFrames = getTimeFrames(dumpFilePattern)
        val gridFrames = getTimeFrames(gridFilePattern)
        val targetFrames = getTimeFrames(targetFilePattern)
        val influxFrames = getTimeFrames(influxFilePattern)

        val frames = if (PARSE_POINTS) dumpFrames.keys else gridFrames.keys
        for (key in frames) {
            allFrames[key] = arrayOfNulls(4)
            if (dumpFrames.containsKey(key)) {
                allFrames[key]!![0] = dumpFrames[key]
            }
            if (gridFrames.containsKey(key)) {
                allFrames[key]!![1] = gridFrames[key]!!
            }
            if (targetFrames.containsKey(key)) {
                allFrames[key]!![2] = targetFrames[key]!!
            }
            if (influxFrames.containsKey(key)) {
                allFrames[key]!![3] = influxFrames[key]!!
            }
        }
    }

    fun parsDumps(timeFrames: MutableList<Timeframe?>, startFrame: Int, endFrame: Int) {
        timeFrames.clear()
        (0 until allFrames.size).forEach { _ -> timeFrames.add(null) }
        val sortedKeys = allFrames.keys.stream().sorted().toList()
        for (i in startFrame until min(endFrame, sortedKeys.size)) {
            Logger.startTimer("Pars data to timeframe")
            val timeframe: Timeframe = this.parseTimeFrame(allFrames[sortedKeys[i]]!!)
            Logger.releaseTimer("Pars data to timeframe")
            timeFrames[i] = timeframe
            EventBusFactory.getEventBus().post(ParserEvent((endFrame - startFrame), (i + 1)))
        }
    }

    private fun parseTimeFrame(files: Array<Path?>): Timeframe {
        val timeframe = Timeframe()

        if (files[0] != null && PARSE_POINTS) {
            timeframe.points = this.parsePoints(files[0]!!)
        }

        if (files[1] != null) {
            timeframe.grid = this.parseGrid(files[1]!!)
        }

        if (files[2] != null) {
            timeframe.target = this.parseTarget(files[2]!!)
        }

        if (files[3] != null) {
            timeframe.countPoints = this.parseInflux(files[3]!!)
        }

        return timeframe
    }

    private fun parsePoints(filePath: Path): MutableList<Array<Number>> {
        val fileLines = Files.readAllLines(filePath)
        val points = ArrayList<Array<Number>>()

        val headers = fileLines[8].replace("ITEM: ATOMS ", "").trim().split(" ")
        val xIndex = headers.indexOf("x")
        val yIndex = headers.indexOf("y")
        val idIndex = headers.indexOf("id")
        val cellIdIndex = headers.indexOf("cellID")

        for (i in 9 until fileLines.size) {
            val params = fileLines[i].split(" ")
            points.add(
                arrayOf(
                    params[idIndex].toInt(),
                    if (Config.unitSystemCGS) params[xIndex].toFloat() else params[xIndex].toFloat() * 100,
                    if (Config.unitSystemCGS) params[yIndex].toFloat() else params[yIndex].toFloat() * 100,
                    params[cellIdIndex].toInt()
                )
            )
        }

        fileLines.clear()

        return points
    }

    private fun parseGrid(filePath: Path): Grid {
        val grid = Grid()

        val fileLines = Files.readAllLines(filePath)

        val headers = fileLines[8].replace("ITEM: CELLS", "").trim().split(" ")
        val idIndex = headers.indexOf("id")
        val idProc = headers.indexOf("proc")
        val pIndex = max(headers.indexOf("c_gTemp[1]"), headers.indexOf("f_aveGridTemp[1]"))
        val tIndex = max(headers.indexOf("c_gTemp[2]"), headers.indexOf("f_aveGridTemp[2]"))
        val vIndex = max(headers.indexOf("c_gridP[1]"), headers.indexOf("f_aveGridU[1]"))
        val keIndex = max(headers.indexOf("c_gridP[2]"), headers.indexOf("f_aveGridU[2]"))
        val nIndex = max(headers.indexOf("c_gridP[3]"), headers.indexOf("f_aveGridU[3"))
        val nrhoIndex = max(headers.indexOf("c_gridP[4]"), headers.indexOf("f_aveGridU[4]"))

        for (i in 9 until fileLines.size) {
            val params = fileLines[i].split(" ")
            val temperature = params[tIndex].toFloat()
            val cs = sqrt(gamma * rR * temperature)
            val u = abs(params[vIndex].toFloat() / (if (Config.unitSystemCGS) 100 else 1))
            val nrho = if (nrhoIndex == -1) 0f else params[nrhoIndex].toFloat()
            val pDynamic =
                if (keIndex == -1 || nrhoIndex == -1) 0f
                else (2f / 3f * params[keIndex].toFloat() * nrho) / (if (Config.unitSystemCGS) 10 else 1)

            grid.addCell(
                params[idIndex].toInt(),
                arrayOf(
                    params[pIndex].toFloat() / (if (Config.unitSystemCGS) 10 else 1),   // density in grid (SI - in Pa, CGS - in barye)
                    temperature,                            // temperature in grid
                    u,                                      // directed velocity by x in grid
                    cs,                                     // sound velocity
                    u / cs,                                 // Mach value
                    if (nIndex != -1) params[nIndex].toFloat() else 0f,       // Count particles in cell (N count)
                    if (Config.unitSystemCGS) nrho else (nrho / 1e6).toFloat(), // Nrho in cell
                    pDynamic,                               // dynamic density in grid (SI - in Pa, CGS - in barye)
                )
            )
            grid.bindProc(params[idIndex].toInt(), if (idProc == -1) 0 else params[idProc].toInt())
        }

        return grid
    }

    private fun parseInflux(filePath: Path): Int {
        val fileLines = Files.readAllLines(filePath)
        var countPoints = 0

        fileLines.forEach { fileLine ->
            val countStr = fileLine.trim().replace("Count points: ", "")
            countPoints = if (!countStr.isEmpty()) countStr.toInt() else 0
        }

        fileLines.clear()

        return countPoints
    }

    private fun parseTarget(filePath: Path): MutableList<Int> {
        val fileLines = Files.readAllLines(filePath)
        val bars = mutableListOf<Int>()

        fileLines.forEach { fileLine ->
            val params = fileLine.split(" ")
            bars.add(params[1].toInt())
        }

        fileLines.clear()

        return bars
    }

    fun parsInFile(filePath: Path) {
        val fileLines = Files.readAllLines(filePath)

        fileLines.forEach { line ->
            val params = line
                .trim()
                .replace("\t", "")
                .replace(" +".toRegex(), " ")
                .trim()
                .split(" ")
            if (params.isEmpty()) {
                return@forEach
            }
            if (params[0].startsWith("#")) {
                return@forEach
            }
            when (params[0].trim()) {
                "global" -> {
                    for (i in 1 until params.size step 2) {
                        Config.globalParams[params[i].trim()] = params[i + 1].trim()
                    }
                }

                "units" -> Config.unitSystemCGS = params[1].trim() == "cgs"
                "timestep" -> Config.tStep = params[1].trim().toFloat()
                "create_box" -> {
                    Config.shapeX = params[2].trim().toFloat() - params[1].trim().toFloat()
                    Config.shapeX = if (Config.unitSystemCGS) Config.shapeX else Config.shapeX * 100
                    Config.shapeY = params[4].trim().toFloat() - params[3].trim().toFloat()
                    Config.shapeY = if (Config.unitSystemCGS) Config.shapeY else Config.shapeY * 100
                }

                "create_grid" -> {
                    Config.spartaCellSize = Config.shapeX / params[1].trim().toInt()
                    Config.monitorCellSizeX = Config.shapeX / MAX_BOX_X
                    Config.monitorCellSizeY = Config.shapeY / MAX_BOX_Y
                    val coeffX = MAX_BOX_X / Config.shapeX
                    val coeffY = MAX_BOX_Y / Config.shapeY
                    Config.defaultMultiplayer = min(coeffX, coeffY).toInt()
                    if (coeffX < coeffY) {
                        Config.defaultBoxY = (Config.shapeY * Config.defaultMultiplayer).toInt()
                        Config.shiftBoxY =
                            if (Config.defaultBoxY > MAX_BOX_Y) (MAX_BOX_Y - Config.defaultBoxY) / 2 else 0
                    } else {
                        Config.defaultBoxX = (Config.shapeX * Config.defaultMultiplayer).toInt()
                        Config.shiftBoxX =
                            if (Config.defaultBoxX > MAX_BOX_X) (MAX_BOX_X - Config.defaultBoxX) / 2 else 0
                    }
                    Config.multiplayer = Config.defaultMultiplayer
                    Config.mainBoxX = Config.defaultBoxX
                    Config.mainBoxY = Config.defaultBoxY
                }

                "read_surf" -> Config.surfFiles.add(params[1].trim())
            }
        }
    }

    fun parsSurfFile(fileName: Path): List<Polygon> {
        val points = mutableListOf<Point>()
        val polygons = mutableListOf<Polygon>()
        var addNewPolygon = true

        val fileLines = Files.readAllLines(fileName)
        val countPoints = fileLines[2].replace("\\D", "").toInt()
        val countLines = fileLines[3].replace("\\D", "").toInt()

        for (i in 7 until 7 + countPoints) {
            val pointElements = fileLines[i].split(" ")
            val point1 = if (Config.unitSystemCGS) pointElements[1].toFloat() else pointElements[1].toFloat() * 100
            val point2 = if (Config.unitSystemCGS) pointElements[2].toFloat() else pointElements[2].toFloat() * 100
            points.add(Point(point1, point2))
        }

        for (i in 7 + countPoints + 3 until 7 + 3 + countPoints + countLines) {
            val linesElements = fileLines[i].split(" ")
            val pointFrom = linesElements[1].toInt()
            val pointTo = linesElements[2].toInt()

            if (addNewPolygon) {
                polygons.add(Polygon())
                polygons.last().addPoint(points[pointFrom - 1])
            }

            addNewPolygon = pointFrom > pointTo

            if (!addNewPolygon) {
                polygons.last().addPoint(points[pointTo - 1])
            }
        }

        return polygons
    }

    /**
     * Pars grid schema from cells.txt file to Map<int, Map<int, int>>.
     * key of root Map - xlo coordinate of cell * 1000
     * key of child Map - ylo coordinate of cell * 1000
     * value of child Map - id of cell
     *
     * @param fileName - full path to file
     * @return - Map with keys 'y : x - id'
     */
    fun parsGridSchema(fileName: Path): MutableMap<Int, GridCell> {
        val gridSchema = mutableMapOf<Int, GridCell>()

        val fileLines = Files.readAllLines(fileName)

        val headers = fileLines[8].replace("ITEM: CELLS ", "").trim().split(" ")
        val idIndex = headers.indexOf("id")
        val xLoIndex = headers.indexOf("xlo")
        val yLoIndex = headers.indexOf("ylo")
        val xHiIndex = headers.indexOf("xhi")
        val yHiIndex = headers.indexOf("yhi")

        for (i in 9 until fileLines.size) {
            val params = fileLines[i].split(" ")
            val cellId = params[idIndex].toInt()
            gridSchema[cellId] = GridCell(
                cellId,
                if (Config.unitSystemCGS) params[xLoIndex].toFloat() else params[xLoIndex].toFloat() * 100,
                if (Config.unitSystemCGS) params[yLoIndex].toFloat() else params[yLoIndex].toFloat() * 100,
                if (Config.unitSystemCGS) params[xHiIndex].toFloat() else params[xHiIndex].toFloat() * 100,
                if (Config.unitSystemCGS) params[yHiIndex].toFloat() else params[yHiIndex].toFloat() * 100
            )
        }

        return gridSchema
    }

    fun revertGridSchema(gridSchema: Map<Int, GridCell>): MutableMap<Int, MutableMap<Int, Int>> {
        val revertedGridSchema = mutableMapOf<Int, MutableMap<Int, Int>>()

        for (key in gridSchema.keys) {
            val xKey = round(gridSchema[key]!!.xLo / Config.spartaCellSize).toInt()
            val yKey = round(gridSchema[key]!!.yLo / Config.spartaCellSize).toInt()
            if (!revertedGridSchema.containsKey(xKey)) {
                revertedGridSchema[xKey] = mutableMapOf()
            }
            revertedGridSchema[xKey]!![yKey] = key
        }

        return revertedGridSchema
    }

    private fun getGridId(revertedGridSchema: Map<Int, Map<Int, Int>>, xCoord: Float, yCoord: Float): Int? {
        val xKey = round(xCoord / Config.spartaCellSize).toInt()
        val yKey = round(yCoord / Config.spartaCellSize).toInt()
        if (revertedGridSchema.containsKey(xKey)) {
            if (revertedGridSchema[xKey]!!.containsKey(yKey)) {
                return revertedGridSchema[xKey]!![yKey]
            }
        }
        return null
    }

    fun parseDulovsData(
        dataFileName: Path,
        xFileName: Path,
        yFileName: Path,
        gridSchemaRevert: Map<Int, Map<Int, Int>>
    ): Map<Int, Float> {
        val mappedData = mutableMapOf<Int, Float>()
        val data = mutableListOf<List<Float>>()

        if (Files.exists(dataFileName) && Files.exists(xFileName) && Files.exists(yFileName)) {
            val xFileLines = Files.readAllLines(xFileName)
            val xCoords = xFileLines.map { it.toFloat() }

            val yFileLines = Files.readAllLines(yFileName)
            val yCoords = yFileLines.map { it.toFloat() }

            val dataFileLines = Files.readAllLines(dataFileName)
            dataFileLines.forEach { dataFileLine ->
                data.add(dataFileLine.split("\t").map { it.toFloat() })
            }

            for (i in 0 until xCoords.size) {
                for (j in 0 until yCoords.size) {
                    val gridId = getGridId(gridSchemaRevert, xCoords[i], yCoords[j]) ?: continue
                    mappedData[gridId] = data[i][j]
                }
            }
        }

        return mappedData
    }
}