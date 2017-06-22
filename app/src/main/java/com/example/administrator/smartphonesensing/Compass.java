package com.example.administrator.smartphonesensing;

/**
 * Created by Sergio on 5/12/17.
 */

import android.hardware.SensorManager;
import android.widget.TextView;


public class Compass {

    public enum Cardinal {
        N  ,
        NE ,
        E  ,
        SE ,
        S  ,
        SW ,
        W ,
        NW
    }

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private float calibration = 335;
    Cardinal direction;

    // compass arrow to rotate
    private TextView directionTextView = null;
    private TextView calTextView = null;

    public Compass(TextView tv, TextView cal) {
        this.directionTextView = tv;
        this.directionTextView.setText("Compass initialized");
        this.calTextView = cal;
        this.calTextView.setText(" " + calibration + " ");
        direction = Cardinal.N; // Just initializing to anything so compiler doesn't yell at me
    }

    public Cardinal getDirection() {
        return direction;
    }

    public void incCalibration() {
        if (calibration < 360)
            calibration += 5;
        else
            calibration = 0;
        this.calTextView.setText(" " + calibration + " ");
    }

    public void decCalibration() {
        if (calibration > 0)
            calibration -= 5;
        else
            calibration = 360;
        this.calTextView.setText(" " + calibration + " ");
    }

    private void updateCompassCardinal() {

        float calibratedAzimuth = (azimuth + calibration) % 360;

        if(calibratedAzimuth < 45)
            direction = Cardinal.N;
        else if(calibratedAzimuth < 90)
            direction = Cardinal.NE;
        else if(calibratedAzimuth < 135)
            direction = Cardinal.E;
        else if(calibratedAzimuth < 180)
            direction = Cardinal.SE;
        else if(calibratedAzimuth < 225)
            direction = Cardinal.S;
        else if(calibratedAzimuth < 270)
            direction = Cardinal.SW;
        else if(calibratedAzimuth < 315)
            direction = Cardinal.W;
        else
            direction = Cardinal.NW;

        directionTextView.setText(direction + " " + (int)calibratedAzimuth);
    }

    // Smoothing inspired by https://github.com/iutinvg/compass
    public void updateCompass(float[] values, String sens) {  // TODO: use an enum instead of String
        final float alpha = 0.97f;

        if (sens == "accel") {
            mGravity[0] = alpha * mGravity[0] + (1 - alpha) * values[0];
            mGravity[1] = alpha * mGravity[1] + (1 - alpha) * values[1];
            mGravity[2] = alpha * mGravity[2] + (1 - alpha) * values[2];
        }

        if (sens == "magnet") {
            mGeomagnetic[0] = alpha * mGeomagnetic[0] + (1 - alpha) * values[0];
            mGeomagnetic[1] = alpha * mGeomagnetic[1] + (1 - alpha) * values[1];
            mGeomagnetic[2] = alpha * mGeomagnetic[2] + (1 - alpha) * values[2];
        }

        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);

        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);

            azimuth = (float) Math.toDegrees(orientation[0]); // orientation
            azimuth = (azimuth + 360) % 360;

            updateCompassCardinal();
        }
    }
}

