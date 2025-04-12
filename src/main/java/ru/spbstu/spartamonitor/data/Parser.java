package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.Logger;
import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Grid;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Parser {

    private String dumpDir;
    private List<String> dumpFiles;
    private static final String dumpFilePattern = "(dump.)([0-9]+)(.txt)";
    private static final String gridFilePattern = "(.*grid.)([0-9]+)(.txt)";
    private static final String targetFilePattern = "(.*target_sum.)([0-9]+)(.txt)";

    public Parser() {
    }

    public void setDumpDir(String dumpDir) throws IOException {
        this.dumpDir = dumpDir;
        this.storeAllDumpFiles();
    }

    private void storeAllDumpFiles() throws IOException {
        this.dumpFiles = Arrays.stream(Objects.requireNonNull(new File(new File(this.dumpDir).getCanonicalPath()).listFiles()))
                .map(File::getName)
                .toList();
    }

    private Map<Integer, String> getTimeFrames(String filePattern) {
        return this.dumpFiles.stream()
                .filter(x -> x.matches(filePattern))
                .collect(Collectors.toMap(x -> Integer.parseInt(x.replaceAll(filePattern, "$2")), x -> x));
    }

    private Map<Integer, String[]> getAllTimeFrames() {
        Map<Integer, String> dumpFrames = this.getTimeFrames(dumpFilePattern);
        Map<Integer, String> gridFrames = this.getTimeFrames(gridFilePattern);
        Map<Integer, String> targetFrames = this.getTimeFrames(targetFilePattern);

        HashMap<Integer, String[]> allFrames = new HashMap<>();
        for (Integer key : dumpFrames.keySet()) {
            allFrames.put(key, new String[]{dumpFrames.get(key), null, null});
            if (gridFrames.containsKey(key)) {
                allFrames.get(key)[1] = gridFrames.get(key);
            }
            if (targetFrames.containsKey(key)) {
                allFrames.get(key)[2] = targetFrames.get(key);
            }
        }

        return allFrames;
    }

    public List<Timeframe> parsAllDumps() throws IOException {
        Map<Integer, String[]> allFrames = this.getAllTimeFrames();
        ArrayList<Timeframe> timeFrames = new ArrayList<>(allFrames.keySet().size());

        List<Integer> sortedKeys = allFrames.keySet().stream().sorted().toList();
        sortedKeys = sortedKeys.subList(0, 20);
        for (Integer frame : sortedKeys) {
            Logger.startTimer(String.format("Parse frame %s", frame));
            timeFrames.add(this.parseTimeFrame(allFrames.get(frame)));
            //Logger.releaseTimer(String.format("Parse frame %s", frame));
        }

        return timeFrames;
    }

    private Timeframe parseTimeFrame(String[] files) throws IOException {
        Timeframe timeframe = new Timeframe();

        if (files[0] != null) {
            timeframe.setPoints(this.parsePoints(files[0]));
        }

        if (files[1] != null) {
            timeframe.setGrid(this.parseGrid(files[1]));
        }

        if (files[2] != null) {
            timeframe.setTarget(this.parseTarget(files[2]));
        }

        return timeframe;
    }

    private Number[][] parsePoints(String fileName) throws IOException {
        List<String> fileLines = Files.readAllLines(Path.of(this.dumpDir, fileName));
        Number[][] points = new Number[fileLines.size() - 9][4];

        List<String> headers = Arrays.stream(fileLines.get(8).replace("ITEM: ATOMS ", "").strip().split(" ")).toList();
        int xIndex = headers.indexOf("x");
        int yIndex = headers.indexOf("y");
        int idIndex = headers.indexOf("id");
        int cellIdIndex = headers.indexOf("cellID");

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            points[i - 9] = new Number[]{
                    Integer.parseInt(params[idIndex]),
                    Float.parseFloat(params[xIndex]),
                    Float.parseFloat(params[yIndex]),
                    Integer.parseInt(params[cellIdIndex])
            };
        }

        fileLines.clear();

        return points;
    }

    private Grid parseGrid(String fileName) throws IOException {
        Grid grid = new Grid();

        List<String> fileLines = Files.readAllLines(Path.of(this.dumpDir, fileName));

        List<String> headers = Arrays.stream(fileLines.get(8).replace("ITEM: CELLS ", "").strip().split(" ")).toList();
        int idIndex = headers.indexOf("id");
        int pIndex = headers.indexOf("c_gTemp[1]");
        int tIndex = headers.indexOf("c_gTemp[2]");

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            grid.addCell(
                    Integer.parseInt(params[idIndex]),
                    new Float[]{
                            Float.parseFloat(params[pIndex]),
                            Float.parseFloat(params[tIndex])
                    }
            );
        }

        fileLines.clear();

        return grid;
    }

    private Integer[] parseTarget(String fileName) throws IOException {
        List<String> fileLines = Files.readAllLines(Path.of(this.dumpDir, fileName));
        Integer[] bars = new Integer[fileLines.size() - 9];

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            bars[i - 9] = Integer.parseInt(params[1]);
        }

        fileLines.clear();

        return bars;
    }

    public void parsInFile(Path filePath) throws IOException {
        List<String> fileLines = Files.readAllLines(filePath);

        for (String line : fileLines) {
            line = line.strip();
            line = line.replaceAll("[\t]", "");
            line = line.replaceAll(" +", " ");
            String[] params = line.strip().split(" ");
            if (params.length == 0) {
                continue;
            }
            if (params[0].startsWith("#")) {
                continue;
            }
            switch (params[0].strip()) {
                case "global" -> {
                    for (int i = 1; i < params.length; i += 2) {
                        Config.globalParams.put(params[i].strip(), params[i + 1].strip());
                    }
                }
                case "timestep" -> Config.tStep = Float.parseFloat(params[1].strip());
                case "create_box" -> {
                    Config.shapeX = (int) (Float.parseFloat(params[2].strip()) - Float.parseFloat(params[1].strip()));
                    Config.shapeY = (int) (Float.parseFloat(params[4].strip()) - Float.parseFloat(params[3].strip()));
                }
                case "create_grid" -> {
                    int boxX = Integer.parseInt(params[1].strip());
                    int boxY = Integer.parseInt(params[2].strip());
                    float coeffX = (float) Config.maxBoxX / ((float) boxX);
                    float coeffY = (float) Config.maxBoxY / ((float) boxY);
                    if (coeffX < coeffY) {
                        Config.coeffXY = coeffX;
                        Config.defaultBoxY = (int) (Config.maxBoxY * Config.coeffXY);
                        Config.shiftBoxY = (Config.maxBoxY - Config.defaultBoxY) / 2;
                    } else {
                        Config.coeffXY = coeffY;
                        Config.defaultBoxX = (int) (Config.maxBoxX * Config.coeffXY);
                        Config.shiftBoxX = (Config.maxBoxX - Config.defaultBoxX) / 2;
                    }
                    Config.multiplayer = (int) (Config.defaultMultiplayer * Config.coeffXY);
                    Config.mainBoxX = Config.defaultBoxX;
                    Config.mainBoxY = Config.defaultBoxY;
                }
                case "read_surf" -> Config.surfFiles.add(params[1].strip());
            }
        }
    }

    public ArrayList<Polygon> parsSurfFile(Path fileName) throws IOException {
        ArrayList<Point> points = new ArrayList<>();
        ArrayList<Polygon> polygons = new ArrayList<>();
        boolean addNewPolygon = true;

        List<String> fileLines = Files.readAllLines(fileName);
        int countPoints = Integer.parseInt(fileLines.get(2).replaceAll("\\D", ""));
        int countLines = Integer.parseInt(fileLines.get(3).replaceAll("\\D", ""));

        for (int i = 7; i < 7 + countPoints; i++) {
            String[] pointElements = fileLines.get(i).split(" ");
            points.add(new Point(Float.parseFloat(pointElements[1]), Float.parseFloat(pointElements[2])));
        }


        for (int i = 7 + countPoints + 3; i < 7 + 3 + countPoints + countLines; i++) {
            String[] linesElements = fileLines.get(i).split(" ");
            int pointFrom = Integer.parseInt(linesElements[1]);
            int pointTo = Integer.parseInt(linesElements[2]);

            if (addNewPolygon) {
                polygons.add(new Polygon());
                polygons.getLast().addPoint(points.get(pointFrom - 1));
            }

            addNewPolygon = pointFrom > pointTo;

            if (!addNewPolygon) {
                polygons.getLast().addPoint(points.get(pointTo - 1));
            }
        }

        return polygons;
    }
}
