package com.example.administrator.smartphonesensing;

import java.util.Vector;

/**
 * Created by Sergio on 6/21/17.
 */

public class KNN {
    public static String knn(RSSPoint refPoint, Vector<RSSPoint> RSSPoints) {
        int smallestDistance = 0;
        String label = "some place, somewhere...";

        for (RSSPoint rp : RSSPoints){
            int distance = 0;
            for (String key : refPoint.accessPoints.keySet()){
                distance += Math.pow((refPoint.getApDistance(key) - rp.getApDistance(key)), 2);
            }
            distance = (int) Math.sqrt(distance);
            if(smallestDistance == 0 || smallestDistance > distance) {
                smallestDistance = distance;
                label = rp.label;
            }
        }
        return label;
    }

    public static String knn(ACCPoint refPoint, Vector<ACCPoint> ACCPoints) {
        int smallestDistance = 0;
        String label = "some place, somewhere...";

        for (ACCPoint ap : ACCPoints){

            int distance = 0;
            distance += Math.pow((refPoint.getX() - ap.getX()), 2);
            distance += Math.pow((refPoint.getY() - ap.getY()), 2);
            distance += Math.pow((refPoint.getZ() - ap.getZ()), 2);
            distance = (int) Math.sqrt(distance);

            if(smallestDistance == 0 || smallestDistance > distance) {
                smallestDistance = distance;
                label = ap.label;
            }
        }
        return label;
    }
}
