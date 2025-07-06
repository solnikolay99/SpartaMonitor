package ru.spbstu.spartamonitor.listeners;

import com.google.common.eventbus.Subscribe;
import ru.spbstu.spartamonitor.SpartaMonitorController;
import ru.spbstu.spartamonitor.events.DrawDensityEvent;
import ru.spbstu.spartamonitor.events.DrawEvent;

public class DrawDensityListener {
    private final SpartaMonitorController controller;

    public DrawDensityListener(SpartaMonitorController controller) {
        this.controller = controller;
    }

    @Subscribe
    public void handleDrawEvent(DrawDensityEvent event) {
        controller.drawDensityChart(event.xCoord());
    }
}
