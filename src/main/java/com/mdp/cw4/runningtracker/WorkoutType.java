package com.mdp.cw4.runningtracker;

/**
 * <h1>Workout Type</h1>
 * Used to differentiate between the different types of workouts users can track. These include:
 * <ul>
 *     <li>Running</li>
 *     <li>Walking</li>
 *     <li>Cycling</li>
 * </ul>
 */
public enum WorkoutType { RUNNING(0), WALKING(1), CYCLING(2);

    private final int workoutTypeID;

    WorkoutType(int workoutTypeID) {
        this.workoutTypeID = workoutTypeID;
    }

    public int getworkoutTypeID(){
        return workoutTypeID;
    }

    public static WorkoutType getDefault(){
        return WorkoutType.RUNNING;
    }
}