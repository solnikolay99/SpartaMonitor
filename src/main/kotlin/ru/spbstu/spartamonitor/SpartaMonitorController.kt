package ru.spbstu.spartamonitor

import config.Config
import javafx.fxml.FXML
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.TextField
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.Stage
import org.controlsfx.control.ToggleSwitch
import ru.spbstu.spartamonitor.canvas.DensityChart
import ru.spbstu.spartamonitor.canvas.GraduationCanvas
import ru.spbstu.spartamonitor.canvas.MainCanvas
import ru.spbstu.spartamonitor.colorize.ColorizeType
import ru.spbstu.spartamonitor.data.FrameGenerator
import ru.spbstu.spartamonitor.eventbus.EventBusFactory
import ru.spbstu.spartamonitor.events.DrawEvent
import ru.spbstu.spartamonitor.logger.Logger.releaseTimer
import ru.spbstu.spartamonitor.logger.Logger.startTimer
import ru.spbstu.spartamonitor.screener.Screener
import ru.spbstu.spartamonitor.screener.Screener.combineFullScene
import java.awt.image.RenderedImage
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.concurrent.Volatile

class SpartaMonitorController {
    val frameGenerator: FrameGenerator = FrameGenerator.frameGenerator
    val fgThread: Thread = Thread(frameGenerator)

    @Volatile
    var drawIterationFinished = true
    var mainStage: Stage? = null
    var colorizeType = ColorizeType.DENSITY_STATIC
    var drawByPoints = true

    @FXML
    lateinit var buttonSaveAsPicture: Button

    @FXML
    lateinit var buttonInit: Button

    @FXML
    lateinit var buttonForward: Button

    @FXML
    lateinit var buttonBackward: Button

    @FXML
    lateinit var buttonStop: Button

    @FXML
    lateinit var buttonPrevStep: Button

    @FXML
    lateinit var buttonNextStep: Button

    @FXML
    lateinit var animationCanvas: MainCanvas

    @FXML
    lateinit var graduationCanvas: GraduationCanvas

    @FXML
    lateinit var densityChart: DensityChart

    @FXML
    lateinit var textDumpFolder: TextField

    @FXML
    lateinit var buttonDumpFolder: Button

    @FXML
    lateinit var selectColorizeType: ComboBox<String>

    @FXML
    lateinit var startFrameNumber: TextField

    @FXML
    lateinit var endFrameNumber: TextField

    @FXML
    lateinit var currentFrameNumber: TextField

    @FXML
    lateinit var switchDrawPointsOrCells: ToggleSwitch

    @FXML
    lateinit var buttonDumpDulov: Button

    init {
        this.fgThread.start()
    }

    fun setStageMain(stage: Stage) {
        this.mainStage = stage

        selectColorizeType.getSelectionModel().select(0)
        if (!Config.PARSE_POINTS) {
            drawByPoints = Config.PARSE_POINTS
            switchDrawPointsOrCells.isSelected = true
            switchDrawPointsOrCells.isDisable = true
        }
    }

    fun loadConfig() {
        Config.dumpDirPath = textDumpFolder.text
    }

    fun showAlert(title: String, header: String, message: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = title
        alert.headerText = header
        alert.contentText = message
        alert.showAndWait()
    }

    @FXML
    fun onInitiateButtonClick() {
        loadConfig()

        try {
            frameGenerator.setDumpDir(this.textDumpFolder.text)
        } catch (_: Exception) {
            showAlert(
                "Проблема с файлами дампов",
                "Не смогли загрузить файлы дампов",
                "Либо они отсутствуют в папке назначения, либо не соответствуют ожидаемому формату."
            )
            return
        }

        try {
            frameGenerator.loadInFile()
        } catch (_: Exception) {
            showAlert(
                "Проблема с загрузкой файла конфигурации",
                "Не смогли загрузить файл in.step / in.ci.step",
                "Файл должен находиться в папке назначения на первом уровне."
            )
            return
        }

        try {
            frameGenerator.loadSurfs(Path.of(this.textDumpFolder.text))
        } catch (_: Exception) {
            showAlert(
                "Проблема с загрузкой файла(-ов) поверхностей",
                "Не смогли загрузить файл(-ы) поверхностей",
                "Файлы поверхностей либо отсутствуют по указанному пути, либо нечитаемы."
            )
            return
        }

        try {
            frameGenerator.loadGrid(Path.of(this.textDumpFolder.text))
        } catch (_: Exception) {
            showAlert(
                "Проблема с загрузкой файла расчётной сетки",
                "Не смогли загрузить или обработать файл cells.txt",
                "Файлы cells.txt либо отсутствует по указанному пути, либо нечитаем, либо несовместим с другими данными."
            )
            return
        }

        try {
            frameGenerator.loadDulovsData(Path.of(this.textDumpFolder.text))
        } catch (_: Exception) {
            showAlert(
                "Проблема с загрузкой файла скейлинга Дулова",
                "Не смогли загрузить или обработать файлы скейлинга дулова",
                "Проверить наличие и корректность файлов dulov/xx_Dulov_check.txt, dulov/yy_Dulov_check.txt," +
                        "dulov/Dulov_density_check.txt, dulov/Dulov_n_check.txt в папке дампа."
            )
            return
        }

        animationCanvas.drawMask(frameGenerator)
        graduationCanvas.colorize(ColorizeType.DENSITY_STATIC)

        frameGenerator.preloadTimeFrames(
            this.startFrameNumber.text.toInt(),
            this.endFrameNumber.text.toInt()
        )

        buttonBackward.isDisable = false
        buttonForward.isDisable = false
        buttonStop.isDisable = true
        buttonPrevStep.isDisable = false
        buttonNextStep.isDisable = false
    }

    @FXML
    fun onButtonSaveAsPictureClick() {
        val fileChooser = FileChooser()
        fileChooser.title = "Save File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("PNG files", "*.png"))
        val file = fileChooser.showSaveDialog(this.mainStage)

        try {
            val mainImage = Screener.getImageFromCanvas(animationCanvas)
            val graduationImage = Screener.getImageFromCanvas(graduationCanvas)
            val chartImage = Screener.getImageFromCanvas(densityChart)
            val outImage: RenderedImage = combineFullScene(mainImage, graduationImage, chartImage)
            ImageIO.write(outImage, "png", file)
            println("Screen saved to '${file.absolutePath}'")
        } catch (_: Exception) {
        }
    }

    @FXML
    fun onNextIterationButtonClick() {
        EventBusFactory.eventBus.post(DrawEvent(1))
        frameGenerator.startOneIteration()
    }

    @FXML
    fun onPrevIterationButtonClick() {
        EventBusFactory.eventBus.post(DrawEvent(-1))
        frameGenerator.startOneIteration()
    }

    @FXML
    fun onForwardIterationsButtonClick() {
        buttonInit.isDisable = true
        buttonBackward.isDisable = true
        buttonForward.isDisable = true
        buttonStop.isDisable = false
        buttonPrevStep.isDisable = true
        buttonNextStep.isDisable = true
        EventBusFactory.eventBus.post(DrawEvent(1))
        frameGenerator.startIterations()
    }

    @FXML
    fun onBackwardIterationsButtonClick() {
        buttonInit.isDisable = true
        buttonBackward.isDisable = true
        buttonForward.isDisable = true
        buttonStop.isDisable = false
        buttonPrevStep.isDisable = true
        buttonNextStep.isDisable = true
        EventBusFactory.eventBus.post(DrawEvent(-1))
        frameGenerator.startIterations()
    }

    @FXML
    fun onStopIterationsButtonClick() {
        frameGenerator.stopIteration()
        buttonInit.isDisable = false
        buttonBackward.isDisable = false
        buttonForward.isDisable = false
        buttonStop.isDisable = true
        buttonPrevStep.isDisable = false
        buttonNextStep.isDisable = false
    }

    @FXML
    fun onClose() {
        this.frameGenerator.isAlive = false
        this.fgThread.interrupt()
    }

    @FXML
    fun onButtonDumpFolderClick() {
        val fileChooser = DirectoryChooser()
        fileChooser.title = "Select dump dir"
        val selectedFile = fileChooser.showDialog(this.mainStage)
        if (selectedFile != null) {
            textDumpFolder.text = selectedFile.absolutePath
        }
    }

    @FXML
    fun onColorizeTypeChange() {
        when (selectColorizeType.getSelectionModel().selectedIndex) {
            0 -> colorizeType = ColorizeType.DENSITY_STATIC
            1 -> colorizeType = ColorizeType.DENSITY_DYNAMIC
            2 -> colorizeType = ColorizeType.TEMPERATURE
            3 -> colorizeType = ColorizeType.VELOCITY
            4 -> colorizeType = ColorizeType.SOUND_VELOCITY
            5 -> colorizeType = ColorizeType.MACH
            6 -> colorizeType = ColorizeType.BIND
            7 -> colorizeType = ColorizeType.N_COUNT
            8 -> colorizeType = ColorizeType.NRHO
            9 -> colorizeType = ColorizeType.DENSITY_STATIC_DIF
            10 -> colorizeType = ColorizeType.DENSITY_DYNAMIC_DIF
            11 -> colorizeType = ColorizeType.NRHO_DIF
            12 -> colorizeType = ColorizeType.NRHO_DULOV
        }
        graduationCanvas.colorize(colorizeType)
        EventBusFactory.eventBus.post(DrawEvent(0))
    }

    @FXML
    fun onDrawTypeChange() {
        drawByPoints = !switchDrawPointsOrCells.isSelected
        EventBusFactory.eventBus.post(DrawEvent(0))
    }

    @FXML
    fun onDumpDulovButtonClick() {
        frameGenerator.saveDulovsData(Path.of(this.textDumpFolder.text))
    }

    fun drawIteration(playDirection: Int) {
        if (drawIterationFinished && (frameGenerator.isRunning || frameGenerator.showOneIteration)) {
            drawIterationFinished = false

            var frame: FrameGenerator.Frame
            do {
                frame = this.frameGenerator.getFrame(playDirection)
            } while (frame.timeframe == null)

            frameGenerator.showOneIteration = false

            startTimer("Draw iteration")

            val title = String.format("%.0f мкс", (frame.frameNumber) * (Config.tStep / 1e-6 * 100))
            animationCanvas.drawIteration(this.frameGenerator, frame, colorizeType, drawByPoints, title)
            densityChart.drawIteration(frame, colorizeType)

            releaseTimer("Draw iteration")

            drawIterationFinished = true
        }
    }

    fun drawDensityChart(xCoord: Float) {
        var frame: FrameGenerator.Frame
        do {
            frame = this.frameGenerator.getFrame(0)
        } while (frame.timeframe == null)

        DensityChart.dulovXLine = xCoord
        densityChart.drawIteration(frame, colorizeType)
    }
}