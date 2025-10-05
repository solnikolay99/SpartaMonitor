package ru.spbstu.spartamonitor.data.models;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class Polygon {
    private final List<Point> points = new ArrayList<>();
    private final List<Line2D> lines = new ArrayList<>();

    public Polygon() {

    }

    public void addPoint(Point point) {
        if (!points.isEmpty()) {
            lines.add(new Line2D.Float(points.getLast().x,
                    points.getLast().y,
                    point.x,
                    point.y));
        }
        points.add(point);
    }

    public Point getPoint(int position) {
        return points.get(position);
    }

    public List<Point> getPoints() {
        return points;
    }

    public List<Line2D> getLines() {
        return lines;
    }

    public float[] getBorderPoints() {
        float[] borderPoints = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        for (Point point : points) {
            if (point.x < borderPoints[0]) {
                borderPoints[0] = point.x;
            } else if (point.x > borderPoints[2]) {
                borderPoints[2] = point.x;
            }

            if (point.y < borderPoints[1]) {
                borderPoints[1] = point.y;
            } else if (point.y > borderPoints[3]) {
                borderPoints[3] = point.y;
            }
        }
        return borderPoints;
    }
}
