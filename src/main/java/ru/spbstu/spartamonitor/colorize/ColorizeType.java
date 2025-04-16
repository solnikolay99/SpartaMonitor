package ru.spbstu.spartamonitor.colorize;

public enum ColorizeType {
    DENSITY("Плотность, Pa", 0, 1080, 90, 15),
    TEMPERATURE("Температура, К", 0, 360, 30, 5),
    VELOCITY("Скорость, м/с", 0, 3000, 300, 50),
    MACH("Число Маха", 0, 10, 1, 1);
    public final String label;
    public final int minValue;
    public final int maxValue;
    public final int stepValue;
    public final int smallStepValue;

    ColorizeType(String label, int minValue, int maxValue, int stepValue, int smallStepValue) {
        this.label = label;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepValue = stepValue;
        this.smallStepValue = smallStepValue;
    }
}
