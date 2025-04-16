package ru.spbstu.spartamonitor.logger;

import java.util.HashMap;

import static ru.spbstu.spartamonitor.config.Config.debugDrawing;

public class Logger {

    private static final HashMap<String, Long> timers = new HashMap<>();

    public static void startTimer(String timerName) {
        if (debugDrawing) {
            //System.out.printf("Start execution for '%s'\n", timerName);
        }
        timers.put(timerName, System.currentTimeMillis());
    }

    public static float releaseTimer(String timerName) {
        if (debugDrawing) {
            System.out.printf("Execution time for '%s' is %.2f s\n", timerName,
                    (float) (System.currentTimeMillis() - timers.get(timerName)) / 1000);
        }
        return System.currentTimeMillis() - timers.get(timerName);
    }
}
