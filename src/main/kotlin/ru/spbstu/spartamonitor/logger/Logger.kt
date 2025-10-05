package ru.spbstu.spartamonitor.logger

import config.DEBUG_DRAWING

object Logger {
    private val timers = mutableMapOf<String, Long>()

    @JvmStatic
    fun startTimer(timerName: String) {
//        if (Config.debugDrawing) {
//            println("Start execution for '$timerName'");
//        }
        timers[timerName] = System.currentTimeMillis()
    }

    @JvmStatic
    fun releaseTimer(timerName: String): Float {
        if (DEBUG_DRAWING) {
            System.out.printf(
                "Execution time for '$timerName' is %.2f s\n",
                (System.currentTimeMillis() - timers[timerName]!!).toFloat() / 1000
            )
        }
        return (System.currentTimeMillis() - timers[timerName]!!).toFloat()
    }
}
