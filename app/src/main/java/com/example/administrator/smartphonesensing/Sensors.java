package com.example.administrator.smartphonesensing;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Created by Sergio on 6/21/17.
 */

public class Sensors implements SensorEventListener, StepListener{
    private SensorManager sensorManager;
    private Sensor gsensor;
    private Sensor msensor;

    private Compass compass;
    private Movement movement;
    private StepDetector stepdetector;

    private int numSteps = 0;

    public Sensors(Context context, Compass comp, Movement move) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        gsensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        stepdetector = new StepDetector();
        stepdetector.registerListener(this);
        compass = comp;
        movement = move;
    }

    public void start() {
        sensorManager.registerListener(this, gsensor, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, msensor, SensorManager.SENSOR_DELAY_UI);
    }

    public void stop() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        synchronized (this) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                compass.updateCompass(event.values, Compass.SensorType.ACCEL);
                movement.updateDebug(event.values);
                stepdetector.detectStep(event.timestamp, event.values[0], event.values[1], event.values[2]);
            }

            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                compass.updateCompass(event.values, Compass.SensorType.MAGNET);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void step(long timeNs) {
        numSteps++;
        //stepCount.setText(TEXT_NUM_STEPS + numSteps);
    }
}
