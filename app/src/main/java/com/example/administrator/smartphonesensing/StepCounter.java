package com.example.administrator.smartphonesensing;

/**
 * Created by Sergio on 6/22/17.
 */

public class StepCounter implements StepListener{ // TODO: Instantiate in Sensors class

    private ParticleFilter particleFilter;
    private Compass compass;
    private FloorMap floorMap;
    //private int xOffset = 0;
    //private int yOffset = 0; //TODO: WE only need this if we "accumulate" steps, in different directions
    private Compass.Cardinal direction;
    private StepDetector stepdetector;
    private boolean walkStarted = false;

    public StepCounter(ParticleFilter _particleFilter, Compass _compass, FloorMap _floorMap) {
        particleFilter = _particleFilter;
        floorMap = _floorMap;
        compass = _compass;
        direction = Compass.Cardinal.N; // Just random init
        stepdetector = new StepDetector();
        stepdetector.registerListener(this);
    }

    public void count(long timestamp, float[] sensordata) {
        stepdetector.detectStep(timestamp, sensordata[0], sensordata[1], sensordata[2]);
    }

    public void incSteps(int steps) {
        direction = compass.getDirection();
        // TODO: update particles position (remember it must be a bit random!)
        int xOffset = 0;
        int yOffset = 0;
        double mapProportion = (floorMap.getMapWidth() / (4 * 13));

        // My stride was about .48m
        int offset = (int)((0.48 * steps) * mapProportion);

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
                yOffset -= offset * 0.5;
                xOffset += offset * 0.5;
                break;
            case SE:
                yOffset += offset * 0.5;
                xOffset += offset * 0.5;
                break;
            case NW:
                yOffset -= offset * 0.5;
                xOffset -= offset * 0.5;
                break;
            case SW:
                yOffset += offset * 0.5;
                xOffset -= offset * 0.5;
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
}
