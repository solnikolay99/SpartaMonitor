package ru.spbstu.spartamonitor;

import ru.spbstu.spartamonitor.data.Parser;
import ru.spbstu.spartamonitor.data.models.Timeframe;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Polygon;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FrameGenerator implements Runnable {

    public Boolean isRunning = Boolean.FALSE;
    public Boolean showOneIteration = Boolean.FALSE;

    private final Object lock = new Object();

    private final Parser parser = new Parser();

    List<Timeframe> timeframes = new ArrayList<>();
    private int curFrame = 0;
    private ArrayList<Polygon> listSurfs;

    public FrameGenerator() {
    }

    @Override
    public void run() {
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

    public void preloadTimeFrames(String dumpDir) throws IOException {
        Logger.startTimer("Get all timeframe data");

        this.parser.setDumpDir(dumpDir);
        this.timeframes = this.parser.parsAllDumps();

        Logger.releaseTimer("Get all timeframe data");
    }

    public void setPrevFrame() {
        this.curFrame--;
    }

    public static class NextFrame {
        public Timeframe timeframe;
        public ArrayList<Integer> listOutPoints;
        public ArrayList<Integer> listPumpingPoints1;
        public ArrayList<Integer> listPumpingPoints2;
        public int frameNumber = 0;

        public NextFrame() {
            this.timeframe = new Timeframe();
            this.listOutPoints = new ArrayList<>();
            this.listPumpingPoints1 = new ArrayList<>();
            this.listPumpingPoints2 = new ArrayList<>();
        }
    }

    public NextFrame getFrame(int countSteps) {
        NextFrame nextFrame = new NextFrame();
        this.curFrame += countSteps;
        if (this.curFrame >= this.timeframes.size()) {
            this.curFrame = 0;
        } else if (this.curFrame < 0) {
            this.curFrame = this.timeframes.size() - 1;
        }
        if (this.isRunning || this.showOneIteration) {
            synchronized (lock) {
                nextFrame.timeframe = this.timeframes.get(this.curFrame);
            }
        }
        nextFrame.frameNumber = this.curFrame;
        return nextFrame;
    }

    public ArrayList<Polygon> getListSurfs() {
        return listSurfs;
    }

    public void loadInFile(Path filePath) throws IOException {
        curFrame = 0;
        this.parser.parsInFile(filePath);
        loadSurfs(filePath.getParent());
    }

    public void loadSurfs(Path rootDir) throws IOException {
        listSurfs = new ArrayList<>();
        for (String filePath : Config.surfFiles) {
            ArrayList<Polygon> polygons = this.parser.parsSurfFile(Path.of(rootDir.toString(), filePath));
            if (!polygons.isEmpty()) {
                listSurfs.addAll(polygons);
            }
        }
    }
}
