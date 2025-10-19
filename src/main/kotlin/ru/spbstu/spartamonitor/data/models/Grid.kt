package ru.spbstu.spartamonitor.data.models

data class Grid(
    var cells: MutableMap<Int, Array<Float>> = mutableMapOf(),
    var procs: MutableMap<Int, Int> = mutableMapOf()
) {
    fun addCell(key: Int, value: Array<Float>) {
        this.cells[key] = value
    }

    fun bindProc(key: Int, value: Int) {
        this.procs[key] = value
    }
}
