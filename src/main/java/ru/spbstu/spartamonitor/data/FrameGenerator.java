package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.logger.Logger;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
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
    private List<Polygon> listSurfs;

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
                    this.timeframes = this.parser.parsAllDumps();
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

    public List<Polygon> getListSurfs() {
        return listSurfs;
    }

    public void loadInFile(Path filePath) throws IOException {
        curFrame = 0;
        this.parser.parsInFile(filePath);
        loadSurfs(filePath.getParent());
    }

    public void loadSurfs(Path rootDir) throws IOException {
        listSurfs = Collections.synchronizedList(new ArrayList<>());
        for (String filePath : Config.surfFiles) {
            ArrayList<Polygon> polygons = this.parser.parsSurfFile(Path.of(rootDir.toString(), filePath));
            if (!polygons.isEmpty()) {
                listSurfs.addAll(polygons);
            }
        }
    }
}
