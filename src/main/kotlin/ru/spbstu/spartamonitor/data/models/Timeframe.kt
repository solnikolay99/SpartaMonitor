package ru.spbstu.spartamonitor.data.models

data class Timeframe(
    var points: MutableList<Array<Number>> = mutableListOf(),
    var grid: Grid = Grid(),
    var target: MutableList<Int> = mutableListOf(),
    var countPoints: Int = 0
)
