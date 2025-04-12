package ru.spbstu.spartamonitor.data.models;

import java.io.Serializable;

public class Point implements Serializable, Cloneable {
    public float x;
    public float y;
    public float z;

    public Point(float x,
                 float y) {
        this.x = x;
        this.y = y;
        this.z = 0;
    }

    @Override
    public Point clone() {
        try {
            return (Point) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
