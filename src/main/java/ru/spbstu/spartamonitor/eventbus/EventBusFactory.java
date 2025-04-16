package ru.spbstu.spartamonitor.eventbus;

import com.google.common.eventbus.EventBus;

public final class EventBusFactory {
    private static final EventBus eventBus = new EventBus();
    public static EventBus getEventBus() {
        return eventBus;
    }
}
