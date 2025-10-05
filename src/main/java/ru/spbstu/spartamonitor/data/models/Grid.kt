package ru.spbstu.spartamonitor.data.models;

import java.util.HashMap;
import java.util.Map;

public class Grid {

    private Map<Integer, float[]> cells = new HashMap<>();
    private Map<Integer, Integer> procs = new HashMap<>();

    public Map<Integer, float[]> getCells() {
        return cells;
    }

    public Map<Integer, Integer> getProcs() {
        return procs;
    }

    public void setCells(Map<Integer, float[]> cells) {
        this.cells = cells;
    }

    public void addCell(Integer key, float[] value) {
        this.cells.put(key, value);
    }

    public void bindProc(Integer key, Integer value) {
        this.procs.put(key, value);
    }
}
