package ru.spbstu.spartamonitor.listeners;

import com.google.common.eventbus.Subscribe;
import ru.spbstu.spartamonitor.SpartaMonitorController;
import ru.spbstu.spartamonitor.events.DrawDensityEvent;

public record DrawDensityListener(SpartaMonitorController controller) {
    @Subscribe
    public void handleDrawEvent(DrawDensityEvent event) {
        controller.drawDensityChart(event.xCoord());
    }
}
