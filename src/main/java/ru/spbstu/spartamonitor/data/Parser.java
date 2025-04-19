package ru.spbstu.spartamonitor.data;

import ru.spbstu.spartamonitor.config.Config;
import ru.spbstu.spartamonitor.data.models.Grid;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.data.models.Timeframe;
import ru.spbstu.spartamonitor.eventbus.EventBusFactory;
import ru.spbstu.spartamonitor.events.ParserEvent;
import ru.spbstu.spartamonitor.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Parser {

    private String dumpDir;
    private List<String> dumpFiles;
    private static final String dumpFilePattern = "(dump.)([0-9]+)(.txt)";
    private static final String gridFilePattern = "(.*grid.)([0-9]+)(.txt)";
    private static final String targetFilePattern = "(.*target_sum.)([0-9]+)(.txt)";
    private final Map<Integer, String[]> allFrames = new HashMap<>();

    /**
     * Constants
     */
    float gamma = 5f / 3f; // показатель адиабаты
    float R = 2077f; // универсальная газовая постоянная для He (в Дж / (кг * К))

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

    public void getAllTimeFrames() {
        Map<Integer, String> dumpFrames = this.getTimeFrames(dumpFilePattern);
        Map<Integer, String> gridFrames = this.getTimeFrames(gridFilePattern);
        Map<Integer, String> targetFrames = this.getTimeFrames(targetFilePattern);

        for (Integer key : dumpFrames.keySet()) {
            allFrames.put(key, new String[]{dumpFrames.get(key), null, null});
            if (gridFrames.containsKey(key)) {
                allFrames.get(key)[1] = gridFrames.get(key);
            }
            if (targetFrames.containsKey(key)) {
                allFrames.get(key)[2] = targetFrames.get(key);
            }
        }
    }

    public void parsDumps(List<Timeframe> timeFrames, int startFrame, int endFrame) throws IOException {
        IntStream.range(0, allFrames.size()).<Timeframe>mapToObj(i -> null).forEach(timeFrames::add);
        List<Integer> sortedKeys = allFrames.keySet().stream().sorted().toList();
        for (int i = startFrame; i < endFrame; i++) {
            Logger.startTimer("Pars data to timeframe");
            Timeframe timeframe = this.parseTimeFrame(allFrames.get(sortedKeys.get(i)));
            Logger.releaseTimer("Pars data to timeframe");
            timeFrames.set(i, timeframe);
            EventBusFactory.getEventBus().post(new ParserEvent((endFrame - startFrame), (startFrame + i + 1)));
        }
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
        int vIndex = headers.indexOf("c_gridP[1]");

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            float temperature = Float.parseFloat(params[tIndex]);
            float cs = (float) Math.sqrt(gamma * R * temperature);
            float u = Math.abs(Float.parseFloat(params[vIndex]) / 100);
            grid.addCell(
                    Integer.parseInt(params[idIndex]),
                    new Float[]{
                            Float.parseFloat(params[pIndex]),   // density in grid
                            temperature,                        // temperature in grid
                            u,                                  // directed velocity by x in grid
                            cs,                                 // sound velocity
                            u / cs                              // Mach value
                    }
            );
        }

        fileLines.clear();

        return grid;
    }

    private Integer[] parseTarget(String fileName) throws IOException {
        List<String> fileLines = Files.readAllLines(Path.of(this.dumpDir, fileName));
        Integer[] bars = new Integer[fileLines.size()];

        for (int i = 0; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            bars[i] = Integer.parseInt(params[1]);
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
                    Config.shapeX = Float.parseFloat(params[2].strip()) - Float.parseFloat(params[1].strip());
                    Config.shapeY = Float.parseFloat(params[4].strip()) - Float.parseFloat(params[3].strip());
                }
                case "create_grid" -> {
                    Config.spartaCellSize = Config.shapeX / Integer.parseInt(params[1].strip());
                    Config.monitorCellSize = Config.shapeX / Config.maxBoxX;
                    float coeffX = (float) Config.maxBoxX / Config.shapeX;
                    float coeffY = (float) Config.maxBoxY / Config.shapeY;
                    Config.defaultMultiplayer = (int) Math.min(coeffX, coeffY);
                    if (coeffX < coeffY) {
                        Config.defaultBoxY = (int) (Config.shapeY * Config.defaultMultiplayer);
                        Config.shiftBoxY = (Config.maxBoxY - Config.defaultBoxY) / 2;
                    } else {
                        Config.defaultBoxX = (int) (Config.shapeX * Config.defaultMultiplayer);
                        Config.shiftBoxX = (Config.maxBoxX - Config.defaultBoxX) / 2;
                    }
                    Config.multiplayer = Config.defaultMultiplayer;
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

    public static class GridCell {
        public int xLo;
        public int yLo;
        public int xHi;
        public int yHi;
        public int cellId;

        public GridCell(int cellId, int xLo, int yLo, int xHi, int yHi) {
            this.cellId = cellId;
            this.xLo = xLo;
            this.yLo = yLo;
            this.xHi = xHi;
            this.yHi = yHi;
        }
    }

    /**
     * Pars grid schema from cells.txt file to Map<int, Map<int, int>>.
     * key of root Map - xlo coordinate of cell * 1000
     * key of child Map - ylo coordinate of cell * 1000
     * value of child Map - id of cell
     *
     * @param fileName - full path to file
     * @return - Map with keys 'y : x - id'
     */
    public HashMap<Integer, HashMap<Integer, GridCell>> parsGridSchema(Path fileName) throws IOException {
        HashMap<Integer, HashMap<Integer, GridCell>> gridSchema = new HashMap<>();

        List<String> fileLines = Files.readAllLines(fileName);

        List<String> headers = Arrays.stream(fileLines.get(8).replace("ITEM: CELLS ", "").strip().split(" ")).toList();
        int idIndex = headers.indexOf("id");
        int xLoIndex = headers.indexOf("xlo");
        int yLoIndex = headers.indexOf("ylo");
        int xHiIndex = headers.indexOf("xhi");
        int yHiIndex = headers.indexOf("yhi");

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            //if (Float.parseFloat(params[distSurfIndex]) > 0.001f) {
            int xLo = (int) (Float.parseFloat(params[xLoIndex]) * 1000);
            int yLo = (int) (Float.parseFloat(params[yLoIndex]) * 1000);
            int xHi = (int) (Float.parseFloat(params[xHiIndex]) * 1000);
            int yHi = (int) (Float.parseFloat(params[yHiIndex]) * 1000);
            if (!gridSchema.containsKey(xLo)) {
                gridSchema.put(xLo, new HashMap<>());
            }
            gridSchema.get(xLo).put(yLo,
                    new GridCell(
                            Integer.parseInt(params[idIndex]),
                            xLo,
                            yLo,
                            xHi,
                            yHi
                    ));
            //}
        }

        return gridSchema;
    }
}
