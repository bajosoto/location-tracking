package com.example.administrator.smartphonesensing;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Sergio on 5/12/17.
 */

public class ReferencePoint {
    String label;
    Map<String, Integer> accessPoints;

    public ReferencePoint(String lbl) {
        this.label = lbl;
        this.accessPoints = new HashMap<String, Integer>();
    }

    public void addAP(String ssid, int rss) {
        this.accessPoints.put(ssid, rss);
    }

    public int getApDistance(String ssid) {
        if (this.accessPoints.containsKey(ssid)) {
            return accessPoints.get(ssid);
        } else {
            return -100;
        }
    }

    public String toString() {
        return label + "\t" + accessPoints.toString();
    }
}
