package com.mdp.cw4.runningtracker.activities;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.PolylineOptions;
import com.mdp.cw4.runningtracker.R;
import com.mdp.cw4.runningtracker.ValueFormatter;
import com.mdp.cw4.runningtracker.WorkoutSessionContentProvider;

import java.util.Arrays;

import static com.mdp.cw4.runningtracker.activities.WorkoutSessionHistory.SESSION_INTENT_ID;

public class ViewWorkoutSession extends AppCompatActivity implements OnMapReadyCallback{

    private TextView distanceTravelled;
    private TextView sessionDuration;
    private TextView averageSpeed;
    private TextView completeDate;
    private ImageView workoutTypeImage;
    private double[] latitudes;
    private double[] longitudes;
    private long sessionId;
    private int sessionTypeIcons[] = { R.drawable.runner, R.drawable.walking, R.drawable.cyclist };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_workout_session);

        distanceTravelled = (TextView) findViewById(R.id.distanceTravelled);
        sessionDuration = (TextView) findViewById(R.id.sessionDuration);
        averageSpeed = (TextView) findViewById(R.id.averageSpeed);
        completeDate = (TextView) findViewById(R.id.date);

        workoutTypeImage = (ImageView) findViewById(R.id.workoutTypeImage);

        long sessionId = getIntent().getLongExtra(SESSION_INTENT_ID, -1);
        getSessionDetails(sessionId);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    /**
     * Inflate and setup the menu options
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.view_session_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.delete:
                createDeleteDialog().show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Draws the route taken for the displayed session on a Google Map and zooms into the starting point
     * If there are
     */
    @Override
    public void onMapReady(final GoogleMap googleMap) {
        // If there is no route to show, the latitudes and longitudes will be null so just return.
        // Also, return if there are a different number of latitude coordinates to longitudes
        if(latitudes == null || longitudes == null || latitudes.length != longitudes.length){
            return;
        }

        LatLng[] latLng = new LatLng[latitudes.length];
        final LatLngBounds.Builder latLngBounds = new LatLngBounds.Builder();

        // Add each latitude and longitude coordinate to the map
        for(int i = 0; i < latitudes.length; i++){
            latLng[i] = new LatLng(latitudes[i], longitudes[i]);
            latLngBounds.include(latLng[i]);
        }

        googleMap.addPolyline(new PolylineOptions()
                .color(getColor(R.color.colorAccent))
                .add(latLng));

        googleMap.setMaxZoomPreference(19);

        // Used as padding for the map to show a small region around the route the user took
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        final int padding = Math.min(metrics.widthPixels, metrics.heightPixels) / 16;

        // Need to wait for the map view to actually be loaded before moving the camera and adding
        // the new LatLng bounds
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                setInitialCameraLocation(googleMap, latLngBounds.build(), padding);
            }
        });
    }

    /**
     * Move the Google map camera and adjust the bounds so that it displays the whole root
     * @param googleMap         GoogleMap object to move around
     * @param latLngBounds      Bounds of the route to zoom and move to
     * @param padding           Amount of area to show around the route taken
     */
    private void setInitialCameraLocation(GoogleMap googleMap, LatLngBounds latLngBounds, int padding){
        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, padding));
    }

    /**
     * Retrieves the details of the workout session corresponding to the sessionID
     * @param sessionId     id as stored in the database of the workout session to retrieve
     *                      information on
     */
    private void getSessionDetails(long sessionId){
        final int MILLISECONDS_PER_SECOND = 1000;

        this.sessionId = sessionId;

        String[] columns = new String[]{
                WorkoutSessionContentProvider.Contract._ID,
                WorkoutSessionContentProvider.Contract.DATE,
                WorkoutSessionContentProvider.Contract.MONTH,
                WorkoutSessionContentProvider.Contract.YEAR,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION,
                WorkoutSessionContentProvider.Contract.LONGITUDE,
                WorkoutSessionContentProvider.Contract.LATITUDE,
                WorkoutSessionContentProvider.Contract.HOUR,
                WorkoutSessionContentProvider.Contract.MINUTE,
                WorkoutSessionContentProvider.Contract.WORKOUT_TYPE
        };

        Uri uri = ContentUris.withAppendedId(WorkoutSessionContentProvider.Contract.SESSION_URI, sessionId);

        Cursor cursor = getContentResolver().query(uri, columns, null, null, null);

        if(cursor != null && cursor.moveToNext()){
            int distanceMetres = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.DISTANCE));
            int durationMilliseconds = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.DURATION));
            int date = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.DATE));
            int month = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MONTH));
            int year = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.YEAR));
            int hour = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.HOUR));
            int minute = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MINUTE));
            int workoutType = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.WORKOUT_TYPE));
            String allLatitudes = cursor.getString(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.LATITUDE));
            String allLongitudes = cursor.getString(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.LONGITUDE));

            double metresPerSecond = 0;
            int seconds = durationMilliseconds / MILLISECONDS_PER_SECOND;
            if(seconds > 0){
                metresPerSecond = (double) distanceMetres / (double) seconds;
            }

            distanceTravelled.setText(ValueFormatter.formatDistance(distanceMetres));
            sessionDuration.setText(ValueFormatter.formatDuration(durationMilliseconds));
            averageSpeed.setText(ValueFormatter.formatAverageSpeed(metresPerSecond));
            String fullDateAndTime = ValueFormatter.formatCompleteDate(year, month, date) + " at "
                    + ValueFormatter.formatTime(hour, minute);
            completeDate.setText(fullDateAndTime);
            workoutTypeImage.setImageResource(Math.max(0, sessionTypeIcons[workoutType]));
            latitudes = splitCoordinates(allLatitudes);
            longitudes = splitCoordinates(allLongitudes);
        }

        if(cursor != null){
            cursor.close();
        }
    }

    /**
     * Dialog used to confirm that the user would like to delete the session from the database
     * @return      The dialog to display to the user
     */
    private AlertDialog createDeleteDialog(){
        AlertDialog.Builder deleteDialogBuilder = new AlertDialog.Builder(this);

        deleteDialogBuilder.setTitle("Warning");

        deleteDialogBuilder.setMessage("Would you like to delete this session?");

        deleteDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                deleteSession();
                dialogInterface.dismiss();
                finish();
            }
        });

        deleteDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        return deleteDialogBuilder.create();
    }


    /**
     * Deletes the session from the database
     */
    private void deleteSession(){
        Uri uri = ContentUris.withAppendedId(WorkoutSessionContentProvider.Contract.SESSION_URI, sessionId);

        getContentResolver().delete(uri, null, null);
    }

    private double[] splitCoordinates(String allCoordinates){
        if("".equals(allCoordinates)){
            return null;
        }

        String[] splitCoordinates = allCoordinates.split(";");
        double[] coordinates = new double[splitCoordinates.length];

        for(int i = 0; i < splitCoordinates.length; i++){
            coordinates[i] = Double.parseDouble(splitCoordinates[i]);
        }

        return coordinates;
    }
}
