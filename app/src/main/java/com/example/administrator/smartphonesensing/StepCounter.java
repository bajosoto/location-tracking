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

        // My stride was about .5m
        int offset = (int)((0.5 * steps) * (floorMap.getMapWidth() / (4 * 13)));

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
            default:  //TODO: There is no NW, SW, NE, NW yet!
                break;
        }
        particleFilter.updateParticles(xOffset, yOffset);
    }

    @Override
    public void step(long timeNs) { //TODO: Call IncStep or something from here
        //numSteps++;
        //stepCount.setText(TEXT_NUM_STEPS + numSteps);
    }
}
