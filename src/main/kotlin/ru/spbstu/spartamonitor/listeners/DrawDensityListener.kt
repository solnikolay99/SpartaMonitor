package ru.spbstu.spartamonitor.listeners

import com.google.common.eventbus.Subscribe
import ru.spbstu.spartamonitor.SpartaMonitorController
import ru.spbstu.spartamonitor.events.DrawDensityEvent

data class DrawDensityListener(val controller: SpartaMonitorController) {
    @Subscribe
    fun handleDrawEvent(event: DrawDensityEvent) {
        controller.drawDensityChart(event.xCoord)
    }
}
