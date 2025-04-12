package ru.spbstu.spartamonitor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Config {
    public static boolean debugDrawing = Boolean.TRUE;  // debug flag

    // Задание параметров
    public static int shapeX = 27;      // высота расчетной области (в см)
    public static int shapeY = 4;       // ширина расчетной области (в см)
    public static final int maxBoxX = 1500; // максимальная высота расчетной области (в точках)
    public static final int maxBoxY = 700;  // максимальная ширина расчетной области (в точках)
    public static int defaultBoxX = maxBoxX; // базовая высота расчетной области (в точках)
    public static int defaultBoxY = maxBoxY;  // базовая ширина расчетной области (в точках)
    public static float coeffXY = 1.0f;
    public static int mainBoxX = defaultBoxX;   // высота расчетной области (в точках)
    public static int mainBoxY = defaultBoxY;   // ширина расчетной области (в точках)
    public static int shiftBoxX = 0;
    public static int shiftBoxY = 0;

    public static double tStep = 5e-8;  // временной шаг (в секундах)

    // New config params
    public static String dumpDirPath = "";

    public static final int defaultMultiplayer = 200;
    public static int multiplayer = defaultMultiplayer;
    public static double kB1 = 1 / 1.38067E-16;
    public static HashMap<String, String> globalParams = new HashMap<>();
    public static List<String> surfFiles = new ArrayList<>();
}
