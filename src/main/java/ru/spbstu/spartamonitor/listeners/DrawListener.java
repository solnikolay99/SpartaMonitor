package ru.spbstu.spartamonitor.listeners;

import com.google.common.eventbus.Subscribe;
import ru.spbstu.spartamonitor.SpartaMonitorController;
import ru.spbstu.spartamonitor.events.DrawEvent;

public class DrawListener {
    private final SpartaMonitorController controller;

    public DrawListener(SpartaMonitorController controller) {
        this.controller = controller;
    }

    @Subscribe
    public void handleDrawEvent(DrawEvent event) {
        controller.drawIteration();
    }
}
