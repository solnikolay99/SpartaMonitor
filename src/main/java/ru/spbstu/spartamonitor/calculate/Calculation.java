package ru.spbstu.spartamonitor.calculate;

import ru.spbstu.spartamonitor.data.models.Timeframe;

import java.util.List;

public class Calculation {

    private static final int reducer = 8;

    public static class Diameter {
        public int diameter = 0;
        public int leftBorder = 0;
        public int rightBorder = 0;
    }

    public static Diameter calculateTargetDiameter(Timeframe timeframe, float percentile) {
        Diameter diameter = new Diameter();
        List<Integer> target = timeframe.getTarget();
        int minValue = 0;
        int maxvalue = target.size();

        if (target.isEmpty()) {
            return diameter;
        }

        float minY = (1 - percentile) * target.stream().max(Integer::compareTo).get();

        for (int i = 0; i < target.size() / 2; i++) {
            if (target.get(i) > minY) {
                break;
            } else {
                minValue = i;
            }
        }
        for (int i = target.size() - 1; i > target.size() / 2; i--) {
            if (target.get(i) > minY) {
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
