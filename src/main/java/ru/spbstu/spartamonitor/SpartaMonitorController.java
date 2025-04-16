package ru.spbstu.spartamonitor;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.spbstu.spartamonitor.calculate.Calculation;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.data.ParserEvent;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.logger.Logger;
import ru.spbstu.spartamonitor.screener.Screener;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema;
import static ru.spbstu.spartamonitor.config.Config.*;

public class SpartaMonitorController {

    private final FrameGenerator frameGenerator = new FrameGenerator();
    private final Thread fgThread;
    private volatile boolean drawIterationFinished = true;
    private Stage mainStage;
    private float zoom = 1f;
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
    protected Canvas animationCanvas;
    @FXML
    protected Canvas graduationCanvas;
    @FXML
    protected LineChart<String, Number> densityLineChart;
    @FXML
    protected TextField textDumpFolder;
    @FXML
    protected Button buttonDumpFolder;
    @FXML
    protected ToggleGroup radioColorizeType;
    @FXML
    protected TextField textCountFrames;

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

        textCountFrames.addEventHandler(ParserEvent.CHANGE_TIMEFRAME_COUNT,
                event -> textCountFrames.setText(String.valueOf(frameGenerator.timeframes.size())));
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

        System.out.printf("Offset by X is %.2f%n", offsetX);
        System.out.printf("Offset by Y is %.2f%n", offsetY);

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

        drawMask();
        colorizeGraduation(ColorizeType.DENSITY);

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
            RenderedImage chartImage = Screener.getImageFromCanvas(densityLineChart);
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
        int denominator = this.zoom > 50 ? 50 : this.zoom > 10 ? 100 : 200;
        this.zoom += (float) event.getDeltaY() / denominator;
        if (this.zoom < 1) {
            this.zoom = 1;
        } else if (this.zoom > 100) {
            this.zoom = 100;
        }
        if (zoom == 1) {
            multiplayer = defaultMultiplayer;
            mainBoxX = defaultBoxX;
            mainBoxY = defaultBoxY;
        } else {
            multiplayer = (int) (defaultMultiplayer * zoom);
            mainBoxX = (int) (defaultBoxX * zoom);
            mainBoxY = (int) (defaultBoxY * zoom);
        }

        shiftBoxX -= (int) event.getDeltaY();
        if (shiftBoxX > 0) {
            shiftBoxX = 0;
        }
        //shiftBoxY -= (int) (newCoordX - oldCoordX);

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
        colorizeGraduation(colorizeType);
        redraw();
    }

    protected void redraw() {
        if (!frameGenerator.isRunning && !frameGenerator.showOneIteration) {
            playDirection = 0;
            frameGenerator.showOneIteration = Boolean.TRUE;
        }
    }

    protected void drawAxes() {
        GraphicsContext gc = this.animationCanvas.getGraphicsContext2D();
        gc.setFill(Color.GRAY);

        for (int i = 0; i <= shapeX; i++) {
            gc.fillRect(i * multiplayer - 1, shiftBoxY + mainBoxY - 6, 1, 6);
        }
        for (int i = 0; i <= shapeX; i += 5) {
            gc.fillRect(i * multiplayer - 1, shiftBoxY + mainBoxY - 10, 1, 10);
        }
    }

    protected void drawMask() {
        GraphicsContext gc = this.animationCanvas.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.clearRect(0, 0, this.animationCanvas.getWidth(), this.animationCanvas.getHeight());
        gc.setFill(Color.AZURE);
        gc.fillRect(shiftBoxX, shiftBoxY, mainBoxX, mainBoxY);

        for (Polygon surf : frameGenerator.getListSurfs()) {
            List<Double> xs = new ArrayList<>();
            List<Double> ys = new ArrayList<>();
            for (Point surfPoint : surf.getPoints()) {
                xs.add((double) (shiftBoxX + surfPoint.x * multiplayer));
                ys.add((double) (shiftBoxY + surfPoint.y * multiplayer));
            }
            gc.setFill(Color.GRAY);
            gc.fillPolygon(xs.stream().mapToDouble(Double::doubleValue).toArray(),
                    ys.stream().mapToDouble(Double::doubleValue).toArray(),
                    xs.size());
        }

        drawAxes();

        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, 45, 22);
        gc.setFill(Color.WHITE);
        gc.fillText(String.format("%.2f X", zoom), 5, 15);
    }

    protected void colorizeGraduation(ColorizeType graduation) {
        GraphicsContext gc = this.graduationCanvas.getGraphicsContext2D();
        double colorStep = graduationCanvas.getWidth() / colorSchema.size();
        int countTextSteps = (graduation.maxValue - graduation.minValue) / graduation.stepValue;
        int countSmallTextSteps = (graduation.maxValue - graduation.minValue) / graduation.smallStepValue;
        double textStep = colorSchema.size() * colorStep / countTextSteps;
        double smallTextStep = colorSchema.size() * colorStep / countSmallTextSteps;

        gc.clearRect(0, 0, this.graduationCanvas.getWidth(), this.graduationCanvas.getHeight());

        for (int i = 0; i < colorSchema.size(); i++) {
            gc.setFill(colorSchema.get(i));
            gc.fillRect(colorStep * i, 40, colorStep, 40);
        }

        gc.setFill(Color.GRAY);
        gc.fillText(String.valueOf(graduation.minValue), 0, 25);
        gc.fillRect(0, 30, 2, 10);
        for (int i = 1; i < countTextSteps; i++) {
            String text = String.valueOf(graduation.minValue + graduation.stepValue * i);
            gc.fillText(text, i * textStep - 7, 25);
            gc.fillRect(i * textStep - 1, 30, 2, 10);
        }
        gc.fillText(String.valueOf(graduation.maxValue), countTextSteps * textStep - 25, 25);
        gc.fillRect(colorStep * colorSchema.size() - 2, 30, 2, 10);

        for (int i = 1; i < countSmallTextSteps; i++) {
            gc.fillRect(i * smallTextStep, 34, 1, 6);
        }

        gc.fillText(graduation.label, (countTextSteps * textStep) / 2 - 40, 10);
    }

    protected Color getColorForType(float value, ColorizeType colorizeType) {
        if (value > colorizeType.maxValue) {
            return colorSchema.getLast();
        } else if (value < colorizeType.minValue) {
            return colorSchema.getFirst();
        }
        return colorSchema.get((int) value * (colorSchema.size() - 1) / (colorizeType.maxValue - colorizeType.minValue));
    }

    protected void colorizePoints(GraphicsContext gc, FrameGenerator.Frame frame, ColorizeType colorizeType) {
        float xLoBorder = -shiftBoxX * monitorCellSize / zoom;
        float xHiBorder = (maxBoxX - shiftBoxX) * monitorCellSize / zoom;
        float yLoBorder = -shiftBoxY * monitorCellSize / zoom;
        float yHiBorder = (maxBoxY - shiftBoxY) * monitorCellSize / zoom;
        int countPoints = 0;
        for (Number[] point : frame.timeframe.getPoints()) {
            if (point[1].floatValue() < xLoBorder || xHiBorder < point[1].floatValue()) {
                continue;
            } else if (point[2].floatValue() < yLoBorder || yHiBorder < point[2].floatValue()) {
                continue;
            }
            try {
                int pointId = point[3].intValue();
                if (!frame.timeframe.getGrid().getCells().containsKey(pointId)) {
                    gc.setFill(Color.BLACK);
                } else if (colorizeType == ColorizeType.DENSITY) {
                    gc.setFill(getColorForType(frame.timeframe.getGrid().getCells().get(pointId)[0], colorizeType));
                } else if (colorizeType == ColorizeType.TEMPERATURE) {
                    gc.setFill(getColorForType(frame.timeframe.getGrid().getCells().get(pointId)[1], colorizeType));
                } else if (colorizeType == ColorizeType.VELOCITY) {
                    gc.setFill(getColorForType(frame.timeframe.getGrid().getCells().get(pointId)[2], colorizeType));
                } else if (colorizeType == ColorizeType.MACH) {
                    gc.setFill(getColorForType(frame.timeframe.getGrid().getCells().get(pointId)[3], colorizeType));
                }
            } catch (Exception ignore) {
                gc.setFill(Color.YELLOW);
            }
            gc.fillOval(shiftBoxX + point[1].floatValue() * multiplayer,
                    shiftBoxY + point[2].floatValue() * multiplayer, 1, 1);
            countPoints++;
        }
        textCountFrames.setText(String.format("%d: %.3f %.3f", countPoints, yLoBorder, yHiBorder));
    }

    protected void drawIteration() {
        if (drawIterationFinished && (frameGenerator.isRunning || frameGenerator.showOneIteration)) {
            drawIterationFinished = false;

            FrameGenerator.Frame frame = this.frameGenerator.getFrame(playDirection);

            Logger.startTimer("Draw iteration");

            frameGenerator.showOneIteration = Boolean.FALSE;

            GraphicsContext gc = this.animationCanvas.getGraphicsContext2D();
            gc.setImageSmoothing(true);

            drawMask();

            colorizePoints(gc, frame, colorizeType);

            this.alertText.setText(String.format("%.0f мкс", (frame.frameNumber + 1) * (tStep / 1e-6 * 100)));

            int countPoints = 0;
            float targetDiameter = 0f;
            if (densityLineChart.getData().isEmpty()) {
                if (frame.timeframe.getTarget() != null) {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    for (int i = 0; i < frame.timeframe.getTarget().length; i++) {
                        series.getData().add(new XYChart.Data<>(String.valueOf(i * 8 / 10), 0));
                    }
                    densityLineChart.getData().add(series);

                    XYChart.Series<String, Number> series2 = new XYChart.Series<>();
                    series2.getData().add(new XYChart.Data<>("0", 0));
                    series2.getData().add(new XYChart.Data<>("0", 0));
                    densityLineChart.getData().add(series2);

                    XYChart.Series<String, Number> series3 = new XYChart.Series<>();
                    series3.getData().add(new XYChart.Data<>("0", 0));
                    series3.getData().add(new XYChart.Data<>("0", 0));
                    densityLineChart.getData().add(series3);
                }
            } else {
                if (frame.timeframe != null && frame.timeframe.getTarget() != null) {
                    int maxY = 0;
                    Calculation.Diameter diameter = Calculation.calculateTargetDiameter(frame.timeframe, 0.5f);
                    for (int i = 0; i < frame.timeframe.getTarget().length; i++) {
                        XYChart.Data<String, Number> element =
                                densityLineChart.getData().getFirst().getData().get(i);
                        element.setYValue(frame.timeframe.getTarget()[i]);
                        maxY = Math.max(maxY, frame.timeframe.getTarget()[i]);
                        countPoints += frame.timeframe.getTarget()[i];
                    }
                    densityLineChart.getData().get(1).getData().getFirst().setXValue(String.valueOf(diameter.leftBorder / 10));
                    densityLineChart.getData().get(1).getData().getFirst().setYValue(0);
                    densityLineChart.getData().get(1).getData().get(1).setXValue(String.valueOf(diameter.leftBorder / 10));
                    densityLineChart.getData().get(1).getData().get(1).setYValue(maxY);
                    densityLineChart.getData().get(2).getData().getFirst().setXValue(String.valueOf(diameter.rightBorder / 10));
                    densityLineChart.getData().get(2).getData().getFirst().setYValue(0);
                    densityLineChart.getData().get(2).getData().get(1).setXValue(String.valueOf(diameter.rightBorder / 10));
                    densityLineChart.getData().get(2).getData().get(1).setYValue(maxY);

                    targetDiameter = (float) diameter.diameter / 8;
                } else {
                    densityLineChart.getData().get(0).getData().forEach(element -> element.setYValue(0));
                    densityLineChart.getData().get(1).getData().forEach(element -> element.setYValue(0));
                    densityLineChart.getData().get(2).getData().forEach(element -> element.setYValue(0));
                }
            }

            densityLineChart.setTitle(String.format("Плотность частиц: %d%n" +
                    "Диаметр: %.1f мм", countPoints, targetDiameter));

            Toolkit.getDefaultToolkit().sync();

            Logger.releaseTimer("Draw iteration");

            drawIterationFinished = true;
        }
    }
}