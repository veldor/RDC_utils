package net.velor.rdc_utils.subclasses;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public class ShiftType implements Serializable {
    public String scheduleName;
    public String scheduleColor;

    public ShiftType(String value, String color) {
        scheduleName = value;
        scheduleColor = color;
    }

    public ShiftType() {}

    @NotNull
    @Override
    public String toString() {
        return "ShiftType{" +
                "scheduleName='" + scheduleName + '\'' +
                ", scheduleColor='" + scheduleColor + '\'' +
                '}';
    }
}
