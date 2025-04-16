package ru.spbstu.spartamonitor;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.spbstu.spartamonitor.canvas.DensityChart;
import ru.spbstu.spartamonitor.canvas.GraduationCanvas;
import ru.spbstu.spartamonitor.canvas.MainCanvas;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.logger.Logger;
import ru.spbstu.spartamonitor.screener.Screener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static ru.spbstu.spartamonitor.config.Config.*;

public class SpartaMonitorController {

    private final FrameGenerator frameGenerator = new FrameGenerator();
    private final Thread fgThread;
    private volatile boolean drawIterationFinished = true;
    private Stage mainStage;
    private int playDirection = 1; // направление проигрывания: 1 - в прямом порядке; -1 - в обратном порядке
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
    protected Label alertText;
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

    private double animatedCanvasX = 0;
    private double animatedCanvasY = 0;
    private int originalShiftX = 0;
    private int originalShiftY = 0;

    public SpartaMonitorController() {
        this.fgThread = new Thread(frameGenerator);
        this.fgThread.start();
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;

        animationCanvas.setOnScroll(this::onZooming);
        animationCanvas.setOnMousePressed(canvasOnMousePressedEventHandler);
        animationCanvas.setOnMouseDragged(canvasOnMouseDraggedEventHandler);
    }

    EventHandler<MouseEvent> canvasOnMousePressedEventHandler = mouseEvent -> {
        animatedCanvasX = mouseEvent.getSceneX();
        animatedCanvasY = mouseEvent.getSceneY();
        originalShiftX = shiftBoxX;
        originalShiftY = shiftBoxY;
    };

    EventHandler<MouseEvent> canvasOnMouseDraggedEventHandler = mouseEvent -> {
        double offsetX = mouseEvent.getSceneX() - animatedCanvasX;
        double offsetY = mouseEvent.getSceneY() - animatedCanvasY;
        shiftBoxX = originalShiftX + (int) offsetX;
        shiftBoxY = originalShiftY + (int) offsetY;
        redraw();
    };

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

        alertText.setText("0.0000");

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
        playDirection = 1;
        frameGenerator.startOneIteration();
    }

    @FXML
    protected void onPrevIterationButtonClick() {
        playDirection = -1;
        frameGenerator.startOneIteration();
    }

    @FXML
    protected void onForwardIterationsButtonClick() {
        playDirection = 1;
        buttonInit.setDisable(true);
        buttonBackward.setDisable(true);
        buttonForward.setDisable(true);
        buttonStop.setDisable(false);
        buttonPrevStep.setDisable(true);
        buttonNextStep.setDisable(true);
        frameGenerator.startIterations();
    }

    @FXML
    protected void onBackwardIterationsButtonClick() {
        playDirection = -1;
        buttonInit.setDisable(true);
        buttonBackward.setDisable(true);
        buttonForward.setDisable(true);
        buttonStop.setDisable(false);
        buttonPrevStep.setDisable(true);
        buttonNextStep.setDisable(true);
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
    protected void onZooming(ScrollEvent event) {
        int denominator = this.animationCanvas.getZoom() > 50 ? 50 : this.animationCanvas.getZoom() > 10 ? 100 : 200;
        this.animationCanvas.changeZoom((float) event.getDeltaY() / denominator);
        if (this.animationCanvas.getZoom() == 1) {
            multiplayer = defaultMultiplayer;
            mainBoxX = defaultBoxX;
            mainBoxY = defaultBoxY;
        } else {
            multiplayer = (int) (defaultMultiplayer * this.animationCanvas.getZoom());
            mainBoxX = (int) (defaultBoxX * this.animationCanvas.getZoom());
            mainBoxY = (int) (defaultBoxY * this.animationCanvas.getZoom());
        }

        shiftBoxX -= (int) event.getDeltaY();
        if (shiftBoxX > 0) {
            shiftBoxX = 0;
        }

        if (mainBoxX + shiftBoxX > maxBoxX) {
            mainBoxX = maxBoxX - shiftBoxX;
        }

        shiftBoxY = (maxBoxY - mainBoxY) / 2;

        redraw();
    }

    @FXML
    protected void onColorizeTypeChange() {
        switch (((RadioButton) radioColorizeType.getSelectedToggle()).getText()) {
            case "Давление" -> colorizeType = ColorizeType.DENSITY;
            case "Температура" -> colorizeType = ColorizeType.TEMPERATURE;
            case "Скорость" -> colorizeType = ColorizeType.VELOCITY;
            case "Число Маха" -> colorizeType = ColorizeType.MACH;
        }
        graduationCanvas.colorize(colorizeType);
        redraw();
    }

    protected void redraw() {
        if (!frameGenerator.isRunning && !frameGenerator.showOneIteration) {
            playDirection = 0;
            frameGenerator.showOneIteration = Boolean.TRUE;
        }
    }

    public void drawIteration() {
        if (drawIterationFinished && (frameGenerator.isRunning || frameGenerator.showOneIteration)) {
            drawIterationFinished = false;

            FrameGenerator.Frame frame = this.frameGenerator.getFrame(playDirection);

            frameGenerator.showOneIteration = Boolean.FALSE;

            Logger.startTimer("Draw iteration");

            animationCanvas.drawIteration(this.frameGenerator, frame, colorizeType);
            densityChart.drawIteration(frame);

            this.alertText.setText(String.format("%.0f мкс", (frame.frameNumber + 1) * (tStep / 1e-6 * 100)));

            Toolkit.getDefaultToolkit().sync();

            Logger.releaseTimer("Draw iteration");

            drawIterationFinished = true;
        }
    }
}