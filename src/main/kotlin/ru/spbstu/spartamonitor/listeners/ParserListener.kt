package ru.spbstu.spartamonitor.listeners

import com.google.common.eventbus.Subscribe
import ru.spbstu.spartamonitor.SpartaMonitorController
import ru.spbstu.spartamonitor.events.ParserEvent

data class ParserListener(val controller: SpartaMonitorController) {
    @Subscribe
    fun handleParserEvent(event: ParserEvent) {
        controller.currentFrameNumber.text = event.countFrames.toString()
    }
}
