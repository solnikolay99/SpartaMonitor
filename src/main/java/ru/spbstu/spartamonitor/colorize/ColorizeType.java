package ru.spbstu.spartamonitor.colorize;

public enum ColorizeType {
    //DENSITY("Плотность", "Pa", 0, 100000, 10000, 2500),
    DENSITY_STATIC("Статическое давление", "Pa", 0, 20000, 2000, 500, false),
    DENSITY_DYNAMIC("Динамическое давление", "Pa", 0, 20000, 2000, 500, false),
    TEMPERATURE("Температура", "К", 0, 360, 30, 5, false),
    VELOCITY("Скорость", "м/с", 0, 3000, 300, 50, false),
    SOUND_VELOCITY("Скорость звука", "м/с", 0, 1000, 100, 20, false),
    MACH("Число Маха", "", 0, 10, 1, 1, false),
    BIND("Привязка к процессору", "", 0, 16, 1, 1, false),
    N_COUNT("Число расчётных частиц", "", 0, 30, 3, 1, false),
    NRHO("Nrho count ед./см3", "", 0, 2E17, 2E16, 1E16, false),
    DENSITY_STATIC_DIF("Разница давлений", "", 0, 200, 20, 2, false),
    DENSITY_DYNAMIC_DIF("Разница полных давлений", "", 0, 200, 20, 2, false),
    NRHO_DIF("Разница концентраций", "", 0, 200, 20, 2, false),
    NRHO_DULOV("Nrho count by Dulov ед./см3", "", 0, 2E17, 2E16, 1E16, false);
    public final String label;
    public final String units;
    public final double minValue;
    public final double maxValue;
    public final double stepValue;
    public final double smallStepValue;
    public final boolean exponential;

    ColorizeType(String label,
                 String units,
                 double minValue,
                 double maxValue,
                 double stepValue,
                 double smallStepValue,
                 boolean exponential) {
        this.label = label;
        this.units = units;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = stepValue;
        this.smallStepValue = smallStepValue;
        this.exponential = exponential;
    }
}
