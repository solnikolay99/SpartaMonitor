package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;
import ru.spbstu.spartamonitor.logger.Logger;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;

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
    public static HashMap<Integer, Parser.GridCell> gridSchema = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Parser.GridCell>> inSurfSchema = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, Integer>> gridSchemaRevert = new HashMap<>();
    public static HashMap<Integer, Float> dulovsPressureData = new HashMap<>();
    public static HashMap<Integer, Float> dulovsNConcentrationData = new HashMap<>();

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
//                    this.parser.parsDumps(this.timeframes, 90, 101);
                    this.parser.parsDumps(this.timeframes, 90, 92);
//                    this.parser.parsDumps(this.timeframes, 0, 1);
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
        this.parser.parsInFile(this.parser.getInFile());
        loadGridSchema(filePath);
        loadSurfs(filePath);
        excludeOutSurfGridCells();
        revertGridSchema();
        loadDulovsData(filePath);
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

    protected void revertGridSchema() {
        gridSchemaRevert = this.parser.revertGridSchema(gridSchema);
    }

    protected void loadDulovsData(Path rootDir) throws IOException {
        Path xFileName = Path.of(rootDir.toString(), "dulov/xx_Dulov_check.txt");
        Path yFileName = Path.of(rootDir.toString(), "dulov/yy_Dulov_check.txt");
        dulovsPressureData = this.parser.parseDulovsData(Path.of(rootDir.toString(), "dulov/Dulov_density_check.txt"),
                xFileName, yFileName, gridSchemaRevert);
        dulovsNConcentrationData = this.parser.parseDulovsData(Path.of(rootDir.toString(), "dulov/Dulov_n_check.txt"),
                xFileName, yFileName, gridSchemaRevert);
    }

    protected void excludeOutSurfGridCells() {
        List<float[]> surfBorders = new ArrayList<>();
        List<java.awt.Polygon> excludedAreas = new ArrayList<>();
        for (List<Polygon> surf : surfs.values()) {
            float[] borders = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
            for (Polygon polygon : surf) {
                float[] polygonBorders = polygon.getBorderPoints();

                if (polygonBorders[0] < borders[0]) {
                    borders[0] = polygonBorders[0];
                }
                if (polygonBorders[1] < borders[1]) {
                    borders[1] = polygonBorders[1];
                }
                if (polygonBorders[2] > borders[2]) {
                    borders[2] = polygonBorders[2];
                }
                if (polygonBorders[3] > borders[3]) {
                    borders[3] = polygonBorders[3];
                }

                java.awt.Polygon excludedArea = new java.awt.Polygon();
                for (ru.spbstu.spartamonitor.data.models.Point point : polygon.getPoints()) {
                    excludedArea.addPoint((int) (point.x * 1000), (int) (point.y * 1000));
                }
                excludedAreas.add(excludedArea);
            }
            surfBorders.add(borders);
        }

        for (Integer cellId : gridSchema.keySet()) {
            Parser.GridCell gridCell = gridSchema.get(cellId);
            for (float[] borders : surfBorders) {
                if (gridCell.xLo >= borders[0] && gridCell.xLo <= borders[2]
                        && gridCell.yLo >= borders[1] && gridCell.yLo <= borders[3]
                        && gridCell.xHi >= borders[0] && gridCell.xHi <= borders[2]
                        && gridCell.yHi >= borders[1] && gridCell.yHi <= borders[3]) {

                    Rectangle gridPolygon = new Rectangle((int) (gridCell.xLo * 1000),
                            (int) (gridCell.yLo * 1000),
                            (int) ((gridCell.xHi - gridCell.xLo) * 1000),
                            (int) ((gridCell.yHi - gridCell.yLo) * 1000));

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

                    if (!inSurfSchema.containsKey((int) (gridCell.xLo * 1000))) {
                        inSurfSchema.put((int) (gridCell.xLo * 1000), new HashMap<>());
                    }
                    inSurfSchema.get((int) (gridCell.xLo * 1000)).put(cellId, gridSchema.get(cellId));
                }
            }
        }
    }

    public void saveDulovsData(Path rootDir) {
        Path dumpDataFile = Path.of(rootDir.toString(), "Dulovs_data.txt");

        System.out.println("Start saving Dulovs dump");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dumpDataFile.toAbsolutePath().toString()))) {
            String header = "ITEM: CELLS id proc f_aveGridTemp[1] f_aveGridTemp[2] f_aveGridU[1] f_aveGridU[3] f_aveGridU[4]";
            for (int i = 0; i < 8; i++) {
                writer.write("");
                writer.newLine();
            }
            writer.write(header);
            writer.newLine();
            for (Integer key : dulovsPressureData.keySet()) {
                writer.write(String.format(Locale.ENGLISH, "%d 0 %.1f 0 0 0 0", key, dulovsPressureData.get(key)));
                writer.newLine();
            }
        } catch (Exception ignore) {
        }

        System.out.printf("Dulovs dump saved to %s%n", dumpDataFile.toAbsolutePath());
    }
}
