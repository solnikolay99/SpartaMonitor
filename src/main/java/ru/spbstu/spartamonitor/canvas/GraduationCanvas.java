package ru.spbstu.spartamonitor.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.spbstu.spartamonitor.colorize.ColorizeType;

import static ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema;

public class GraduationCanvas extends Canvas {

    public void colorize(ColorizeType graduation) {
        GraphicsContext gc = this.getGraphicsContext2D();
        double colorStep = this.getWidth() / colorSchema.size();
        int countTextSteps = (graduation.maxValue - graduation.minValue) / graduation.stepValue;
        int countSmallTextSteps = (graduation.maxValue - graduation.minValue) / graduation.smallStepValue;
        double textStep = colorSchema.size() * colorStep / countTextSteps;
        double smallTextStep = colorSchema.size() * colorStep / countSmallTextSteps;

        gc.clearRect(0, 0, this.getWidth(), this.getHeight());

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

        gc.fillText(String.format("%s, %s", graduation.label, graduation.units), (countTextSteps * textStep) / 2 - 40, 10);
    }
}
