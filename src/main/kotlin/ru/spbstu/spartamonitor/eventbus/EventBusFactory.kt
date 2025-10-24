package ru.spbstu.spartamonitor.eventbus

import com.google.common.eventbus.EventBus

object EventBusFactory {
    @JvmField
    val eventBus: EventBus = EventBus()
}
