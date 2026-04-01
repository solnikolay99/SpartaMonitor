package ru.spbstu.spartamonitor.colorize;

public enum ColorizeType {
    DENSITY_STATIC("Статическое давление", "кPa", 0, 120000, 24000, 4800, true, 360, false),
//    DENSITY_STATIC("Статическое давление", "Pa", 0, 5000, 500, 125, true, 360, false),
    DENSITY_DYNAMIC("Давление", "кPa", 0, 120000, 24000, 4800, true, 360, false),
//    DENSITY_DYNAMIC("Давление", "Pa", 0, 5000, 500, 125, true, 360, false),
    TEMPERATURE("Температура", "К", 0, 360, 30, 5, false, 360, false),
//    TEMPERATURE("Температура", "К", 0, 56, 18, 9, false, 3, false),
    VELOCITY("Скорость", "м/с", 0, 3000, 300, 50, false, 360, false),
    SOUND_VELOCITY("Скорость звука", "м/с", 0, 1000, 100, 20, false, 360, false),
    MACH("Число Маха", "", 0, 3, 1, 1, false, 3, false),
    BIND("Привязка к процессору", "", 0, 16, 1, 1, false, 360, false),
    N_COUNT("Число расчётных частиц", "", 0, 30, 3, 1, false, 360, false),
    NRHO("Концентрация", "ед./см3", 0, 2E17, 5E16, 1E16, false, 360, false),
//    NRHO("Концентрация", "ед./см3", 0, 5E15, 5E14, 1E14, false, 360, false),
    MEAN_FREE_PATH("Длина свободного пробега", "м", 0, 1E-7, 5E-8, 1E-8, false, 360, false),
    KNUDSEN_VALUE("Число Кнудсена", "", 0, 0.5, 0.05, 0.01, false, 360, false),
    DENSITY_STATIC_DIF("Разница давлений", "", 0, 200, 20, 2, false, 360, false),
    DENSITY_DYNAMIC_DIF("Разница полных давлений", "", 0, 200, 20, 2, false, 360, false),
    NRHO_DIF("Разница концентраций", "", 0, 200, 20, 2, false, 360, false),
    NRHO_DULOV("Nrho count by Dulov ед./см3", "", 0, 2E17, 2E16, 1E16, false, 360, false);
    public final String label;
    public final String units;
    public final double minValue;
    public double maxValue;
    public double stepValue;
    public double smallStepValue;
    public final boolean compactValue;
    public final int countColors;
    public final boolean exponential;

    ColorizeType(String label,
                 String units,
                 double minValue,
                 double maxValue,
                 double stepValue,
                 double smallStepValue,
                 boolean compactValue,
                 int countColors,
                 boolean exponential) {
        this.label = label;
        this.units = units;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = stepValue;
        this.smallStepValue = smallStepValue;
        this.compactValue = compactValue;
        this.countColors = countColors;
        this.exponential = exponential;
    }

    public static void changeBorders(ColorizeType type, float maxValue) {
        if (maxValue > 0) {
            type.maxValue = maxValue;
            type.stepValue = maxValue / 10;
            type.smallStepValue = maxValue / 40;
        }
    }
}
