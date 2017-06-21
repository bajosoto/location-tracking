package com.example.administrator.smartphonesensing;

/**
 * From https://github.com/iutinvg/compass
 */

import android.hardware.SensorManager;
import android.widget.TextView;


public class Compass {
    private static final String TAG = "Compass";

    private float[] mGravity = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float azimuth = 0f;
    private float currectAzimuth = 0;
    private float calibration = 210;

    // compass arrow to rotate
    private TextView arrowView = null;
    private TextView calTextView = null;

    public Compass(TextView tv, TextView cal) {
        this.arrowView = tv;
        this.arrowView.setText("Compass initialized");
        this.calTextView = cal;
        this.calTextView.setText(" " + calibration + " ");
    }

    public void incCalibration() {
        if (calibration < 360)
            calibration += 10;
        this.calTextView.setText(" " + calibration + " ");
    }

    public void decCalibration() {
        if (calibration > 0)
            calibration -= 10;
        this.calTextView.setText(" " + calibration + " ");
    }

    private void adjustCompassText() {

        float calibratedAzimuth = (azimuth + calibration) % 360;
        String direction;

        if(calibratedAzimuth < 45)
            direction = "N";
        else if(calibratedAzimuth < 90)
            direction = "NE";
        else if(calibratedAzimuth < 135)
            direction = "E";
        else if(calibratedAzimuth < 180)
            direction = "SE";
        else if(calibratedAzimuth < 225)
            direction = "S";
        else if(calibratedAzimuth < 270)
            direction = "SW";
        else if(calibratedAzimuth < 315)
            direction = "W";
        else
            direction = "NW";

        arrowView.setText(direction);
    }

    public void updateCompass(float[] values, String sens) {
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

                adjustCompassText();
            }

    }
}

