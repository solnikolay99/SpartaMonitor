package ru.spbstu.spartamonitor.colorize

enum class ColorizeType(
    val label: String,
    val units: String,
    val minValue: Double,
    val maxValue: Double,
    val stepValue: Double,
    val smallStepValue: Double,
    val exponential: Boolean
) {
    //DENSITY("Плотность", "Pa", 0.0, 100000.0, 10000.0, 2500.0),
    DENSITY_STATIC("Статическое давление", "Pa", 0.0, 20000.0, 2000.0, 500.0, false),
    DENSITY_DYNAMIC("Динамическое давление", "Pa", 0.0, 20000.0, 2000.0, 500.0, false),
    TEMPERATURE("Температура", "К", 0.0, 360.0, 30.0, 5.0, false),
    VELOCITY("Скорость", "м/с", 0.0, 3000.0, 300.0, 50.0, false),
    SOUND_VELOCITY("Скорость звука", "м/с", 0.0, 1000.0, 100.0, 20.0, false),
    MACH("Число Маха", "", 0.0, 10.0, 1.0, 1.0, false),
    BIND("Привязка к процессору", "", 0.0, 16.0, 1.0, 1.0, false),
    N_COUNT("Число расчётных частиц", "", 0.0, 30.0, 3.0, 1.0, false),
    NRHO("Nrho count ед./см3", "", 0.0, 2E17, 2E16, 1E16, false),
    DENSITY_STATIC_DIF("Разница давлений", "", 0.0, 200.0, 20.0, 2.0, false),
    DENSITY_DYNAMIC_DIF("Разница полных давлений", "", 0.0, 200.0, 20.0, 2.0, false),
    NRHO_DIF("Разница концентраций", "", 0.0, 200.0, 20.0, 2.0, false),
    NRHO_DULOV("Nrho count by Dulov ед./см3", "", 0.0, 2E17, 2E16, 1E16, false);
}