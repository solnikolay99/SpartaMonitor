package ru.spbstu.spartamonitor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {
    public static boolean debugDrawing = Boolean.TRUE;  // debug flag

    public static boolean unitSystemCGS = true;  // система единиц (true - СГС, false - СИ)

    // Задание параметров
    public static float shapeX = 10;      // высота расчетной области (в см)
    public static float shapeY = 15;       // ширина расчетной области (в см)
    public static float spartaCellSize = 0f;    // размер ячейки (в см)
    public static float monitorCellSizeX = 0f;  // размер ячейки по X (в см)
    public static float monitorCellSizeY = 0f;  // размер ячейки по Y (в см)
    public static final int maxBoxX = 1500;     // максимальная высота расчетной области (в точках)
    public static final int maxBoxY = 700;      // максимальная ширина расчетной области (в точках)
    public static int defaultBoxX = maxBoxX;    // базовая высота расчетной области (в точках)
    public static int defaultBoxY = maxBoxY;    // базовая ширина расчетной области (в точках)
    public static int mainBoxX = defaultBoxX;   // высота расчетной области (в точках)
    public static int mainBoxY = defaultBoxY;   // ширина расчетной области (в точках)
    public static int shiftBoxX = 0;
    public static int shiftBoxY = 0;

    public static double tStep = 5e-8;  // временной шаг (в секундах)

    // New config params
    public static String dumpDirPath = "";
    public static final boolean parsPoints = false;

    public static int defaultMultiplayer = 200;
    public static int multiplayer = defaultMultiplayer;
    public static double kB1 = 1 / 1.38067E-16;
    public static HashMap<String, String> globalParams = new HashMap<>();
    public static List<String> surfFiles = new ArrayList<>();
}
