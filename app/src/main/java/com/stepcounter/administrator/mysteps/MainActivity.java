package com.stepcounter.administrator.mysteps;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener,StepListener{

    private StepDetector stepdetector;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private boolean isSensorPresent = false;
    private TextView stepCount;
    private TextView stepString;
    private TextView direction;
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps =0;

    private static float[] accel = new float[3];
    private static float[] magnet = new float[3];
    private int accelCounter = 0;
    private int magnetCounter = 0;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stepCount = (TextView) findViewById(R.id.stepssincereboot);
        stepString = (TextView) findViewById(R.id.stepString);
        direction = (TextView) findViewById(R.id.direction);
        stepdetector = new StepDetector();

        sensorManager = (SensorManager)this.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
        {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            stepdetector.registerListener(this);
        } else {
        }

        if(sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) != null)
        {
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            isSensorPresent = true;
            stepCount.setText("True");
        } else {
            isSensorPresent = false;
            stepCount.setText("False");
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isSensorPresent) {
            numSteps = 0;
            stepCount.setText(TEXT_NUM_STEPS + numSteps);
            sensorManager.registerListener( this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            sensorManager.registerListener( this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isSensorPresent) {
            sensorManager.unregisterListener( this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float alpha = (float)0.2;
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            if (accelCounter == 0){
                accel = event.values;
            }
            else {
                accel[0] = alpha * event.values[0] + (1 - alpha) * accel[0];
                accel[1] = alpha * event.values[1] + (1 - alpha) * accel[1];
                accel[2] = alpha * event.values[2] + (1 - alpha) * accel[2];
                stepdetector.detectStep(event.timestamp, accel[0], accel[1], accel[2]);
            }
            accelCounter++;
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            if (magnetCounter == 0) {
                magnet = event.values;
            }
            else {
                magnet[0] = alpha * event.values[0] + (1 - alpha) * magnet[0];
                magnet[1] = alpha * event.values[1] + (1 - alpha) * magnet[1];
                magnet[2] = alpha * event.values[2] + (1 - alpha) * magnet[2];
            }
            magnetCounter++;
        }

        if(accel != null && magnet != null) {
            float inc[] = new float[9];
            float rot[] = new float[9];

            boolean success = SensorManager.getRotationMatrix(rot, inc, accel, magnet);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(rot, orientation);
                //double azimuth = 180 * orientation[0] / Math.PI; // orientation contains: azimuth, pitch and roll
                double azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;
                direction.setText(Double.toString(azimuth));
            }
        }
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        stepCount.setText(TEXT_NUM_STEPS + numSteps);
    }
}
