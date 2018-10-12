package com.mdp.cw4.runningtracker;

/**
 * <h1>Session Listener</h1>
 * An interface for classes wishing to register to receive updates of an ongoing activity session
 */
public interface IWorkoutSessionListener {
    void onDurationUpdated(long durationMilliseconds);
    void onDistanceUpdated(int distance);
    void onPaceUpdated(double pace);
    void onLocationUpdated(double latitude, double longitude);
}
