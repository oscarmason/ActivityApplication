package com.mdp.cw4.runningtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <h1>Workout Session Database</h1>
 * Wrapper around the workout session database which stores the details about past workout sessions
 * including:
 * <p>
 * <ul>
 * <li>Date</li>
 * <li>Month</li>
 * <li>Year</li>
 * <li>Hour</li>
 * <li>Minute</li>
 * <li>Duration</li>
 * <li>Distance</li>
 * <li>Workout type</li>
 * <li>Latitude</li>
 * <li>Longitude</li>
 * </ul>
 */
public class WorkoutSessionDatabase extends SQLiteOpenHelper {

    public WorkoutSessionDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + WorkoutSessionContentProvider.SESSION_TABLE_NAME + " "
                + "(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                WorkoutSessionContentProvider.Contract.DATE + " INTEGER, " +
                WorkoutSessionContentProvider.Contract.MONTH + " " + "INTEGER, " +
                WorkoutSessionContentProvider.Contract.YEAR + " INTEGER, " +
                WorkoutSessionContentProvider.Contract.HOUR + " INTEGER, " +
                WorkoutSessionContentProvider.Contract.MINUTE + " INTEGER, " +
                WorkoutSessionContentProvider.Contract.DURATION + " INTEGER, " +
                WorkoutSessionContentProvider.Contract.DISTANCE + " INTEGER," +
                WorkoutSessionContentProvider.Contract.WORKOUT_TYPE + " INTEGER," +
                WorkoutSessionContentProvider.Contract.LATITUDE + " TEXT, " +
                WorkoutSessionContentProvider.Contract.LONGITUDE + " TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        database.execSQL("DROP TABLE IF EXISTS " + WorkoutSessionContentProvider.SESSION_TABLE_NAME);
        onCreate(database);
    }
}
