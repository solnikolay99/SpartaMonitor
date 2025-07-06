package ru.spbstu.spartamonitor.canvas;

import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import ru.spbstu.spartamonitor.calculate.Calculation;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.data.Parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class DensityChart extends LineChart<String, Number> {

    private ColorizeType curColorizeType = ColorizeType.DENSITY_STATIC;
    public static float dulovXLine = 0.5f + 0.005f * 250;
    public static float dulovYLine = 2.0f;

    public DensityChart(@NamedArg("xAxis") Axis<String> xAxis,
                        @NamedArg("yAxis") Axis<Number> yAxis,
                        @NamedArg("data") ObservableList<Series<String, Number>> data) {
        super(xAxis, yAxis, data);
    }

    public DensityChart(@NamedArg("xAxis") Axis<String> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
        super(xAxis, yAxis);
    }

    public void drawIteration(FrameGenerator.Frame frame, ColorizeType colorizeType) {
        if (curColorizeType != colorizeType) {
            this.getData().clear();
            curColorizeType = colorizeType;
        }
        if (colorizeType == ColorizeType.DENSITY_STATIC_DIF
                || colorizeType == ColorizeType.DENSITY_DYNAMIC_DIF
                || colorizeType == ColorizeType.NRHO_DIF) {
            showDulovDiffData(frame);
        } else {
            showTargetData(frame);
        }
    }

    protected Float getDulovData(int cellId) {
        if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF || curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                return FrameGenerator.dulovsPressureData.get(cellId);
            }
        } else if (curColorizeType == ColorizeType.NRHO_DIF) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                return FrameGenerator.dulovsNConcentrationData.get(cellId);
            }
        }
        return null;
    }

    protected float getCellValue(FrameGenerator.Frame frame, int cellId) {
        if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
            return frame.timeframe.getGrid().getCells().get(cellId)[0];
        } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
            return frame.timeframe.getGrid().getCells().get(cellId)[7];
        } else if (curColorizeType == ColorizeType.NRHO_DIF) {
            return frame.timeframe.getGrid().getCells().get(cellId)[6];
        } else {
            return 0f;
        }
    }

    public void showDulovDiffData(FrameGenerator.Frame frame) {
        HashMap<Integer, Float> dulovData = new HashMap<>();

        this.getData().clear();

        if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF || curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
            for (int cellId : frame.timeframe.getGrid().getCells().keySet()) {
                Parser.GridCell gridCell = FrameGenerator.gridSchema.get(cellId);
                if (gridCell.xLo < 0.6f) {
                    continue;
                }
                if (gridCell.yLo <= dulovYLine && dulovYLine < gridCell.yHi) {
                    dulovData.put(cellId, gridCell.xLo);
                }
            }
        } else if (curColorizeType == ColorizeType.NRHO_DIF) {
            for (int cellId : frame.timeframe.getGrid().getCells().keySet()) {
                Parser.GridCell gridCell = FrameGenerator.gridSchema.get(cellId);
                if (gridCell.xLo <= dulovXLine && dulovXLine < gridCell.xHi) {
                    dulovData.put(cellId, gridCell.yLo);
                }
            }
        }

        XYChart.Series<String, Number> series1 = new XYChart.Series<>();
        XYChart.Series<String, Number> series2 = new XYChart.Series<>();
        int countSteps = curColorizeType == ColorizeType.NRHO_DIF ? dulovData.size() : 100;
        int stepSize = curColorizeType == ColorizeType.NRHO_DIF ? 10 : 1;
        ArrayList<Integer> dulovCells = new ArrayList<>(dulovData.keySet().stream().toList());
        Collections.sort(dulovCells);
        for (int i = 0; i < countSteps; i += stepSize) {
            Float dulovValue = getDulovData(dulovCells.get(i));
            Float originalValue = getCellValue(frame, dulovCells.get(i));
            System.out.printf("id = %d; xLo = %.4f; yLo = %.4f%n", dulovCells.get(i),
                    FrameGenerator.gridSchema.get(dulovCells.get(i)).xLo,
                    FrameGenerator.gridSchema.get(dulovCells.get(i)).yLo);
            series1.getData().add(new Data<>(String.valueOf(dulovData.get(dulovCells.get(i))), Objects.requireNonNullElse(dulovValue, 0)));
            series2.getData().add(new XYChart.Data<>(String.valueOf(dulovData.get(dulovCells.get(i))), originalValue));
        }
        this.getData().add(series1);
        this.getData().add(series2);

        if (!dulovData.isEmpty()) {
            if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
                this.setTitle(String.format("График среза по давлению%n для y = %.4f см", dulovYLine));
            } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
                this.setTitle(String.format("График среза по полному давлению%n для y = %.4f см", dulovYLine));
            } else if (curColorizeType == ColorizeType.NRHO_DIF) {
                this.setTitle(String.format("График среза по концентрации%n для х = %.4f см", dulovXLine));
            }
        } else {
            this.setTitle("График невозможно построить из-за отсутствия данных");
        }
    }

    public void showTargetData(FrameGenerator.Frame frame) {
        int targetPoints = 0;
        int totalPoints = 0;
        float targetDiameter = 0f;
        BigDecimal outTotalPoints = new BigDecimal(0);
        BigDecimal outTargetPoints = new BigDecimal(0);

        if (this.getData().isEmpty()) {
            if (frame.timeframe.getTarget() != null) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();

                int maxY = 0;
                Calculation.Diameter diameter = Calculation.calculateTargetDiameter(frame.timeframe, 0.5f);

                for (int i = 0; i < frame.timeframe.getTarget().length; i++) {
                    series.getData().add(new XYChart.Data<>(String.valueOf(i * 8 / 10), frame.timeframe.getTarget()[i]));
                    maxY = Math.max(maxY, frame.timeframe.getTarget()[i]);
                }
                this.getData().add(series);

                XYChart.Series<String, Number> series2 = new XYChart.Series<>();
                series2.getData().add(new XYChart.Data<>(String.valueOf(diameter.leftBorder / 10), 0));
                series2.getData().add(new XYChart.Data<>(String.valueOf(diameter.leftBorder / 10), maxY));
                this.getData().add(series2);

                XYChart.Series<String, Number> series3 = new XYChart.Series<>();
                series3.getData().add(new XYChart.Data<>(String.valueOf(diameter.rightBorder / 10), 0));
                series3.getData().add(new XYChart.Data<>(String.valueOf(diameter.rightBorder / 10), maxY));
                this.getData().add(series3);

                targetDiameter = (float) diameter.diameter / 10;
            }
        } else {
            if (frame.timeframe != null && frame.timeframe.getTarget() != null) {
                int maxY = 0;
                Calculation.Diameter diameter = Calculation.calculateTargetDiameter(frame.timeframe, 0.5f);

                for (int i = 0; i < frame.timeframe.getTarget().length; i++) {
                    XYChart.Data<String, Number> element = this.getData().getFirst().getData().get(i);
                    element.setYValue(frame.timeframe.getTarget()[i]);
                    maxY = Math.max(maxY, frame.timeframe.getTarget()[i]);
                    targetPoints += frame.timeframe.getTarget()[i];
                }

                this.getData().get(1).getData().getFirst().setXValue(String.valueOf(diameter.leftBorder / 10));
                this.getData().get(1).getData().getFirst().setYValue(0);
                this.getData().get(1).getData().get(1).setXValue(String.valueOf(diameter.leftBorder / 10));
                this.getData().get(1).getData().get(1).setYValue(maxY);
                this.getData().get(2).getData().getFirst().setXValue(String.valueOf(diameter.rightBorder / 10));
                this.getData().get(2).getData().getFirst().setYValue(0);
                this.getData().get(2).getData().get(1).setXValue(String.valueOf(diameter.rightBorder / 10));
                this.getData().get(2).getData().get(1).setYValue(maxY);

                targetDiameter = (float) diameter.diameter / 10;
            } else {
                this.getData().get(0).getData().forEach(element -> element.setYValue(0));
                this.getData().get(1).getData().forEach(element -> element.setYValue(0));
                this.getData().get(2).getData().forEach(element -> element.setYValue(0));
            }
        }

        if (frame.timeframe != null) {
            totalPoints = frame.timeframe.getCountPoints();
            outTotalPoints = new BigDecimal(Config.globalParams.get("fnum")).multiply(BigDecimal.valueOf(totalPoints));
            outTargetPoints = new BigDecimal(Config.globalParams.get("fnum")).multiply(BigDecimal.valueOf(targetPoints));
        }

        this.setTitle(String.format("Общее число частиц:%n" +
                        "%.2e (%d)%n" +
                        "Плотность частиц на мишени:%n" +
                        "%.2e (%d)%n" +
                        "Диаметр: %.1f мм",
                outTotalPoints, totalPoints, outTargetPoints, targetPoints, targetDiameter));
    }
}
