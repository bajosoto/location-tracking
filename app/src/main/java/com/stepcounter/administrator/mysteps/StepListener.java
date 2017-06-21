package com.stepcounter.administrator.mysteps;

/**
 * Created by Administrator on 15-06-2017.
 */

public interface StepListener {

    /**
     * Called when a step has been detected.  Given the time in nanoseconds at
     * which the step was detected.
     */
    public void step(long timeNs);
}
