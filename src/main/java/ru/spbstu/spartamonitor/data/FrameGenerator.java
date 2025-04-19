package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;
import ru.spbstu.spartamonitor.logger.Logger;

import java.awt.*;
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
    private static int curFrame = 0;
    public HashMap<String, List<Polygon>> surfs = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Parser.GridCell>> gridSchema = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Parser.GridCell>> inSurfSchema = new HashMap<>();

    private static final FrameGenerator frameGenerator = new FrameGenerator();

    public static FrameGenerator getFrameGenerator() {
        return frameGenerator;
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
                    this.parser.parsDumps(this.timeframes, 100, 120);
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
        curFrame += countSteps;
        if (curFrame >= this.timeframes.size()) {
            curFrame = 0;
        } else if (curFrame < 0) {
            curFrame = this.timeframes.size() - 1;
        }
        if (this.isRunning || this.showOneIteration || countSteps == 0) {
            synchronized (lock) {
                frame.timeframe = this.timeframes.get(curFrame);
            }
        }
        frame.frameNumber = curFrame;
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
        List<java.awt.Polygon> excludedAreas = new ArrayList<>();
        for (List<Polygon> surf : surfs.values()) {
            int[] borders = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
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

                java.awt.Polygon excludedArea = new java.awt.Polygon();
                for (ru.spbstu.spartamonitor.data.models.Point point : polygon.getPoints()) {
                    excludedArea.addPoint((int) (point.x * 1000), (int) (point.y * 1000));
                }
                excludedAreas.add(excludedArea);
            }
            surfBorders.add(borders);
        }

        for (Integer xLo : gridSchema.keySet()) {
            boolean flgXOutside = true;
            for (int[] borders : surfBorders) {
                if (xLo >= borders[0] && xLo <= borders[2]) {
                    flgXOutside = false;
                    break;
                }
            }
            if (flgXOutside) {
                continue;
            }

            for (Integer yLo : gridSchema.get(xLo).keySet()) {
                Parser.GridCell gridCell = gridSchema.get(xLo).get(yLo);
                for (int[] borders : surfBorders) {
                    if (gridCell.xLo >= borders[0] && gridCell.xLo <= borders[2] && gridCell.yLo >= borders[1] && gridCell.yLo <= borders[3]
                            && gridCell.xHi >= borders[0] && gridCell.xHi <= borders[2] && gridCell.yHi >= borders[1] && gridCell.yHi <= borders[3]) {

                        Rectangle gridPolygon = new Rectangle(gridCell.xLo,
                                gridCell.yLo,
                                gridCell.xHi - gridCell.xLo,
                                gridCell.yHi - gridCell.yLo);

                        boolean flgGridPolygonInside = false;
                        for (java.awt.Polygon excludedArea : excludedAreas) {
                            if (excludedArea.contains(gridPolygon)) {
                                flgGridPolygonInside = true;
                                break;
                            }
                        }

                        if (flgGridPolygonInside) {
                            continue;
                        }

                        if (!inSurfSchema.containsKey(xLo)) {
                            inSurfSchema.put(xLo, new HashMap<>());
                        }
                        inSurfSchema.get(xLo).put(yLo, gridSchema.get(xLo).get(yLo));
                    }
                }
            }
        }
    }
}
