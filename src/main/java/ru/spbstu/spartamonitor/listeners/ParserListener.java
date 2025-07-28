package ru.spbstu.spartamonitor.listeners;

import com.google.common.eventbus.Subscribe;
import ru.spbstu.spartamonitor.SpartaMonitorController;
import ru.spbstu.spartamonitor.events.ParserEvent;

public class ParserListener {

    private final SpartaMonitorController controller;

    public ParserListener(SpartaMonitorController controller) {
        this.controller = controller;
    }

    @Subscribe
    public void handleParserEvent(ParserEvent event) {
        controller.currentFrameNumber.setText(String.format("%d", event.countFrames()));
    }
}
