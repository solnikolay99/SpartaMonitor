package ru.spbstu.spartamonitor;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static ru.spbstu.spartamonitor.config.Config.*;

public class SpartaMonitorController {

    public enum COLORIZE_TYPE {
        DENSITY, TEMPERATURE
    }

    private final FrameGenerator frameGenerator = new FrameGenerator();
    private final Thread fgThread;
    private Stage mainStage;
    private float zoom = 1f;
    private int playDirection = 1; // направление проигрывания: 1 - в прямом порядке; -1 - в обратном порядке
    private static final List<Color> colorSchema = new ArrayList<>();
    private COLORIZE_TYPE colorizeType = COLORIZE_TYPE.DENSITY;

    static {
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(127, 127, 255 - i));
        }
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(127, 165 + i, 127));
        }
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(165 + i, 127, 127));
        }
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(127, 195 + i, 195 + i));
        }
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(165 + i, 165 + i, 127));
        }
        for (int i = 0; i < 60; i++) {
            colorSchema.add(Color.rgb(165 + i, 127, 165 + i));
        }

        //Color.rgb(255, 127, 127);
    }

    @FXML
    public Button buttonInit;
    @FXML
    public Button buttonForward;
    @FXML
    public Button buttonBackward;
    @FXML
    public Button buttonStop;
    @FXML
    public Button buttonPrevStep;
    @FXML
    public Button buttonNextStep;
    @FXML
    protected Label alertText;
    @FXML
    protected Canvas animationCanvas;
    @FXML
    protected Canvas graduationCanvas;
    @FXML
    protected BarChart<Number, String> densityBarChart;
    @FXML
    protected BarChart<String, Number> pumpingBarChart1;
    @FXML
    protected BarChart<String, Number> pumpingBarChart2;
    @FXML
    protected TextField textInFile;
    @FXML
    protected Button buttonInFile;
    @FXML
    protected TextField textDumpFolder;
    @FXML
    protected Button buttonDumpFolder;
    @FXML
    protected ToggleGroup radioColorizeType;

    public SpartaMonitorController() {
        this.fgThread = new Thread(frameGenerator);
        this.fgThread.start();
    }

    public void setMainStage(Stage stage) {
        this.mainStage = stage;

        animationCanvas.setOnScroll(this::onZooming);
        //sliderZoom.valueProperty().addListener((event) -> onZooming());
    }

    protected void loadConfig() {
        Config.dumpDirPath = textDumpFolder.getText();
    }

    @FXML
    protected void onInitiateButtonClick() throws IOException {
        loadConfig();

        frameGenerator.loadInFile(Path.of(this.textInFile.getText()));

        drawMask();
        colorizeGraduation(0, 360, 30);

        frameGenerator.preloadTimeFrames(this.textDumpFolder.getText());

        alertText.setText("0.0000");

        buttonBackward.setDisable(false);
        buttonForward.setDisable(false);
        buttonStop.setDisable(true);
        buttonPrevStep.setDisable(false);
        buttonNextStep.setDisable(false);
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
        this.fgThread.interrupt();
    }

    @FXML
    protected void onButtonInFileClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select in file");
        File selectedFile = fileChooser.showOpenDialog(this.mainStage);
        if (selectedFile != null) {
            textInFile.setText(selectedFile.getAbsolutePath());
        }
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
        //System.out.printf("Zooming start %n");
        /*
        System.out.printf("Zooming end with zoom value %.2f: x=%.2f, y=%.2f %n",
                event.getDeltaY() / 400, event.getSceneX(), event.getSceneY());
        */
        this.zoom += (float) event.getDeltaY() / 400;
        if (this.zoom < 1) {
            this.zoom = 1;
        } else if (this.zoom > 10) {
            this.zoom = 10;
        }
        if (zoom == 1) {
            multiplayer = (int) (defaultMultiplayer * coeffXY);
            mainBoxX = defaultBoxX;
            mainBoxY = defaultBoxY;
        } else {
            multiplayer = (int) (defaultMultiplayer * coeffXY * zoom);
            mainBoxX = (int) (defaultBoxX * zoom);
            mainBoxY = (int) (defaultBoxY * zoom);
        }
        if (mainBoxX > maxBoxX) {
            mainBoxX = maxBoxX;
        }
        if (mainBoxY > maxBoxY) {
            mainBoxY = maxBoxY;
        }

        shiftBoxY = (maxBoxY - mainBoxY) / 2;

        if (!frameGenerator.isRunning && !frameGenerator.showOneIteration) {
            frameGenerator.setPrevFrame();
            frameGenerator.showOneIteration = Boolean.TRUE;
        }
        //System.out.printf("Zooming end with zoom value %.2f: x = %d; y = %d %n", zoom, mainBoxX, mainBoxY);
    }

    @FXML
    protected void onColorizeTypeChange() {
        if (((RadioButton) radioColorizeType.getSelectedToggle()).getText().equals("Давление")) {
            colorizeType = COLORIZE_TYPE.DENSITY;
        } else if (((RadioButton) radioColorizeType.getSelectedToggle()).getText().equals("Температура")) {
            colorizeType = COLORIZE_TYPE.TEMPERATURE;
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
    }

    protected void colorizeGraduation(int minValue, int maxValue, int stepValue) {
        GraphicsContext gc = this.graduationCanvas.getGraphicsContext2D();
        int colorStep = (int) (graduationCanvas.getWidth() / colorSchema.size());
        int countTextSteps = (maxValue - minValue) / stepValue;
        int textStep = colorSchema.size() * colorStep / countTextSteps;

        gc.clearRect(0, 0, this.graduationCanvas.getWidth(), this.graduationCanvas.getHeight());

        for (int i = 0; i < colorSchema.size(); i++) {
            gc.setFill(colorSchema.get(i));
            gc.fillRect(colorStep * i, 40, colorStep, 40);
        }

        gc.setFill(Color.BLACK);
        gc.fillText(String.valueOf(minValue), 0, 20);
        gc.fillRect(0, 25, 2, 15);
        for (int i = 1; i < countTextSteps; i++) {
            String text = String.valueOf(minValue + stepValue * i);
            gc.fillText(text, i * textStep - 7, 20);
            gc.fillRect(i * textStep - 1, 25, 2, 15);
        }
        gc.fillText(String.valueOf(maxValue), countTextSteps * textStep - 25, 20);
        gc.fillRect(colorStep * colorSchema.size() - 2, 25, 2, 15);
    }

    protected Color getColorForDensity(float density) {
        if (density > colorSchema.size() - 1) {
            density = colorSchema.size() - 1;
        } else if (density < 0) {
            density = 0;
        }
        return colorSchema.get((int) density);
    }

    protected Color getColorForTemperature(float temperature) {
        if (temperature > colorSchema.size() - 1) {
            temperature = colorSchema.size() - 1;
        } else if (temperature < 0) {
            temperature = 0;
        }
        return colorSchema.get((int) temperature);
    }

    protected void colorizePoints(GraphicsContext gc, FrameGenerator.NextFrame frame, COLORIZE_TYPE colorizeType) {
        int countErrors = 0;
        for (Number[] point : frame.timeframe.getPoints()) {
            try {
                if (colorizeType == COLORIZE_TYPE.DENSITY) {
                    gc.setFill(getColorForDensity(frame.timeframe.getGrid().getCells().get((int) point[3].longValue())[0]));
                } else if (colorizeType == COLORIZE_TYPE.TEMPERATURE) {
                    gc.setFill(getColorForTemperature(frame.timeframe.getGrid().getCells().get((int) point[3].longValue())[1]));
                }
            } catch (Exception ignore) {
                gc.setFill(Color.BLACK);
                countErrors++;
            }
            gc.fillOval(shiftBoxX + point[1].floatValue() * multiplayer,
                    shiftBoxY + point[2].floatValue() * multiplayer, 1, 1);
        }
        //System.out.printf("Count errors in colorize method: %d of %d points%n", countErrors, frame.timeframe.getPoints().length);
    }

    protected void drawIteration() {
        if ((frameGenerator.isRunning || frameGenerator.showOneIteration)) {
            FrameGenerator.NextFrame frame = this.frameGenerator.getFrame(playDirection);

            Logger.startTimer("Draw iteration");

            frameGenerator.showOneIteration = Boolean.FALSE;

            GraphicsContext gc = this.animationCanvas.getGraphicsContext2D();
            gc.setImageSmoothing(true);

            drawMask();

            if (colorizeType == COLORIZE_TYPE.DENSITY) {
                colorizeGraduation(0, 360, 30);
            } else if (colorizeType == COLORIZE_TYPE.TEMPERATURE) {
                colorizeGraduation(0, 450, 30);
            }

            colorizePoints(gc, frame, colorizeType);

            this.alertText.setText(String.format("%.0f мкс", (frame.frameNumber + 1) * (Config.tStep / 1e-6 * 100)));

            int countPoints = 0;
            if (densityBarChart.getData().isEmpty()) {
                XYChart.Series<Number, String> series = new XYChart.Series<>();
                for (int i = 0; i < Config.mainBoxY; i++) {
                    series.getData().add(new XYChart.Data<>(0, String.valueOf(i)));
                }
                densityBarChart.getData().add(series);
            } else {
                for (int i = 0; i < frame.listOutPoints.size(); i++) {
                    XYChart.Data<Number, String> element =
                            densityBarChart.getData().getFirst().getData().get(i);
                    element.setXValue(frame.listOutPoints.get(i));
                    countPoints += frame.listOutPoints.get(i);
                }
            }

            /*
            if (pumpingBarChart1.getData().isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                for (int i = 0; i < Config.mainBoxX; i++) {
                    series.getData().add(new XYChart.Data<>(String.valueOf(i), 0));
                }
                pumpingBarChart1.getData().add(series);
            } else {
                for (int i = 0; i < nextFrame.listPumpingPoints1.size(); i++) {
                    XYChart.Data<String, Number> element =
                            pumpingBarChart1.getData().getFirst().getData().get(i);
                    element.setYValue(nextFrame.listPumpingPoints1.get(i));
                }
            }

            if (pumpingBarChart2.getData().isEmpty()) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                for (int i = 0; i < Config.mainBoxX; i++) {
                    series.getData().add(new XYChart.Data<>(String.valueOf(i), 0));
                }
                pumpingBarChart2.getData().add(series);
            } else {
                for (int i = 0; i < nextFrame.listPumpingPoints2.size(); i++) {
                    XYChart.Data<String, Number> element =
                            pumpingBarChart2.getData().getFirst().getData().get(i);
                    element.setYValue(-nextFrame.listPumpingPoints2.get(i));
                }
            }

            densityBarChart.setTitle(String.format("Плотность частиц: %d", countPoints));
            */

            Toolkit.getDefaultToolkit().sync();

            /*
            float iterationTime = Logger.releaseTimer("Draw iteration");
            if (iterationTime < frameTimeDelta) {
                try {
                    Thread.sleep((int) (iterationTime - frameTimeDelta));
                } catch (Exception ignore) {
                }
            }
            /**/
        }
    }
}