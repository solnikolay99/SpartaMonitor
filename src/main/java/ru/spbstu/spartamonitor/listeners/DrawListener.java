package ru.spbstu.spartamonitor.listeners;

import com.google.common.eventbus.Subscribe;
import ru.spbstu.spartamonitor.SpartaMonitorController;
import ru.spbstu.spartamonitor.events.DrawEvent;

public class DrawListener {
    private final SpartaMonitorController controller;
    private Integer direction = 1; // направление проигрывания: 1 - в прямом порядке; -1 - в обратном порядке

    public DrawListener(SpartaMonitorController controller) {
        this.controller = controller;
    }

    @Subscribe
    public void handleDrawEvent(DrawEvent event) {
        if (event.getDirection() != null) {
            if (event.getDirection() == 0) {
                controller.frameGenerator.showOneIteration();
                controller.drawIteration(0);
            } else {
                direction = event.getDirection();
                controller.drawIteration(direction);
            }
        } else {
            controller.drawIteration(direction);
        }
    }
}
