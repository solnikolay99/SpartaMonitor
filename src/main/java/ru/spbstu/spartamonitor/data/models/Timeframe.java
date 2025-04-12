package ru.spbstu.spartamonitor.data.models;

public class Timeframe {

    private Number[][] points;
    private Grid grid = new Grid();
    private Integer[] target;

    public Number[][] getPoints() {
        return points;
    }

    public void setPoints(Number[][] points) {
        this.points = points;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public Integer[] getTarget() {
        return target;
    }

    public void setTarget(Integer[] target) {
        this.target = target;
    }
}
