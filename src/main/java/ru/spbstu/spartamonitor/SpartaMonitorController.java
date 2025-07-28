package ru.spbstu.spartamonitor;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.ToggleSwitch;
import ru.spbstu.spartamonitor.canvas.DensityChart;
import ru.spbstu.spartamonitor.canvas.GraduationCanvas;
import ru.spbstu.spartamonitor.canvas.MainCanvas;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.eventbus.EventBusFactory;
import ru.spbstu.spartamonitor.events.DrawEvent;
import ru.spbstu.spartamonitor.logger.Logger;
import ru.spbstu.spartamonitor.screener.Screener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static ru.spbstu.spartamonitor.config.Config.tStep;

public class SpartaMonitorController {

    public final FrameGenerator frameGenerator = FrameGenerator.getFrameGenerator();
    private final Thread fgThread;
    private volatile boolean drawIterationFinished = true;
    private Stage mainStage;
    private ColorizeType colorizeType = ColorizeType.DENSITY_STATIC;
    private boolean drawByPoints = true;

    @FXML
    protected Button buttonSaveAsPicture;
    @FXML
    protected Button buttonInit;
    @FXML
    protected Button buttonForward;
    @FXML
    protected Button buttonBackward;
    @FXML
    protected Button buttonStop;
    @FXML
    protected Button buttonPrevStep;
    @FXML
    protected Button buttonNextStep;
    @FXML
    protected MainCanvas animationCanvas;
    @FXML
    protected GraduationCanvas graduationCanvas;
    @FXML
    protected DensityChart densityChart;
    @FXML
    protected TextField textDumpFolder;
    @FXML
    protected Button buttonDumpFolder;
    @FXML
    public ComboBox<String> selectColorizeType;
    @FXML
    public TextField startFrameNumber;
    @FXML
    public TextField endFrameNumber;
    @FXML
    public TextField currentFrameNumber;
    @FXML
    public ToggleSwitch switchDrawPointsOrCells;
    @FXML
    public Button buttonDumpDulov;

    public SpartaMonitorController() {
        this.fgThread = new Thread(frameGenerator);
        this.fgThread.start();
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;

        selectColorizeType.getSelectionModel().select(0);
        if (!Config.parsPoints) {
            drawByPoints = Config.parsPoints;
            switchDrawPointsOrCells.setSelected(true);
            switchDrawPointsOrCells.setDisable(true);
        }
    }

    protected void loadConfig() {
        Config.dumpDirPath = textDumpFolder.getText();
    }

    @FXML
    protected void onInitiateButtonClick() throws IOException {
        loadConfig();

        frameGenerator.setDumpDir(this.textDumpFolder.getText());
        frameGenerator.loadInFile(Path.of(this.textDumpFolder.getText()));

        animationCanvas.drawMask(frameGenerator);
        graduationCanvas.colorize(ColorizeType.DENSITY_STATIC);

        frameGenerator.preloadTimeFrames(Integer.parseInt(this.startFrameNumber.getText()),
                Integer.parseInt(this.endFrameNumber.getText()));

        buttonBackward.setDisable(false);
        buttonForward.setDisable(false);
        buttonStop.setDisable(true);
        buttonPrevStep.setDisable(false);
        buttonNextStep.setDisable(false);
    }

    @FXML
    protected void onButtonSaveAsPictureClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG files", "*.png"));
        File file = fileChooser.showSaveDialog(this.mainStage);

        try {
            RenderedImage mainImage = Screener.getImageFromCanvas(animationCanvas);
            RenderedImage graduationImage = Screener.getImageFromCanvas(graduationCanvas);
            RenderedImage chartImage = Screener.getImageFromCanvas(densityChart);
            RenderedImage outImage = Screener.combineFullScene(mainImage, graduationImage, chartImage);
            ImageIO.write(outImage, "png", file);
            System.out.printf("Screen saved to '%s'%n", file.getAbsolutePath());
        } catch (Exception ignore) {
        }
    }

    @FXML
    protected void onNextIterationButtonClick() {
        EventBusFactory.getEventBus().post(new DrawEvent(1));
        frameGenerator.startOneIteration();
    }

    @FXML
    protected void onPrevIterationButtonClick() {
        EventBusFactory.getEventBus().post(new DrawEvent(-1));
        frameGenerator.startOneIteration();
    }

    @FXML
    protected void onForwardIterationsButtonClick() {
        buttonInit.setDisable(true);
        buttonBackward.setDisable(true);
        buttonForward.setDisable(true);
        buttonStop.setDisable(false);
        buttonPrevStep.setDisable(true);
        buttonNextStep.setDisable(true);
        EventBusFactory.getEventBus().post(new DrawEvent(1));
        frameGenerator.startIterations();
    }

    @FXML
    protected void onBackwardIterationsButtonClick() {
        buttonInit.setDisable(true);
        buttonBackward.setDisable(true);
        buttonForward.setDisable(true);
        buttonStop.setDisable(false);
        buttonPrevStep.setDisable(true);
        buttonNextStep.setDisable(true);
        EventBusFactory.getEventBus().post(new DrawEvent(-1));
        frameGenerator.startIterations();
    }

    @FXML
    protected void onStopIterationsButtonClick() {
        frameGenerator.stopIteration();
        buttonInit.setDisable(false);
        buttonBackward.setDisable(false);
        buttonForward.setDisable(false);
        buttonStop.setDisable(true);
        buttonPrevStep.setDisable(false);
        buttonNextStep.setDisable(false);
    }

    @FXML
    protected void onClose() {
        this.frameGenerator.isAlive = false;
        this.fgThread.interrupt();
    }

    @FXML
    protected void onButtonDumpFolderClick() {
        DirectoryChooser fileChooser = new DirectoryChooser();
        fileChooser.setTitle("Select dump dir");
        File selectedFile = fileChooser.showDialog(this.mainStage);
        if (selectedFile != null) {
            textDumpFolder.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    protected void onColorizeTypeChange() {
        switch (selectColorizeType.getSelectionModel().getSelectedIndex()) {
            case 0 -> colorizeType = ColorizeType.DENSITY_STATIC;
            case 1 -> colorizeType = ColorizeType.DENSITY_DYNAMIC;
            case 2 -> colorizeType = ColorizeType.TEMPERATURE;
            case 3 -> colorizeType = ColorizeType.VELOCITY;
            case 4 -> colorizeType = ColorizeType.SOUND_VELOCITY;
            case 5 -> colorizeType = ColorizeType.MACH;
            case 6 -> colorizeType = ColorizeType.BIND;
            case 7 -> colorizeType = ColorizeType.N_COUNT;
            case 8 -> colorizeType = ColorizeType.NRHO;
            case 9 -> colorizeType = ColorizeType.DENSITY_STATIC_DIF;
            case 10 -> colorizeType = ColorizeType.DENSITY_DYNAMIC_DIF;
            case 11 -> colorizeType = ColorizeType.NRHO_DIF;
            case 12 -> colorizeType = ColorizeType.NRHO_DULOV;
        }
        graduationCanvas.colorize(colorizeType);
        EventBusFactory.getEventBus().post(new DrawEvent(0));
    }

    @FXML
    protected void onDrawTypeChange() {
        drawByPoints = !switchDrawPointsOrCells.isSelected();
        EventBusFactory.getEventBus().post(new DrawEvent(0));
    }

    @FXML
    protected void onDumpDulovButtonClick() {
        frameGenerator.saveDulovsData(Path.of(this.textDumpFolder.getText()));
    }

    public void drawIteration(int playDirection) {
        if (drawIterationFinished && (frameGenerator.isRunning || frameGenerator.showOneIteration)) {
            drawIterationFinished = false;

            FrameGenerator.Frame frame;
            do {
                frame = this.frameGenerator.getFrame(playDirection);
            } while (frame.timeframe == null);

            frameGenerator.showOneIteration = Boolean.FALSE;

            Logger.startTimer("Draw iteration");

            String title = String.format("%.0f мкс", (frame.frameNumber) * (tStep / 1e-6 * 100));
            animationCanvas.drawIteration(this.frameGenerator, frame, colorizeType, drawByPoints, title);
            densityChart.drawIteration(frame, colorizeType);

            Toolkit.getDefaultToolkit().sync();

            Logger.releaseTimer("Draw iteration");

            drawIterationFinished = true;
        }
    }

    public void drawDensityChart(Float xCoord) {
        FrameGenerator.Frame frame;
        do {
            frame = this.frameGenerator.getFrame(0);
        } while (frame.timeframe == null);

        DensityChart.dulovXLine = xCoord;
        densityChart.drawIteration(frame, colorizeType);

        Toolkit.getDefaultToolkit().sync();
    }
}