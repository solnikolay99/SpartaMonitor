package ru.spbstu.spartamonitor.data;

import javafx.event.Event;
import javafx.event.EventType;

public class ParserEvent extends Event {

    public static final EventType<ParserEvent> CHANGE_TIMEFRAME_COUNT = new EventType<>(Event.ANY, "CHANGE_TIMEFRAME_COUNT");

    public ParserEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }
}
