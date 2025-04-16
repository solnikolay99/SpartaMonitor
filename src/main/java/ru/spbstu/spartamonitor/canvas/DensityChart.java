package ru.spbstu.spartamonitor.canvas;

import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import ru.spbstu.spartamonitor.calculate.Calculation;
import ru.spbstu.spartamonitor.data.FrameGenerator;

public class DensityChart extends LineChart<String, Number> {
    public DensityChart(@NamedArg("xAxis") Axis<String> xAxis,
                        @NamedArg("yAxis") Axis<Number> yAxis,
                        @NamedArg("data") ObservableList<Series<String, Number>> data) {
        super(xAxis, yAxis, data);
    }

    public DensityChart(@NamedArg("xAxis") Axis<String> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
        super(xAxis, yAxis);
    }

    public void drawIteration(FrameGenerator.Frame frame) {
        int countPoints = 0;
        float targetDiameter = 0f;

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

                targetDiameter = (float) diameter.diameter / 8;
            }
        } else {
            if (frame.timeframe != null && frame.timeframe.getTarget() != null) {
                int maxY = 0;
                Calculation.Diameter diameter = Calculation.calculateTargetDiameter(frame.timeframe, 0.5f);

                for (int i = 0; i < frame.timeframe.getTarget().length; i++) {
                    XYChart.Data<String, Number> element = this.getData().getFirst().getData().get(i);
                    element.setYValue(frame.timeframe.getTarget()[i]);
                    maxY = Math.max(maxY, frame.timeframe.getTarget()[i]);
                    countPoints += frame.timeframe.getTarget()[i];
                }

                this.getData().get(1).getData().getFirst().setXValue(String.valueOf(diameter.leftBorder / 10));
                this.getData().get(1).getData().getFirst().setYValue(0);
                this.getData().get(1).getData().get(1).setXValue(String.valueOf(diameter.leftBorder / 10));
                this.getData().get(1).getData().get(1).setYValue(maxY);
                this.getData().get(2).getData().getFirst().setXValue(String.valueOf(diameter.rightBorder / 10));
                this.getData().get(2).getData().getFirst().setYValue(0);
                this.getData().get(2).getData().get(1).setXValue(String.valueOf(diameter.rightBorder / 10));
                this.getData().get(2).getData().get(1).setYValue(maxY);

                targetDiameter = (float) diameter.diameter / 8;
            } else {
                this.getData().get(0).getData().forEach(element -> element.setYValue(0));
                this.getData().get(1).getData().forEach(element -> element.setYValue(0));
                this.getData().get(2).getData().forEach(element -> element.setYValue(0));
            }
        }

        this.setTitle(String.format("Плотность частиц: %d%n" + "Диаметр: %.1f мм", countPoints, targetDiameter));
    }
}
