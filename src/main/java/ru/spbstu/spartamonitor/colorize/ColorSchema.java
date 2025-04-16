package ru.spbstu.spartamonitor.colorize;

import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ColorSchema {

    public static List<Color> colorSchema = new ArrayList<>();

    static {
        colorSchema.add(Color.RED);
        colorSchema.add(Color.GREENYELLOW);
        colorSchema.add(Color.BLUE);
        colorSchema.add(Color.GOLD);
        colorSchema.addAll(linearGradient(Color.BLUEVIOLET, Color.MEDIUMSPRINGGREEN, 116));
        colorSchema.addAll(linearGradient(Color.MEDIUMSPRINGGREEN, Color.ORANGE, 120));
        colorSchema.addAll(linearGradient(Color.ORANGE, Color.MAGENTA, 120));
    }

    private static List<Color> linearGradient(Color startColor, Color endColor, int countColors) {
        List<Color> outColors = new ArrayList<>();
        outColors.add(startColor);

        for (int i = 1; i < countColors; i++) {
            int[] newColor = new int[3];
            int sRed = (int) (startColor.getRed() * 255);
            int eRed = (int) (endColor.getRed() * 255);
            int sGreen = (int) (startColor.getGreen() * 255);
            int eGreen = (int) (endColor.getGreen() * 255);
            int sBlue = (int) (startColor.getBlue() * 255);
            int eBlue = (int) (endColor.getBlue() * 255);
            newColor[0] = (int) (sRed + ((float) i / (countColors - 1)) * (eRed - sRed));
            newColor[1] = (int) (sGreen + ((float) i / (countColors - 1)) * (eGreen - sGreen));
            newColor[2] = (int) (sBlue + ((float) i / (countColors - 1)) * (eBlue - sBlue));
            outColors.add(Color.rgb(newColor[0], newColor[1], newColor[2]));
        }

        return outColors;
    }
}
