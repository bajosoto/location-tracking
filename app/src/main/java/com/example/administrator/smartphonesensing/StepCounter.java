package com.example.administrator.smartphonesensing;

/**
 * Created by Sergio on 6/22/17.
 */

public class StepCounter implements StepListener{

    private ParticleFilter particleFilter;
    private Compass compass;
    private FloorMap floorMap;
    //private int xOffset = 0;
    //private int yOffset = 0; // We only need this if we "accumulate" steps, in different directions
    private Compass.Cardinal direction;
    private StepDetector stepdetector;
    private int numStride;
    private boolean walkStarted = false;

    public StepCounter(ParticleFilter _particleFilter, Compass _compass, FloorMap _floorMap, int _numStride) {
        particleFilter = _particleFilter;
        floorMap = _floorMap;
        compass = _compass;
        numStride = _numStride;
        direction = Compass.Cardinal.N; // Just random init
        stepdetector = new StepDetector();
        stepdetector.registerListener(this);
    }

    public void count(long timestamp, float[] sensordata) {
        stepdetector.detectStep(timestamp, sensordata[0], sensordata[1], sensordata[2]);
    }

    public void incSteps(int steps) {
        direction = compass.getDirection();
        int xOffset = 0;
        int yOffset = 0;
        double mapProportion = (floorMap.getMapWidth() / (4 * 13));

        // My stride was about .48m
        double stride = numStride / 100.0;
        int offset = (int)((stride * steps) * mapProportion);

        switch (direction) {
            case N:
                yOffset -= offset;
                break;
            case S:
                yOffset += offset;
                break;
            case E:
                xOffset += offset;
                break;
            case W:
                xOffset -= offset;
                break;
            case NE:
                yOffset -= offset * 0.707;  // Hardcoded approximation of sqrt(2)/2
                xOffset += offset * 0.707;  // Multiplications can be optimized, but this makes it more readable
                break;                      // and performance impact is not noticeable
            case SE:
                yOffset += offset * 0.707;
                xOffset += offset * 0.707;
                break;
            case NW:
                yOffset -= offset * 0.707;
                xOffset -= offset * 0.707;
                break;
            case SW:
                yOffset += offset * 0.707;
                xOffset -= offset * 0.707;
                break;
            default:
                break;
        }
        particleFilter.updateParticles(xOffset, yOffset);
    }

    public void startWalk() {
        walkStarted = true;
    }

    public void stopWalk() {
        walkStarted = false;
    }

    public boolean hasWalkStarted() {
        return walkStarted;
    }

    @Override
    public void step(long timeNs) {
        if (walkStarted)
            incSteps(1);
    }

    public int incStride() {
        if (numStride < 100){
            numStride += 1;
        }
        return numStride;
    }

    public int decStride() {
        if (numStride > 0){
            numStride -= 1;
        }
        return numStride;
    }
}
