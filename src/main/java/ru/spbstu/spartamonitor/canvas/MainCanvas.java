package ru.spbstu.spartamonitor.canvas;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;

import java.util.ArrayList;
import java.util.List;

import static ru.spbstu.spartamonitor.colorize.ColorSchema.colorSchema;
import static ru.spbstu.spartamonitor.config.Config.*;

public class MainCanvas extends Canvas {

    private float zoom = 1f;

    public void drawIteration(FrameGenerator frameGenerator,
                              FrameGenerator.Frame frame,
                              ColorizeType colorizeType) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setImageSmoothing(true);

        drawMask(frameGenerator);

        colorizePoints(gc, frame, colorizeType);
    }

    protected void drawAxes() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setFill(Color.GRAY);

        for (int i = 0; i <= shapeX; i++) {
            gc.fillRect(i * multiplayer - 1, shiftBoxY + mainBoxY - 6, 1, 6);
        }
        for (int i = 0; i <= shapeX; i += 5) {
            gc.fillRect(i * multiplayer - 1, shiftBoxY + mainBoxY - 10, 1, 10);
        }
    }

    public void drawMask(FrameGenerator frameGenerator) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.clearRect(0, 0, this.getWidth(), this.getHeight());
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
            gc.fillOval(shiftBoxX + point[1].floatValue() * multiplayer, shiftBoxY + point[2].floatValue() * multiplayer, 1, 1);
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void changeZoom(float delta) {
        this.zoom += delta;
        if (this.zoom < 1) {
            this.zoom = 1;
        } else if (this.zoom > 100) {
            this.zoom = 100;
        }
    }
}
