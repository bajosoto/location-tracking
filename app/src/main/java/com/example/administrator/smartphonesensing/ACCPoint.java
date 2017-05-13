package com.example.administrator.smartphonesensing;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sergio on 5/12/17.
 */

public class ACCPoint {
    String label;
    private Float xPeak;
    private Float yPeak;
    private Float zPeak;

    public ACCPoint(String lbl, Float x, Float y, Float z) {
        this.label = lbl;
        this.xPeak = x;
        this.yPeak = y;
        this.zPeak = z;
    }

    public Float getX() {
        return this.xPeak;
    }

    public Float getY() {
        return this.yPeak;
    }

    public Float getZ() {
        return this.zPeak;
    }

    public String toString() {
        return label + "\t" +
                Float.toString(xPeak) + ", " +
                Float.toString(yPeak) + ", " +
                Float.toString(zPeak) + "\n";
    }
}
