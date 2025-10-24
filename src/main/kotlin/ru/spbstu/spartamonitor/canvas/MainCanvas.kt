package ru.spbstu.spartamonitor.canvas

import config.Config
import config.MAX_BOX_X
import config.MAX_BOX_Y
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.canvas.Canvas
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.paint.Color
import ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema
import ru.spbstu.spartamonitor.colorize.ColorizeType
import ru.spbstu.spartamonitor.data.FrameGenerator
import ru.spbstu.spartamonitor.data.models.GridCell
import ru.spbstu.spartamonitor.eventbus.EventBusFactory
import ru.spbstu.spartamonitor.events.DrawDensityEvent
import ru.spbstu.spartamonitor.events.DrawEvent
import kotlin.math.abs

class MainCanvas : Canvas() {
    private var zoom: Float = 1f

    private var animatedCanvasX = 0.0
    private var animatedCanvasY = 0.0
    private var originalShiftX = 0
    private var originalShiftY = 0

    private var curColorizeType = ColorizeType.DENSITY_STATIC

    fun drawIteration(
        frameGenerator: FrameGenerator,
        frame: FrameGenerator.Frame,
        colorizeType: ColorizeType,
        flgDrawByPointsOrCells: Boolean,
        title: String
    ) {
        val gc = this.getGraphicsContext2D()
        gc.isImageSmoothing = true

        drawMask(frameGenerator)

        curColorizeType = colorizeType
        if (flgDrawByPointsOrCells) {
            colorizePoints(frame, colorizeType)
        } else {
            colorizeCells(frame, colorizeType)
        }

        drawTitle(title)
    }

    private fun drawAxes() {
        val gc = this.getGraphicsContext2D()
        gc.fill = Color.GRAY

        run {
            var i = 0
            while (i <= Config.shapeX) {
                gc.fillRect(
                    (i * Config.multiplayer - 1).toDouble(),
                    (Config.shiftBoxY + Config.mainBoxY - 6).toDouble(),
                    1.0,
                    6.0
                )
                i++
            }
        }
        var i = 0
        while (i <= Config.shapeX) {
            gc.fillRect(
                (i * Config.multiplayer - 1).toDouble(),
                (Config.shiftBoxY + Config.mainBoxY - 10).toDouble(),
                1.0,
                10.0
            )
            i += 5
        }
    }

    private fun drawZoom() {
        val gc = this.getGraphicsContext2D()

        gc.fill = Color.LIGHTGRAY
        gc.fillRect(0.0, 0.0, 45.0, 22.0)
        gc.fill = Color.WHITE
        gc.fillText(String.format("%.2f X", zoom), 5.0, 15.0)
    }

    private fun drawTitle(title: String) {
        val gc = this.getGraphicsContext2D()

        val middle = gc.canvas.width.toInt() / 2
        gc.fill = Color.LIGHTGRAY
        gc.fillRect((middle - 50).toDouble(), 0.0, 100.0, 22.0)
        gc.fill = Color.WHITE
        gc.fillText(title, (middle - (title.length.toFloat() * 5 / 2)).toDouble(), 15.0)
    }

    fun drawMask(frameGenerator: FrameGenerator) {
        val gc = this.getGraphicsContext2D()
        gc.isImageSmoothing = true
        gc.clearRect(0.0, 0.0, this.width, this.height)
        gc.fill = Color.AZURE
        gc.fillRect(
            Config.shiftBoxX.toDouble(),
            Config.shiftBoxY.toDouble(),
            Config.mainBoxX.toDouble(),
            Config.mainBoxY.toDouble()
        )

        for (surfs in frameGenerator.surfs.values) {
            for (surf in surfs) {
                val xs: MutableList<Double> = mutableListOf()
                val ys: MutableList<Double> = mutableListOf()
                for (surfPoint in surf.points) {
                    xs.add((Config.shiftBoxX + surfPoint.x * Config.multiplayer).toDouble())
                    ys.add((Config.shiftBoxY + surfPoint.y * Config.multiplayer).toDouble())
                }
                gc.fill = Color.GRAY
                gc.fillPolygon(
                    xs.toDoubleArray(),
                    ys.toDoubleArray(),
                    xs.size
                )
            }
        }

        drawAxes()
        drawZoom()
    }

    private fun getDiffByDulov(frame: FrameGenerator.Frame, cellId: Int): Float? {
        if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                val origCellValue = frame.timeframe!!.grid.cells[cellId]!![0]
                val dulovsValue: Float = FrameGenerator.dulovsPressureData[cellId]!!
                return abs(origCellValue / dulovsValue * 100)
            }
        } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                val origCellValue = frame.timeframe!!.grid.cells[cellId]!![7]
                val dulovsValue: Float = FrameGenerator.dulovsPressureData[cellId]!!
                return abs(origCellValue / dulovsValue * 100)
            }
        } else if (curColorizeType == ColorizeType.NRHO_DIF) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                val origCellValue = frame.timeframe!!.grid.cells[cellId]!![6]
                val dulovsValue: Float = FrameGenerator.dulovsNConcentrationData[cellId]!!
                return abs(origCellValue / dulovsValue * 100)
            }
        }
        return null
    }

    private fun getDulovData(cellId: Int): Float? {
        if (curColorizeType == ColorizeType.NRHO_DULOV) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                return FrameGenerator.dulovsNConcentrationData[cellId]
            }
        }
        return null
    }

    private fun getColorForType(frame: FrameGenerator.Frame, cellId: Int, colorizeType: ColorizeType): Color? {
        val value = when (colorizeType) {
            ColorizeType.DENSITY_STATIC -> frame.timeframe!!.grid.cells[cellId]!![0]
            ColorizeType.TEMPERATURE -> frame.timeframe!!.grid.cells[cellId]!![1]
            ColorizeType.VELOCITY -> frame.timeframe!!.grid.cells[cellId]!![2]
            ColorizeType.SOUND_VELOCITY -> frame.timeframe!!.grid.cells[cellId]!![3]
            ColorizeType.MACH -> frame.timeframe!!.grid.cells[cellId]!![4]
            ColorizeType.BIND -> frame.timeframe!!.grid.procs[cellId]!!.toFloat()
            ColorizeType.N_COUNT -> frame.timeframe!!.grid.cells[cellId]!![5]
            ColorizeType.NRHO -> frame.timeframe!!.grid.cells[cellId]!![6]
            ColorizeType.DENSITY_STATIC_DIF,
            ColorizeType.DENSITY_DYNAMIC_DIF,
            ColorizeType.NRHO_DIF -> getDiffByDulov(
                frame,
                cellId
            )

            ColorizeType.NRHO_DULOV -> getDulovData(cellId)
            ColorizeType.DENSITY_DYNAMIC -> frame.timeframe!!.grid.cells[cellId]!![7]
        }

        if (value == null) {
            return null
        } else if (value > colorizeType.maxValue) {
            return colorSchema.last()
        } else if (value < colorizeType.minValue) {
            return colorSchema[0]
        }
        return colorSchema[(value * (colorSchema.size - 1) / (colorizeType.maxValue - colorizeType.minValue)).toInt()]
    }

    private fun colorizePoints(frame: FrameGenerator.Frame, colorizeType: ColorizeType) {
        val gc = this.getGraphicsContext2D()

        val xLoBorder = -Config.shiftBoxX * Config.monitorCellSizeX / zoom
        val xHiBorder: Float = (MAX_BOX_X - Config.shiftBoxX) * Config.monitorCellSizeX / zoom
        val yLoBorder = -Config.shiftBoxY * Config.monitorCellSizeY / zoom
        val yHiBorder: Float = (MAX_BOX_Y - Config.shiftBoxY) * Config.monitorCellSizeY / zoom

        for (point in frame.timeframe!!.points) {
            if (point[1].toFloat() !in xLoBorder..xHiBorder) {
                continue
            } else if (point[2].toFloat() !in yLoBorder..yHiBorder) {
                continue
            }
            val color = try {
                val cellId = point[3].toInt()
                if (!frame.timeframe!!.grid.cells.containsKey(cellId)) {
                    Color.BLACK
                } else {
                    getColorForType(frame, cellId, colorizeType)
                }
            } catch (_: Exception) {
                Color.YELLOW
            }
            if (color != null) {
                gc.fill = color
                gc.fillOval(
                    (Config.shiftBoxX + point[1].toFloat() * Config.multiplayer).toDouble(),
                    (Config.shiftBoxY + point[2].toFloat() * Config.multiplayer).toDouble(),
                    1.0,
                    1.0
                )
            }
        }
    }

    private fun colorizeCells(frame: FrameGenerator.Frame, colorizeType: ColorizeType) {
        val gc = this.getGraphicsContext2D()

        val xLoBorder = -Config.shiftBoxX * Config.monitorCellSizeX / zoom
        val xHiBorder: Float = (MAX_BOX_X - Config.shiftBoxX) * Config.monitorCellSizeX / zoom
        val yLoBorder = -Config.shiftBoxY * Config.monitorCellSizeY / zoom
        val yHiBorder: Float = (MAX_BOX_Y - Config.shiftBoxY) * Config.monitorCellSizeY / zoom

        for (cellId in frame.timeframe!!.grid.cells.keys) {
            val gridCell: GridCell = FrameGenerator.gridSchema[cellId]!!
            if (gridCell.xLo < xLoBorder || gridCell.xHi < xLoBorder || xHiBorder < gridCell.xLo || xHiBorder < gridCell.xHi) {
                continue
            } else if (gridCell.yLo < yLoBorder || gridCell.yHi < yLoBorder || yHiBorder < gridCell.yLo || yHiBorder < gridCell.yHi) {
                continue
            }
            val color = getColorForType(frame, cellId, colorizeType)
            if (color != null) {
                gc.fill = color
                gc.fillRect(
                    (Config.shiftBoxX + gridCell.xLo * Config.multiplayer).toDouble(),
                    (Config.shiftBoxY + gridCell.yLo * Config.multiplayer).toDouble(),
                    ((gridCell.xHi - gridCell.xLo) * Config.multiplayer).toDouble(),
                    ((gridCell.yHi - gridCell.yLo) * Config.multiplayer).toDouble()
                )
            }
        }
    }

    fun changeZoom(delta: Float) {
        this.zoom += delta
        if (this.zoom < 1) {
            this.zoom = 1f
        } else if (this.zoom > 100) {
            this.zoom = 100f
        }
    }

    @FXML
    private fun onZooming(event: ScrollEvent) {
        val denominator = if (this.zoom > 50) 50 else if (this.zoom > 10) 100 else 200
        this.changeZoom(event.deltaY.toFloat() / denominator)
        if (this.zoom == 1f) {
            Config.multiplayer = Config.defaultMultiplayer
            Config.mainBoxX = Config.defaultBoxX
            Config.mainBoxY = Config.defaultBoxY
        } else {
            Config.multiplayer = (Config.defaultMultiplayer * this.zoom).toInt()
            Config.mainBoxX = (Config.defaultBoxX * this.zoom).toInt()
            Config.mainBoxY = (Config.defaultBoxY * this.zoom).toInt()
        }

        Config.shiftBoxX -= event.deltaY.toInt()
        if (Config.shiftBoxX > 0) {
            Config.shiftBoxX = 0
        }

        if (Config.mainBoxX + Config.shiftBoxX > MAX_BOX_X) {
            Config.mainBoxX = MAX_BOX_X - Config.shiftBoxX
        }

        Config.shiftBoxY = (MAX_BOX_Y - Config.mainBoxY) / 2

        EventBusFactory.eventBus.post(DrawEvent(0))
    }

    var canvasOnMousePressedEventHandler: EventHandler<MouseEvent> = EventHandler { mouseEvent ->
        if (mouseEvent.button == MouseButton.PRIMARY) {
            animatedCanvasX = mouseEvent.sceneX
            animatedCanvasY = mouseEvent.sceneY
            originalShiftX = Config.shiftBoxX
            originalShiftY = Config.shiftBoxY
        } else if (mouseEvent.button == MouseButton.SECONDARY) {
            showCoordsForRightButton(mouseEvent)
        }
    }

    var canvasOnMouseReleasedEventHandler: EventHandler<MouseEvent> = EventHandler { mouseEvent ->
        if (mouseEvent.button == MouseButton.SECONDARY) {
            EventBusFactory.eventBus.post(DrawEvent(0))
        }
    }

    var canvasOnMouseDraggedEventHandler: EventHandler<MouseEvent> = EventHandler { mouseEvent ->
        if (mouseEvent.button == MouseButton.PRIMARY) {
            val offsetX = mouseEvent.sceneX - animatedCanvasX
            val offsetY = mouseEvent.sceneY - animatedCanvasY
            Config.shiftBoxX = originalShiftX + offsetX.toInt()
            Config.shiftBoxY = originalShiftY + offsetY.toInt()
            EventBusFactory.eventBus.post(DrawEvent(0))
        }
    }

    init {
        this.onScroll = EventHandler { event: ScrollEvent -> this.onZooming(event) }
        this.onMousePressed = canvasOnMousePressedEventHandler
        this.onMouseReleased = canvasOnMouseReleasedEventHandler
        this.onMouseDragged = canvasOnMouseDraggedEventHandler
    }

    private fun showCoordsForRightButton(mouseEvent: MouseEvent) {
        val canvasX = mouseEvent.x
        val canvasY = mouseEvent.y

        val surfX = ((canvasX - Config.shiftBoxX) / Config.multiplayer * 1000).toInt() / 5 * 5
        val surfX2 = (surfX.toFloat() / Config.spartaCellSize / 1000).toInt()
        val surfX1 = ((canvasX - Config.shiftBoxX) / Config.multiplayer * 1000).toInt() / 5 * 5 + 5
        val surfY = ((canvasY - Config.shiftBoxY) / Config.multiplayer * 1000).toFloat() / 5 * 5 / 1000
        val surfY2 = (surfY / Config.spartaCellSize).toInt()
        val frame = FrameGenerator.frameGenerator.getFrame(0)

        var countCells = 0
        var countCellsWithValue = 0
        var cellValue = 0f
        var cellSumValue = 0f
        if (FrameGenerator.inSurfSchema.containsKey(surfX)) {
            val cellIds = FrameGenerator.inSurfSchema[surfX]!!
            val frameCells = frame.timeframe!!.grid.cells

            countCells = cellIds.size
            for (gridCell in cellIds.values) {
                if (frameCells.containsKey(gridCell.cellId)) {
                    if (curColorizeType == ColorizeType.DENSITY_STATIC) {
                        if (frameCells[gridCell.cellId]!![0] > 0f) {
                            cellSumValue += frameCells[gridCell.cellId]!![0]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![0]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.TEMPERATURE) {
                        if (frameCells[gridCell.cellId]!![1] > 0f) {
                            cellSumValue += frameCells[gridCell.cellId]!![1]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![1]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.VELOCITY) {
                        if (frameCells[gridCell.cellId]!![2] > 0f) {
                            cellSumValue += frameCells[gridCell.cellId]!![2]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![2]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.SOUND_VELOCITY) {
                        if (frameCells[gridCell.cellId]!![3] > 0f) {
                            cellSumValue += frameCells[gridCell.cellId]!![3]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![3]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.MACH) {
                        if (frameCells[gridCell.cellId]!![4] < Float.MAX_VALUE) {
                            cellSumValue += frameCells[gridCell.cellId]!![4]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![4]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.N_COUNT) {
                        if (frameCells[gridCell.cellId]!![5] < Float.MAX_VALUE) {
                            cellSumValue += frameCells[gridCell.cellId]!![5]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![5]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.NRHO) {
                        if (frameCells[gridCell.cellId]!![6] < Float.MAX_VALUE) {
                            cellSumValue += frameCells[gridCell.cellId]!![6]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![6]
                            }
                        }
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC) {
                        if (frameCells[gridCell.cellId]!![7] > 0f) {
                            cellSumValue += frameCells[gridCell.cellId]!![7]
                            countCellsWithValue++
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells[gridCell.cellId]!![7]
                            }
                        }
                    }
                }
            }
        } else {
            if (FrameGenerator.gridSchemaRevert.containsKey(surfX2) &&
                FrameGenerator.gridSchemaRevert[surfX2]!!.containsKey(surfY2)
            ) {
                val cellId: Int = FrameGenerator.gridSchemaRevert[surfX2]!![surfY2]!!
                val frameCells = frame.timeframe!!.grid.cells

                if (frameCells.containsKey(cellId)) {
                    if (curColorizeType == ColorizeType.DENSITY_STATIC) {
                        if (frameCells[cellId]!![0] > 0f) {
                            cellValue = frameCells[cellId]!![0]
                        }
                    } else if (curColorizeType == ColorizeType.TEMPERATURE) {
                        if (frameCells[cellId]!![1] > 0f) {
                            cellValue = frameCells[cellId]!![1]
                        }
                    } else if (curColorizeType == ColorizeType.VELOCITY) {
                        if (frameCells[cellId]!![2] > 0f) {
                            cellValue = frameCells[cellId]!![2]
                        }
                    } else if (curColorizeType == ColorizeType.SOUND_VELOCITY) {
                        if (frameCells[cellId]!![3] > 0f) {
                            cellValue = frameCells[cellId]!![3]
                        }
                    } else if (curColorizeType == ColorizeType.MACH) {
                        if (frameCells[cellId]!![4] < Float.MAX_VALUE) {
                            cellValue = frameCells[cellId]!![4]
                        }
                    } else if (curColorizeType == ColorizeType.N_COUNT) {
                        if (frameCells[cellId]!![5] < Float.MAX_VALUE) {
                            cellValue = frameCells[cellId]!![5]
                        }
                    } else if (curColorizeType == ColorizeType.NRHO) {
                        if (frameCells[cellId]!![6] < Float.MAX_VALUE) {
                            cellValue = frameCells[cellId]!![6]
                        }
                    } else if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
                        cellValue = getDiffByDulov(frame, cellId)!!
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
                        cellValue = getDiffByDulov(frame, cellId)!!
                    } else if (curColorizeType == ColorizeType.NRHO_DIF) {
                        cellValue = getDiffByDulov(frame, cellId)!!
                    } else if (curColorizeType == ColorizeType.NRHO_DULOV) {
                        cellValue = getDulovData(cellId)!!
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC) {
                        if (frameCells[cellId]!![7] > 0f) {
                            cellValue = frameCells[cellId]!![7]
                        }
                    }
                }
            }
        }

        countCells = if (countCells == 0) 1 else countCells
        countCellsWithValue = if (countCellsWithValue == 0) 1 else countCellsWithValue
        val formattedCellValue =
            if (cellValue > 100000f) String.format("%.2e", cellValue) else String.format("%.1f", cellValue)
        val formattedCellSumValue = if ((cellSumValue / countCells) > 100000f) String.format(
            "%.2e",
            cellSumValue / countCells
        ) else String.format("%.1f", cellSumValue / countCells)
        val formattedPerCellSumValue = if ((cellSumValue / countCellsWithValue) > 100000f) String.format(
            "%.2e",
            cellSumValue / countCellsWithValue
        ) else String.format("%.1f", cellSumValue / countCellsWithValue)
        val text = String.format(
            "%.3f - %.3f см: %s | %s | %s %s",
            surfX.toFloat() / 1000,
            surfX1.toFloat() / 1000,
            formattedCellValue,
            formattedCellSumValue,
            formattedPerCellSumValue,
            curColorizeType.units
        )

        val gc = this.getGraphicsContext2D()

        gc.fill = Color.GRAY
        gc.fillRect(canvasX, 0.0, 1.0, this.height)
        gc.fillRect(canvasX - (text.length.toFloat() * 6 / 2) - 1.5, 0.0, (text.length * 6 + 3).toDouble(), 22.0)
        gc.fill = Color.WHITE
        gc.fillText(text, canvasX - (text.length.toFloat() * 5 / 2), 15.0)

        EventBusFactory.eventBus.post(DrawDensityEvent(surfX.toFloat() / 1000))
    }
}
