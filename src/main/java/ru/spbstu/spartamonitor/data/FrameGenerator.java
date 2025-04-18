package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;
import ru.spbstu.spartamonitor.logger.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class FrameGenerator implements Runnable {

    public Boolean isRunning = Boolean.FALSE;
    public Boolean showOneIteration = Boolean.FALSE;

    private final Object lock = new Object();

    private final Parser parser = new Parser();
    public volatile boolean isAlive = true;
    private volatile boolean flgPreload = false;

    public List<Timeframe> timeframes = Collections.synchronizedList(new ArrayList<>());
    private int curFrame = 0;
    public HashMap<String, List<Polygon>> surfs = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Integer>> gridSchema = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Integer>> inSurfSchema = new HashMap<>();

    public FrameGenerator() {
    }

    public void setDumpDir(String dumpDir) throws IOException {
        this.parser.setDumpDir(dumpDir);
    }

    @Override
    public void run() {
        while (isAlive) {
            if (flgPreload) {
                Logger.startTimer("Get all timeframe data");
                try {
                    this.parser.getAllTimeFrames();
                    this.parser.parsDumps(this.timeframes, 0, 20);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                Logger.releaseTimer("Get all timeframe data");

                flgPreload = false;
            } else {
                try {
                    Thread.sleep(100);
                } catch (Exception ignore) {
                }
            }
        }
    }

    public void startOneIteration() {
        this.isRunning = Boolean.FALSE;
        this.showOneIteration = Boolean.TRUE;
    }

    public void showOneIteration() {
        this.showOneIteration = Boolean.TRUE;
    }

    public void startIterations() {
        this.isRunning = Boolean.TRUE;
    }

    public void stopIteration() {
        this.isRunning = Boolean.FALSE;
    }

    public void preloadTimeFrames() {
        flgPreload = true;
    }

    public static class Frame {
        public Timeframe timeframe;
        public int frameNumber = 0;

        public Frame() {
            this.timeframe = new Timeframe();
        }
    }

    public Frame getFrame(int countSteps) {
        Frame frame = new Frame();
        this.curFrame += countSteps;
        if (this.curFrame >= this.timeframes.size()) {
            this.curFrame = 0;
        } else if (this.curFrame < 0) {
            this.curFrame = this.timeframes.size() - 1;
        }
        if (this.isRunning || this.showOneIteration) {
            synchronized (lock) {
                frame.timeframe = this.timeframes.get(this.curFrame);
            }
        }
        frame.frameNumber = this.curFrame;
        return frame;
    }

    public HashMap<String, List<Polygon>> getSurfs() {
        return surfs;
    }

    public void loadInFile(Path filePath) throws IOException {
        curFrame = 0;
        this.parser.parsInFile(filePath);
        loadGridSchema(filePath.getParent());
        loadSurfs(filePath.getParent());
        excludeOutSurfGridCells();
    }

    protected void loadSurfs(Path rootDir) throws IOException {
        surfs = new HashMap<>();
        for (String filePath : Config.surfFiles) {
            ArrayList<Polygon> polygons = this.parser.parsSurfFile(Path.of(rootDir.toString(), filePath));
            if (!polygons.isEmpty()) {
                surfs.put(filePath, polygons);
            }
        }
    }

    protected void loadGridSchema(Path rootDir) throws IOException {
        gridSchema = this.parser.parsGridSchema(Path.of(rootDir.toString(), "cells.txt"));
    }

    protected void excludeOutSurfGridCells() {
        List<int[]> surfBorders = new ArrayList<>();
        for (List<Polygon> surf: surfs.values()) {
            int[] borders = new int[] {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (Polygon polygon : surf) {
                float[] polygonBorders = polygon.getBorderPoints();

                if (polygonBorders[0] * 1000 < borders[0]) {
                    borders[0] = (int) (polygonBorders[0] * 1000);
                }
                if (polygonBorders[1] * 1000 < borders[1]) {
                    borders[1] = (int) (polygonBorders[1] * 1000);
                }
                if (polygonBorders[2] * 1000 > borders[2]) {
                    borders[2] = (int) (polygonBorders[2] * 1000);
                }
                if (polygonBorders[3] * 1000 > borders[3]) {
                    borders[3] = (int) (polygonBorders[3] * 1000);
                }
            }
            surfBorders.add(borders);
        }

        for (Integer yLo : gridSchema.keySet()) {
            boolean flgYOutside = true;
            for (int[] borders : surfBorders) {
                if (yLo >= borders[1] && yLo <= borders[3]) {
                    flgYOutside = false;
                    break;
                }
            }
            if (flgYOutside) {
                continue;
            }

            for (Integer xLo : gridSchema.get(yLo).keySet()) {
                for (int[] borders : surfBorders) {
                    if (xLo >= borders[0] && xLo <= borders[2]) {
                        if (!inSurfSchema.containsKey(xLo)) {
                            inSurfSchema.put(xLo, new HashMap<>());
                        }
                        inSurfSchema.get(xLo).put(yLo, gridSchema.get(yLo).get(xLo));
                    }
                }
            }
        }
    }
}
