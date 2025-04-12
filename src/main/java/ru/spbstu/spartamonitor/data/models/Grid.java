package ru.spbstu.spartamonitor.data.models;

import java.util.HashMap;
import java.util.Map;

public class Grid {

    private Map<Integer, Float[]> cells = new HashMap<>();

    public Map<Integer, Float[]> getCells() {
        return cells;
    }

    public void setCells(Map<Integer, Float[]> cells) {
        this.cells = cells;
    }

    public void addCell(Integer key, Float[] value) {
        this.cells.put(key, value);
    }
}
