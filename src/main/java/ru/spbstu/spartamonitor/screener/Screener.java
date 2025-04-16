package ru.spbstu.spartamonitor.screener;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.chart.Chart;
import javafx.scene.image.WritableImage;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;

public class Screener {

    public static BufferedImage combineFullScene(RenderedImage mainImage,
                                                 RenderedImage graduationImage,
                                                 RenderedImage chartImage) {
        int xOffset = 20;
        int yOffset = 20;
        int width = Math.max(mainImage.getWidth(), mainImage.getWidth()) + 3 * xOffset + chartImage.getWidth();
        int height = Math.max(mainImage.getHeight() + graduationImage.getHeight(), chartImage.getHeight()) + 3 * yOffset;

        BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gc = outImage.createGraphics();
        java.awt.Color oldColor = gc.getColor();
        gc.setPaint(java.awt.Color.LIGHT_GRAY);
        gc.fillRect(0, 0, width, height);
        gc.setColor(oldColor);
        gc.drawImage((BufferedImage) mainImage, null, xOffset, yOffset);
        gc.drawImage((BufferedImage) graduationImage, null, xOffset, mainImage.getHeight() + 2 * yOffset);
        gc.drawImage((BufferedImage) chartImage, null, width - chartImage.getWidth() - xOffset,
                (height - chartImage.getHeight() - (int) (1.5 * yOffset)) / 2);
        gc.dispose();

        return outImage;
    }

    public static RenderedImage getImageFromCanvas(Canvas canvas) {
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        canvas.snapshot(null, writableImage);
        return SwingFXUtils.fromFXImage(writableImage, null);
    }

    public static RenderedImage getImageFromCanvas(Chart chart) {
        WritableImage writableImage = new WritableImage((int) chart.getWidth(), (int) chart.getHeight());
        chart.snapshot(null, writableImage);
        return SwingFXUtils.fromFXImage(writableImage, null);
    }
}
