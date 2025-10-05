package ru.spbstu.spartamonitor.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.spbstu.spartamonitor.colorize.ColorSchema;
import ru.spbstu.spartamonitor.colorize.ColorizeType;

public class GraduationCanvas extends Canvas {

    public void colorize(ColorizeType graduation) {
        GraphicsContext gc = this.getGraphicsContext2D();
        double colorStep = this.getWidth() / ColorSchema.INSTANCE.getColorSchema().size();
        int countTextSteps = (int) ((graduation.getMaxValue() - graduation.getMinValue()) / graduation.getStepValue());
        int countSmallTextSteps = (int) ((graduation.getMaxValue() - graduation.getMinValue()) / graduation.getSmallStepValue());
        double textStep = ColorSchema.INSTANCE.getColorSchema().size() * colorStep / countTextSteps;
        double smallTextStep = ColorSchema.INSTANCE.getColorSchema().size() * colorStep / countSmallTextSteps;

        gc.clearRect(0, 0, this.getWidth(), this.getHeight());

        for (int i = 0; i < ColorSchema.INSTANCE.getColorSchema().size(); i++) {
            gc.setFill(ColorSchema.INSTANCE.getColorSchema().get(i));
            gc.fillRect(colorStep * i, 40, colorStep, 40);
        }

        gc.setFill(Color.GRAY);
        gc.fillText(String.valueOf(graduation.getMinValue()), 0, 25);
        gc.fillRect(0, 30, 2, 10);
        for (int i = 1; i < countTextSteps; i++) {
            String text;
            if (graduation.getStepValue() > 1e5) {
                text = String.valueOf(graduation.getMinValue() + graduation.getStepValue() * i);
            } else {
                text = String.format("%.0f", graduation.getMinValue() + graduation.getStepValue() * i);
            }
            gc.fillText(text, i * textStep - 7, 25);
            gc.fillRect(i * textStep - 1, 30, 2, 10);
        }
        gc.fillText(String.valueOf(graduation.getMaxValue()), countTextSteps * textStep - 25, 25);
        gc.fillRect(colorStep * ColorSchema.INSTANCE.getColorSchema().size() - 2, 30, 2, 10);

        for (int i = 1; i < countSmallTextSteps; i++) {
            gc.fillRect(i * smallTextStep, 34, 1, 6);
        }

        gc.fillText(String.format("%s, %s", graduation.getLabel(), graduation.getUnits()), (countTextSteps * textStep) / 2 - 40, 10);
    }
}
