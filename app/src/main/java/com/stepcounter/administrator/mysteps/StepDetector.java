package com.stepcounter.administrator.mysteps;

/**
 * Created by Administrator on 15-06-2017.
 */

public class StepDetector {

    private static final int ACCEL_WIN_SIZE = 50;
    private static final int VEL_WIN_SIZE = 10;
    private static final float STEP_THRESHOLD = 4f;
    private static final int STEP_DELAY_NS = 250000000;

    private int accCounter = 0;
    private float[] accX = new float[ACCEL_WIN_SIZE];
    private float[] accY = new float[ACCEL_WIN_SIZE];
    private float[] accZ = new float[ACCEL_WIN_SIZE];
    private int velCounter = 0;
    private float[] vel = new float[VEL_WIN_SIZE];

    private long lastStepTimeNs = 0;
    private float oldVelocityEstimate = 0;

    private StepListener stepListener;

    public void registerListener(StepListener listener) {
        this.stepListener = listener;
    }

    /* gets accelerometer values and detects steps, informs to listener
    */

    public void detectStep(long timeNs, float x, float y, float z)
    {

        float[] currentAcc = new float[3];
        currentAcc[0] = x;
        currentAcc[1] = y;
        currentAcc[2] = z;

        // First step is to update our guess of where the global z vector is.
        accCounter++;
        accX[accCounter % ACCEL_WIN_SIZE] = currentAcc[0];
        accY[accCounter % ACCEL_WIN_SIZE] = currentAcc[1];
        accZ[accCounter % ACCEL_WIN_SIZE] = currentAcc[2];

        float[] worldZ = new float[3];
        worldZ[0] = sum(accX) / Math.min(accCounter, ACCEL_WIN_SIZE);
        worldZ[1] = sum(accY) / Math.min(accCounter, ACCEL_WIN_SIZE);
        worldZ[2] = sum(accZ) / Math.min(accCounter, ACCEL_WIN_SIZE);

        float normalization_factor = norm(worldZ);

        worldZ[0] = worldZ[0] / normalization_factor;
        worldZ[1] = worldZ[1] / normalization_factor;
        worldZ[2] = worldZ[2] / normalization_factor;

        // Next step is to figure out the component of the current acceleration
        // in the direction of world_z and subtract gravity's contribution
        float currentZ = dot(worldZ, currentAcc) - normalization_factor;
        velCounter++;
        vel[velCounter % VEL_WIN_SIZE] = currentZ;

        float velocityEstimate = sum(vel);

        if (velocityEstimate > STEP_THRESHOLD && oldVelocityEstimate <= STEP_THRESHOLD
                && (timeNs - lastStepTimeNs > STEP_DELAY_NS)) {
            stepListener.step(timeNs);
            lastStepTimeNs = timeNs;
        }
        oldVelocityEstimate = velocityEstimate;
    }

    private static float sum(float[] array)
    {
        float ret = 0;
        for (int i = 0; i < array.length; i++) {
            ret += array[i];
        }
        return ret;
    }

    public static float norm(float[] array) {
        float ret = 0;
        for (int i = 0; i < array.length; i++) {
            ret += array[i] * array[i];
        }
        return (float) Math.sqrt(ret);
    }

    // Note: only works with 3D vectors.
    public static float dot(float[] a, float[] b) {
        float ret = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return ret;
    }

}
