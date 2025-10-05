package ru.spbstu.spartamonitor.calculate

import ru.spbstu.spartamonitor.data.models.Timeframe

class Calculation {

    fun calculateTargetDiameter(timeframe : Timeframe, percentile: Float) : Diameter {
        val diameter = Diameter()
        val target: MutableList<Int> = timeframe.target
        var minValue = 0
        var maxvalue = target.size

        if (target.isEmpty()) {
            return diameter
        }

        val minY = (1 - percentile) * target.max()

        for (i in 0..(target.size / 2)) {
            if (target[i] > minY) {
                break
            } else {
                minValue = i
            }
        }
        for(i in (target.size - 1)..(target.size / 2)) {
            if (target[i] > minY) {
                break
            } else {
                maxvalue = i
            }
        }

        val reducer = 8
        diameter.diameter = (maxvalue - minValue) * reducer
        diameter.leftBorder = minValue * reducer
        diameter.rightBorder = maxvalue * reducer

        return diameter
    }
}