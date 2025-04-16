package ru.spbstu.spartamonitor.calculate;

import ru.spbstu.spartamonitor.data.models.Timeframe;

import java.util.Arrays;

public class Calculation {

    private static final int reducer = 8;

    public static class Diameter {
        public int diameter = 0;
        public int leftBorder = 0;
        public int rightBorder = 0;
    }

    public static Diameter calculateTargetDiameter(Timeframe timeframe, float percentile) {
        Diameter diameter = new Diameter();
        Integer[] target = timeframe.getTarget();
        int minValue = 0;
        int maxvalue = target.length;

        if (target.length == 0) {
            return diameter;
        }

        float minY = (1 - percentile) * Arrays.stream(target).max(Integer::compareTo).get();

        for (int i = 0; i < target.length / 2; i++) {
            if (target[i] > minY) {
                break;
            } else {
                minValue = i;
            }
        }
        for (int i = target.length - 1; i > target.length / 2; i--) {
            if (target[i] > minY) {
                break;
            } else {
                maxvalue = i;
            }
        }

        diameter.diameter = (maxvalue - minValue) * reducer;
        diameter.leftBorder = minValue * reducer;
        diameter.rightBorder = maxvalue * reducer;

        return diameter;
    }
}
