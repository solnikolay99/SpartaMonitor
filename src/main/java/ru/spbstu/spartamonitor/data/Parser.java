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
    private static final String inFilePattern = "(.*)(in)(.*)(.step)";
    private static final String dumpFilePattern = "(dump.)([0-9]+)(.txt)";
    private static final String gridFilePattern = "(.*grid.)([0-9]+)(.txt)";
    private static final String influxFilePattern = "(.*influx_sum.)([0-9]+)(.txt)";
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

    public Path getInFile() throws IOException {
        return Path.of(Arrays.stream(Objects.requireNonNull(new File(new File(this.dumpDir).getCanonicalPath()).listFiles()))
                .map(File::getAbsolutePath)
                .filter(x -> x.matches(inFilePattern))
                .findFirst().orElseThrow());
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
        Map<Integer, String> influxFrames = this.getTimeFrames(influxFilePattern);

        Set<Integer> frames = Config.parsPoints ? dumpFrames.keySet() : gridFrames.keySet();
        for (Integer key : frames) {
            allFrames.put(key, new String[4]);
            if (dumpFrames.containsKey(key)) {
                allFrames.get(key)[0] = dumpFrames.get(key);
            }
            if (gridFrames.containsKey(key)) {
                allFrames.get(key)[1] = gridFrames.get(key);
            }
            if (targetFrames.containsKey(key)) {
                allFrames.get(key)[2] = targetFrames.get(key);
            }
            if (influxFrames.containsKey(key)) {
                allFrames.get(key)[3] = influxFrames.get(key);
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
            EventBusFactory.getEventBus().post(new ParserEvent((endFrame - startFrame), (i + 1)));
        }
    }

    private Timeframe parseTimeFrame(String[] files) throws IOException {
        Timeframe timeframe = new Timeframe();

        if (files[0] != null && Config.parsPoints) {
            timeframe.setPoints(this.parsePoints(files[0]));
        }

        if (files[1] != null) {
            timeframe.setGrid(this.parseGrid(files[1]));
        }

        if (files[2] != null) {
            timeframe.setTarget(this.parseTarget(files[2]));
        }

        if (files[3] != null) {
            timeframe.setCountPoints(this.parseInflux(files[3]));
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
                    Config.unitSystemCGS ? Float.parseFloat(params[xIndex]) : Float.parseFloat(params[xIndex]) * 100,
                    Config.unitSystemCGS ? Float.parseFloat(params[yIndex]) : Float.parseFloat(params[yIndex]) * 100,
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
        int idProc = headers.indexOf("proc");
        int pIndex = Math.max(headers.indexOf("c_gTemp[1]"), headers.indexOf("f_aveGridTemp[1]"));
        int tIndex = Math.max(headers.indexOf("c_gTemp[2]"), headers.indexOf("f_aveGridTemp[2]"));
        int vIndex = Math.max(headers.indexOf("c_gridP[1]"), headers.indexOf("f_aveGridU[1]"));
        int keIndex = Math.max(headers.indexOf("c_gridP[2]"), headers.indexOf("f_aveGridU[2]"));
        int nIndex = Math.max(headers.indexOf("c_gridP[3]"), headers.indexOf("f_aveGridU[3"));
        int nrhoIndex = Math.max(headers.indexOf("c_gridP[4]"), headers.indexOf("f_aveGridU[4]"));

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            float temperature = Float.parseFloat(params[tIndex]);
            float cs = (float) Math.sqrt(gamma * R * temperature);
            float u = Math.abs(Float.parseFloat(params[vIndex]) / (Config.unitSystemCGS ? 100 : 1));
            float nrho = nrhoIndex == -1 ? 0f: Float.parseFloat(params[nrhoIndex]);
            float pDynamic = keIndex == -1 || nrhoIndex == -1 ? 0f
                    : (2f / 3f * Float.parseFloat(params[keIndex]) * nrho) / (Config.unitSystemCGS ? 10 : 1);
            grid.addCell(
                    Integer.parseInt(params[idIndex]),
                    new float[]{
                            Float.parseFloat(params[pIndex]) / (Config.unitSystemCGS ? 10 : 1),   // density in grid (SI - in Pa, CGS - in barye)
                            temperature,                            // temperature in grid
                            u,                                      // directed velocity by x in grid
                            cs,                                     // sound velocity
                            u / cs,                                 // Mach value
                            nIndex != -1 ? Float.parseFloat(params[nIndex]) : 0f,       // Count particles in cell (N count)
                            Config.unitSystemCGS ? nrho : (float) (nrho / 1e6), // Nrho in cell
                            pDynamic,                               // dynamic density in grid (SI - in Pa, CGS - in barye)
                    }
            );
            grid.bindProc(Integer.parseInt(params[idIndex]), idProc == -1 ? 0 : Integer.parseInt(params[idProc]));
        }

        fileLines.clear();

        return grid;
    }

    private int parseInflux(String fileName) throws IOException {
        List<String> fileLines = Files.readAllLines(Path.of(this.dumpDir, fileName));
        int countPoints = 0;

        for (String fileLine : fileLines) {
            String countStr = fileLine.trim().replace("Count points: ", "");
            countPoints = !countStr.isEmpty() ? Integer.parseInt(countStr) : 0;
        }

        fileLines.clear();

        return countPoints;
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
            line = line.replaceAll("\t", "");
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
                case "units" -> Config.unitSystemCGS = params[1].strip().equals("cgs");
                case "timestep" -> Config.tStep = Float.parseFloat(params[1].strip());
                case "create_box" -> {
                    Config.shapeX = Float.parseFloat(params[2].strip()) - Float.parseFloat(params[1].strip());
                    Config.shapeX = Config.unitSystemCGS ? Config.shapeX : Config.shapeX * 100;
                    Config.shapeY = Float.parseFloat(params[4].strip()) - Float.parseFloat(params[3].strip());
                    Config.shapeY = Config.unitSystemCGS ? Config.shapeY : Config.shapeY * 100;
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
            float point1 = Config.unitSystemCGS ? Float.parseFloat(pointElements[1]) : Float.parseFloat(pointElements[1]) * 100;
            float point2 = Config.unitSystemCGS ? Float.parseFloat(pointElements[2]) : Float.parseFloat(pointElements[2]) * 100;
            points.add(new Point(point1, point2));
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
        public float xLo;
        public float yLo;
        public float xHi;
        public float yHi;
        public int cellId;

        public GridCell(int cellId, float xLo, float yLo, float xHi, float yHi) {
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
    public HashMap<Integer, GridCell> parsGridSchema(Path fileName) throws IOException {
        HashMap<Integer, GridCell> gridSchema = new HashMap<>();

        List<String> fileLines = Files.readAllLines(fileName);

        List<String> headers = Arrays.stream(fileLines.get(8).replace("ITEM: CELLS ", "").strip().split(" ")).toList();
        int idIndex = headers.indexOf("id");
        int xLoIndex = headers.indexOf("xlo");
        int yLoIndex = headers.indexOf("ylo");
        int xHiIndex = headers.indexOf("xhi");
        int yHiIndex = headers.indexOf("yhi");

        for (int i = 9; i < fileLines.size(); i++) {
            String[] params = fileLines.get(i).split(" ");
            int cellId = Integer.parseInt(params[idIndex]);
            gridSchema.put(cellId,
                    new GridCell(cellId,
                            Config.unitSystemCGS ? Float.parseFloat(params[xLoIndex]) : Float.parseFloat(params[xLoIndex]) * 100,
                            Config.unitSystemCGS ? Float.parseFloat(params[yLoIndex]) : Float.parseFloat(params[yLoIndex]) * 100,
                            Config.unitSystemCGS ? Float.parseFloat(params[xHiIndex]) : Float.parseFloat(params[xHiIndex]) * 100,
                            Config.unitSystemCGS ? Float.parseFloat(params[yHiIndex]) : Float.parseFloat(params[yHiIndex]) * 100
                    )
            );
        }

        return gridSchema;
    }

    public HashMap<Integer, HashMap<Integer, Integer>> revertGridSchema(HashMap<Integer, GridCell> gridSchema) {
        HashMap<Integer, HashMap<Integer, Integer>> revertedGridSchema = new HashMap<>();

        for (Integer key: gridSchema.keySet()) {
            int xKey = Math.round(gridSchema.get(key).xLo / Config.spartaCellSize);
            int yKey = Math.round(gridSchema.get(key).yLo / Config.spartaCellSize);
            if (!revertedGridSchema.containsKey(xKey)) {
                revertedGridSchema.put(xKey, new HashMap<>());
            }
            revertedGridSchema.get(xKey).put(yKey, key);
        }

        return revertedGridSchema;
    }

    private Integer getGridId(HashMap<Integer, HashMap<Integer, Integer>> revertedGridSchema, float xCoord, float yCoord) {
        int xKey = Math.round(xCoord / Config.spartaCellSize);
        int yKey = Math.round(yCoord / Config.spartaCellSize);
        if (revertedGridSchema.containsKey(xKey)) {
            if (revertedGridSchema.get(xKey).containsKey(yKey)) {
                return revertedGridSchema.get(xKey).get(yKey);
            }
        }
        return null;
    }

    public HashMap<Integer, Float> parseDulovsData(Path dataFileName,
                                                   Path xFileName,
                                                   Path yFileName,
                                                   HashMap<Integer, HashMap<Integer, Integer>> gridSchemaRevert) throws IOException {
        HashMap<Integer, Float> mappedData = new HashMap<>();
        ArrayList<List<Float>> data = new ArrayList<>();

        if (Files.exists(dataFileName) && Files.exists(xFileName) && Files.exists(yFileName)) {
            List<String> xFileLines = Files.readAllLines(xFileName);
            ArrayList<Float> xCoords = xFileLines.stream().map(Float::parseFloat).collect(Collectors.toCollection(ArrayList::new));

            List<String> yFileLines = Files.readAllLines(yFileName);
            ArrayList<Float> yCoords = yFileLines.stream().map(Float::parseFloat).collect(Collectors.toCollection(ArrayList::new));

            List<String> dataFileLines = Files.readAllLines(dataFileName);
            for (String dataFileLine : dataFileLines) {
                String[] densities = dataFileLine.split("\t");
                data.add(Arrays.stream(densities).map(Float::parseFloat).toList());
            }

            for (int i = 0; i < xCoords.size(); i++) {
                for (int j = 0; j < yCoords.size(); j++) {
                    Integer gridId = getGridId(gridSchemaRevert, xCoords.get(i), yCoords.get(j));
                    if (gridId == null) {
                        continue;
                    }
                    mappedData.put(gridId, data.get(i).get(j));
                }
            }
        }

        return mappedData;
    }
}
