package config

const val DEBUG_DRAWING = true // debug flag
const val MAX_BOX_X: Int = 1500 // максимальная высота расчетной области (в точках)
const val MAX_BOX_Y: Int = 700 // максимальная ширина расчетной области (в точках)

class Config {
    companion object {
        @JvmField
        var PARSE_POINTS = false

        @JvmField
        var shapeX: Float = 10f // высота расчетной области (в см)
        @JvmField
        var shapeY: Float = 15f // ширина расчетной области (в см)
        @JvmField
        var spartaCellSize: Float = 0f // размер ячейки (в см)
        @JvmField
        var monitorCellSizeX: Float = 0f // размер ячейки по X (в см)
        @JvmField
        var monitorCellSizeY: Float = 0f // размер ячейки по Y (в см)
        @JvmField
        var defaultBoxX: Int = MAX_BOX_X // базовая высота расчетной области (в точках)
        @JvmField
        var defaultBoxY: Int = MAX_BOX_Y // базовая ширина расчетной области (в точках)
        @JvmField
        var mainBoxX: Int = defaultBoxX // высота расчетной области (в точках)
        @JvmField
        var mainBoxY: Int = defaultBoxY // ширина расчетной области (в точках)
        @JvmField
        var shiftBoxX: Int = 0
        @JvmField
        var shiftBoxY: Int = 0

        @JvmField
        var dumpDirPath = ""
        @JvmField
        var unitSystemCGS = true // система единиц (true - СГС, false - СИ)
        @JvmField
        var tStep: Float = 5e-8.toFloat() // временной шаг (в секундах)
        @JvmField
        var defaultMultiplayer: Int = 200
        @JvmField
        var multiplayer: Int = defaultMultiplayer
        @JvmField
        var globalParams: MutableMap<String, String> = mutableMapOf()
        @JvmField
        var surfFiles: MutableList<String> = mutableListOf()
    }
}
