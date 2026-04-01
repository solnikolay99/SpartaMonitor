package ru.spbstu.spartamonitor.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.config.Config;

import static ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema;

public class GraduationCanvas extends Canvas {

    public ColorizeType curColorizeType = null;

    public void colorize(ColorizeType graduation) {
        curColorizeType = graduation;

        int countColors = Math.min(graduation.countColors, colorSchema.size());
        GraphicsContext gc = this.getGraphicsContext2D();
//        double colorStep = this.getWidth() / countColors;
        double colorStep = ((float) Config.defaultBoxX) / countColors;
        int countTextSteps = (int) ((graduation.maxValue - graduation.minValue) / graduation.stepValue);
        int countSmallTextSteps = (int) ((graduation.maxValue - graduation.minValue) / graduation.smallStepValue);
        double textStep = countColors * colorStep / countTextSteps;
        double smallTextStep = countColors * colorStep / countSmallTextSteps;

        gc.clearRect(0, 0, this.getWidth(), this.getHeight());

        for (int i = 0; i < countColors; i++) {
            gc.setFill(colorSchema.get(i));
            gc.fillRect(colorStep * i, 40, colorStep, 40);
        }

        gc.setFill(Color.GRAY);
        gc.fillText(getFormatedValue(graduation, 0), 0, 25);
        gc.fillRect(0, 30, 2, 10);
        for (int i = 1; i < countTextSteps; i++) {
            String text = getFormatedValue(graduation, i);
            gc.fillText(text, i * textStep - 7, 25);
            gc.fillRect(i * textStep - 1, 30, 2, 10);
        }
        gc.fillText(getFormatedValue(graduation, countTextSteps), countTextSteps * textStep - 15, 25);
        gc.fillRect(colorStep * countColors - 2, 30, 2, 10);

        for (int i = 1; i < countSmallTextSteps; i++) {
            gc.fillRect(i * smallTextStep, 34, 1, 6);
        }

        gc.fillText(String.format("%s, %s", graduation.label, graduation.units), (countTextSteps * textStep) / 2 - 40, 10);
    }

    private String getFormatedValue(ColorizeType graduation, int step) {
        String text;
        if (graduation.compactValue) {
            int power = 0;
            double maxValue = graduation.maxValue;
            while (maxValue > 1000) {
                power++;
                maxValue /= 1000;
            }
            text = String.format("%.1f", (graduation.minValue + graduation.stepValue * step) / Math.pow(1000, power));
        } else {
            if (graduation.stepValue > 1e5) {
                text = String.valueOf(graduation.minValue + graduation.stepValue * step);
            } else if (graduation.stepValue < 1e-3) {
                text = String.format("%.1e", graduation.minValue + graduation.stepValue * step);
            } else if (graduation.stepValue < 1) {
                if (graduation.stepValue >= 0.1) {
                    text = String.format("%.1f", graduation.minValue + graduation.stepValue * step);
                } else if (graduation.stepValue >= 0.01) {
                    text = String.format("%.2f", graduation.minValue + graduation.stepValue * step);
                } else {
                    text = String.format("%.3f", graduation.minValue + graduation.stepValue * step);
                }
            } else {
                text = String.format("%.0f", graduation.minValue + graduation.stepValue * step);
            }
        }
        return text;
    }
}
