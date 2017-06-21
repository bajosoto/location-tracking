package com.example.administrator.smartphonesensing;

import android.graphics.Color;
import android.widget.TextView;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * Created by Sergio on 6/21/17.
 */

// TODO: Serialize movement training data. It might be a pain in the ass since we're sending it TextViews from main Activity

public class Movement {

    public enum AccelScanAction {
        TRAIN_WALK,
        TRAIN_STAND,
        DETECT_WALK,
        NONE
    }

    private TextView currentX;
    private TextView currentY;
    private TextView currentZ;
    private TextView textAcc;

    private static Vector<ACCPoint> accPoints;
    private List<Float> xResults;
    private List<Float> yResults;
    private List<Float> zResults;

    private AccelScanAction accelAction;
    private int currAccSample;
    private int numACCSamples;

    LogWriter logAcc;

    public Movement (TextView _currentX, TextView _currentY, TextView _currentZ, TextView _textAcc, int _numACCSamples) {
        currentX = _currentX;
        currentY = _currentY;
        currentZ = _currentZ;
        textAcc = _textAcc;
        accelAction = AccelScanAction.NONE;
        currAccSample = 0;
        numACCSamples = _numACCSamples;
        accPoints = new Vector();
        xResults = new Vector();
        yResults = new Vector();
        zResults = new Vector();
        logAcc = new LogWriter("logAcc.txt");
        logAcc.clearFile();
    }

    public void updateDebug(float[] values) {
        float aX = 0;
        float aY = 0;
        float aZ = 0;

        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");

        // get the the x,y,z values of the accelerometer
        aX = values[0];
        aY = values[1];
        aZ = values[2];

        // display the current x,y,z accelerometer values
        currentX.setText(Float.toString(aX));
        currentY.setText(Float.toString(aY));
        currentZ.setText(Float.toString(aZ));

        if (accelAction != AccelScanAction.NONE) {
            if (currAccSample == 0) {        // First element is stored unfiltered
                xResults.add(aX);
                yResults.add(aY);
                zResults.add(aZ);
            } else {                        // Remaining elements are smoothed
                xResults.add(0.5f * aX + 0.5f * xResults.get(currAccSample - 1));
                yResults.add(0.5f * aY + 0.5f * yResults.get(currAccSample - 1));
                zResults.add(0.5f * aZ + 0.5f * zResults.get(currAccSample - 1));
            }
            currAccSample++;
            if (currAccSample == numACCSamples) {   // If this is the last sample in the window
                // Sort Lists
                Collections.sort(xResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });
                Collections.sort(yResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });
                Collections.sort(zResults, new Comparator<Float>() {
                    @Override
                    public int compare(Float lhs, Float rhs) {
                        return lhs > rhs ? -1 : (lhs < rhs) ? 1 : 0;
                    }
                });

                // Calculate peak to peak values  TODO: Log p2p values. Sometimes it's detecting walking when standing...
                Float newX = Math.abs(xResults.get(0) - xResults.get(xResults.size() - 1));
                Float newY = Math.abs(yResults.get(0) - yResults.get(yResults.size() - 1));
                Float newZ = Math.abs(zResults.get(0) - zResults.get(zResults.size() - 1));

                // Create new ACCPoint instance
                ACCPoint newAccPoint = new ACCPoint(accelAction.toString(), newX, newY, newZ);

                // TODO: Take this outside the block (e.g. always detect walking or standing) Need to guarantee data  available (see serialize TODO)
                // TODO: We need this in order to know when to update the particle filter
                if (accelAction == AccelScanAction.DETECT_WALK) {
                    // Do KNN and update label
                    textAcc.setText("You are " + KNN.knn(newAccPoint, accPoints));
                } else {
                    // Add new trained data to Vector
                    textAcc.setText("Done!");
                    accPoints.add(newAccPoint);
                }

                currAccSample = 0;
                xResults.clear();
                yResults.clear();
                zResults.clear();
                accelAction = AccelScanAction.NONE;
            }
        }
    }

    public void setAccelAction(AccelScanAction action) {
        accelAction = action;
    }
}
