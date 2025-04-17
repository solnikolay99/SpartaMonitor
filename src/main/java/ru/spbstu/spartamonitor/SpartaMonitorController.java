package ru.spbstu.spartamonitor;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
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

    public final FrameGenerator frameGenerator = new FrameGenerator();
    private final Thread fgThread;
    private volatile boolean drawIterationFinished = true;
    private Stage mainStage;
    private ColorizeType colorizeType = ColorizeType.DENSITY;

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
    protected ToggleGroup radioColorizeType;
    @FXML
    public TextField textCountFrames;

    public SpartaMonitorController() {
        this.fgThread = new Thread(frameGenerator);
        this.fgThread.start();
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;
    }

    protected void loadConfig() {
        Config.dumpDirPath = textDumpFolder.getText();
    }

    @FXML
    protected void onInitiateButtonClick() throws IOException {
        loadConfig();

        frameGenerator.setDumpDir(this.textDumpFolder.getText());
        frameGenerator.loadInFile(Path.of(this.textDumpFolder.getText(), "in.step"));

        animationCanvas.drawMask(frameGenerator);
        graduationCanvas.colorize(ColorizeType.DENSITY);

        frameGenerator.preloadTimeFrames();

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
        switch (((RadioButton) radioColorizeType.getSelectedToggle()).getText()) {
            case "Давление" -> colorizeType = ColorizeType.DENSITY;
            case "Температура" -> colorizeType = ColorizeType.TEMPERATURE;
            case "Скорость" -> colorizeType = ColorizeType.VELOCITY;
            case "Скорость звука" -> colorizeType = ColorizeType.SOUND_VELOCITY;
            case "Число Маха" -> colorizeType = ColorizeType.MACH;
        }
        graduationCanvas.colorize(colorizeType);
        EventBusFactory.getEventBus().post(new DrawEvent(0));
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
            animationCanvas.drawIteration(this.frameGenerator, frame, colorizeType, title);
            densityChart.drawIteration(frame);

            Toolkit.getDefaultToolkit().sync();

            Logger.releaseTimer("Draw iteration");

            drawIterationFinished = true;
        }
    }
}