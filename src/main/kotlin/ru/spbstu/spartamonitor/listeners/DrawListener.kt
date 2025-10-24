package ru.spbstu.spartamonitor.listeners

import com.google.common.eventbus.Subscribe
import ru.spbstu.spartamonitor.SpartaMonitorController
import ru.spbstu.spartamonitor.events.DrawEvent

class DrawListener(private val controller: SpartaMonitorController) {
    private var direction: Int = 1 // направление проигрывания: 1 - в прямом порядке; -1 - в обратном порядке

    @Subscribe
    fun handleDrawEvent(event: DrawEvent) {
        if (event.direction != null) {
            if (event.direction == 0) {
                controller.frameGenerator.showOneIteration()
                controller.drawIteration(0)
            } else {
                direction = event.direction
                controller.drawIteration(direction)
            }
        } else {
            controller.drawIteration(direction)
        }
    }
}
