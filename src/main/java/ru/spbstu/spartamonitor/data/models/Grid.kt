package ru.spbstu.spartamonitor.data.models

data class Grid(
    var cells: MutableMap<Int, FloatArray> = mutableMapOf(),
    var procs: MutableMap<Int, Int> = mutableMapOf()
) {
    fun addCell(key: Int, value: FloatArray) {
        this.cells[key] = value
    }

    fun bindProc(key: Int, value: Int) {
        this.procs[key] = value
    }
}
