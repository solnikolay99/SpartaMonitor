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
}
