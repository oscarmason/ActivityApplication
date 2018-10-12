package com.mdp.cw4.runningtracker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.mdp.cw4.runningtracker.activities.RunningTracker;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for monitoring an ongoing activity session. It provides updates for the
 * duration, user's current position, distance travelled and pace, displays a notification showing
 * the current duration of the activity session, and stores completed sessions
 */
public class WorkoutSessionService extends Service {

    private int date;
    private int month;
    private int year;
    private int hour;
    private int minute;
    private int distance;
    private double pace;
    private boolean sessionRunning;
    private long sessionCurrentDurationMilliseconds;

    private List<IWorkoutSessionListener> sessionListeners;
    private List<Double> longitudes = new ArrayList<>();
    private List<Double> latitudes = new ArrayList<>();

    private IBinder sessionBinder;
    private SessionDurationHandler sessionDurationHandler;
    private AtomicBoolean sessionInProgress;

    private SessionLocationListener sessionLocationListener;
    private NotificationCompat.Builder notification;
    private NotificationManager notificationManager;
    private WorkoutType workoutType;

    private final int UPDATE_MILLISECONDS = 200;
    private final int NOTIFICATION_ID = 11;

    @Override
    public void onCreate(){
        sessionBinder = new SessionBinder();
        sessionDurationHandler = new SessionDurationHandler();
        sessionListeners = new ArrayList<>();
        sessionInProgress = new AtomicBoolean(true);
        sessionRunning = true;

        registerLocationListener();
        setupNotificationBar();
    }

    /**
     * Get the workout type requested by the user
     */
    @Override
    public int onStartCommand (Intent intent, int flags, int startId) {
        workoutType = (WorkoutType) intent.getSerializableExtra(RunningTracker.INTENT_WORKOUT_TYPE);

        if(workoutType == null){
            workoutType = WorkoutType.getDefault();
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        sessionInProgress.set(false);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sessionBinder;
    }

    /**
     * Register the Session Location Listener with the system to receive location updates
     */
    private void registerLocationListener(){
        final int MIN_UPDATE_TIME_MILLISECONDS = 200;
        final int MIN_UPDATE_DISTANCE_METRES = 2;

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sessionLocationListener = new SessionLocationListener();

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_UPDATE_TIME_MILLISECONDS,
                    MIN_UPDATE_DISTANCE_METRES, sessionLocationListener);
        } catch(SecurityException e) {
            e.printStackTrace();
        }
    }

    private void setupNotificationBar(){
        Intent intent = new Intent(this, RunningTracker.class);
        PendingIntent runningTrackerIntent = PendingIntent.getActivity(this, 0, intent, 0);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notification = new NotificationCompat.Builder(this)
                .setTicker(("message"))
                .setSmallIcon(R.drawable.runner)
                .setContentTitle(getString(R.string.app_name))
                .setContentIntent(runningTrackerIntent)
                .setAutoCancel(false);

        startForeground(NOTIFICATION_ID, notification.build());
        updateNotification();
    }

    public void resumeSession(){
        sessionRunning = true;
    }

    public void pauseSession(){
        sessionRunning = false;
    }

    public boolean isSessionRunning(){
        return sessionRunning;
    }

    public boolean isSessionInProgress(){
        return sessionInProgress.get();
    }

    public void stopSession(){
        sessionInProgress.set(false);
        sessionRunning = false;
    }

    /**
     * Updates the notification to display the duration of the current workout session
     */
    private void updateNotification(){
        notification.setContentText(ValueFormatter.formatDuration(sessionDurationHandler.getCurrentDurationMilliseconds()));
        notificationManager.notify(NOTIFICATION_ID, notification.build());
    }

    /**
     * Add the listener and inform them of the current values
     * @param sessionListener   Session Listener to register to receive updates
     */
    public void addSessionListener(IWorkoutSessionListener sessionListener){
        sessionListeners.add(sessionListener);
        sessionListener.onPaceUpdated(pace);
        sessionListener.onDistanceUpdated(distance);
        sessionListener.onDurationUpdated(sessionCurrentDurationMilliseconds);
    }

    /**
     * Remove the session listener passed in from the list of listeners to stop them from receiving
     * updates
     * @param sessionListener   Session Listener to remove form the list
     */
    public void removeSessionListener(IWorkoutSessionListener sessionListener){
        sessionListeners.remove(sessionListener);
    }

    /**
     * Stores the complete session to the database
     */
    public void addSessionToDatabase(){

        String longitudes = parseCoordinates(this.longitudes);
        String latitudes = parseCoordinates(this.latitudes);

        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutSessionContentProvider.Contract.DISTANCE, distance);
        contentValues.put(WorkoutSessionContentProvider.Contract.DURATION, sessionCurrentDurationMilliseconds);
        contentValues.put(WorkoutSessionContentProvider.Contract.DATE, date);
        contentValues.put(WorkoutSessionContentProvider.Contract.MONTH, month);
        contentValues.put(WorkoutSessionContentProvider.Contract.YEAR, year);
        contentValues.put(WorkoutSessionContentProvider.Contract.HOUR, hour);
        contentValues.put(WorkoutSessionContentProvider.Contract.MINUTE, minute);
        contentValues.put(WorkoutSessionContentProvider.Contract.LATITUDE, latitudes);
        contentValues.put(WorkoutSessionContentProvider.Contract.LONGITUDE, longitudes);
        contentValues.put(WorkoutSessionContentProvider.Contract.WORKOUT_TYPE, workoutType.getworkoutTypeID());

        getContentResolver().insert(WorkoutSessionContentProvider.Contract.SESSION_URI, contentValues);
    }

    /**
     * Converts the list of coordinates i.e. longitudes, to a string separated by a semi-colon
     * ready for storing in the database
     * @param coordinates   The coordinates to store
     * @return
     */
    private String parseCoordinates(List<Double> coordinates){
        StringBuilder stringBuilder = new StringBuilder();
        for(Double coordinate : coordinates){
            stringBuilder.append(coordinate);
            stringBuilder.append(";");
        }

        return stringBuilder.toString();
    }

    /**
     * The following methods notify registered listeners of updates regarding the ongoing workout session
     */
    private void notifySessionListenersDurationUpdate(long sessionCurrentDurationMilliseconds){
        for(IWorkoutSessionListener timeProgressedListener : sessionListeners){
            timeProgressedListener.onDurationUpdated(sessionCurrentDurationMilliseconds);
        }
    }

    private void notifySessionListenersDistanceUpdate(int sessionDistance){
        for(IWorkoutSessionListener timeProgressedListener : sessionListeners){
            timeProgressedListener.onDistanceUpdated(sessionDistance);
        }
    }

    private void notifySessionListenersSpeedUpdate(double sessionSpeed){
        for(IWorkoutSessionListener timeProgressedListener : sessionListeners){
            timeProgressedListener.onPaceUpdated(sessionSpeed);
        }
    }

    private void notifySessionListenersLocationUpdate(double latitude, double longitude){
        for(IWorkoutSessionListener timeProgressedListener : sessionListeners){
            timeProgressedListener.onLocationUpdated(latitude, longitude);
        }
    }

    /**
     * Monitors the duration of the ongoing session and updates registered session listeners of
     * duration changes
     */
    class SessionDurationHandler extends Thread implements Runnable{
        private long previousTimeMilliseconds;
        private long currentTimeMilliseconds;

        // SystemClock.uptimeMillis() is used instead of System.currentTimeMillis() as with
        // the latter, the time may jump backwards or forwards unpredictably. See link for info:
        // https://developer.android.com/reference/android/os/SystemClock.html#uptimeMillis()
        SessionDurationHandler(){
            previousTimeMilliseconds = SystemClock.uptimeMillis();

            date = Calendar.getInstance().get(Calendar.DATE);
            month = Calendar.getInstance().get(Calendar.MONTH);
            year = Calendar.getInstance().get(Calendar.YEAR);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("mm", Locale.ENGLISH);
            minute = Integer.parseInt(simpleDateFormat.format(Calendar.getInstance().getTime()));
            simpleDateFormat = new SimpleDateFormat("HH", Locale.ENGLISH);
            hour = Integer.parseInt(simpleDateFormat.format(Calendar.getInstance().getTime()));
            start();
        }

        /**
         * The notification must only be updated if the session is still running. In order to avoid
         * a race condition where the sessionRunning value is set to false after being checked but
         * before calling updateNotification, both these statements must be ran atomically using
         * synchronise
         */
        @Override
        public void run() {
            while(sessionInProgress.get()){
                try{
                    Thread.sleep(UPDATE_MILLISECONDS);

                    currentTimeMilliseconds = SystemClock.uptimeMillis();

                    if(sessionRunning){
                        sessionCurrentDurationMilliseconds += currentTimeMilliseconds - previousTimeMilliseconds;
                        notifySessionListenersDurationUpdate(sessionCurrentDurationMilliseconds);
                    }

                    previousTimeMilliseconds = currentTimeMilliseconds;

                    synchronized (this){
                        if(sessionInProgress.get()){
                            WorkoutSessionService.this.updateNotification();
                        }
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                    return;
                }
            }
        }

        long getCurrentDurationMilliseconds(){
            return sessionCurrentDurationMilliseconds;
        }
    }

    /**
     * Provides access to the functionality of the session service
     */
    public class SessionBinder extends Binder{
        public WorkoutSessionService getSessionService(){
            return WorkoutSessionService.this;
        }
    }

    /**
     * Used to keep updated about the user's current location, and use that information to calculate
     * the distance travelled and their current pace
     */
    public class SessionLocationListener implements LocationListener {
        private Location lastLocation;
        private long lastTimeStamp;
        private long timeStamp;
        private double distanceToNewLocation;

        @Override
        public void onLocationChanged(Location location) {
            timeStamp = SystemClock.uptimeMillis();

            // While a session is running, update the stored distance travelled and the user's
            // current pace
            // Need to check if a lastLocation has been stored first
            if(lastLocation != null && sessionRunning){
                distanceToNewLocation = Math.round(lastLocation.distanceTo(location));
                distance += distanceToNewLocation;
                pace = distanceToNewLocation / ((double) (timeStamp - lastTimeStamp) / 1000.0);

                notifySessionListenersDistanceUpdate(distance);
                notifySessionListenersSpeedUpdate(pace);

                longitudes.add(lastLocation.getLongitude());
                latitudes.add(lastLocation.getLatitude());

                notifySessionListenersLocationUpdate(location.getLatitude(), location.getLongitude());
            }
            lastLocation = location;
            lastTimeStamp = timeStamp;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}