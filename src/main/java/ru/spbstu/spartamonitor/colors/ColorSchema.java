package ru.spbstu.spartamonitor.colors;

import java.util.HashMap;
import java.util.List;

public class ColorSchema {

    HashMap<String, List<List<Integer>>> _jetSchema = new HashMap<>() {
        {
            put("red", List.of(
                    List.of(0, 0, 0),
                    List.of(89, 0, 0),
                    List.of(168, 255, 255),
                    List.of(227, 255, 255),
                    List.of(255, 127, 127)
            ));
            put("green", List.of(
                    List.of(0, 0, 0),
                    List.of(32, 0, 0),
                    List.of(96, 255, 255),
                    List.of(163, 255, 255),
                    List.of(232, 0, 0),
                    List.of(255, 0, 0)));
            put("blue", List.of(
                    List.of(0, 127, 127),
                    List.of(28, 255, 255),
                    List.of(87, 255, 255),
                    List.of(166, 0, 0),
                    List.of(255, 0, 0)));
        }
    };

}
