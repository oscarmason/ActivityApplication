package com.mdp.cw4.runningtracker.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.mdp.cw4.runningtracker.IWorkoutSessionListener;
import com.mdp.cw4.runningtracker.R;
import com.mdp.cw4.runningtracker.ValueFormatter;
import com.mdp.cw4.runningtracker.WorkoutSessionService;
import com.mdp.cw4.runningtracker.WorkoutType;

/**
 * <h1>Running Tracker</h1>
 * Workout session is an activity which provides functionality to track a new activity session and
 * store the completed activity session.
 * Furthermore, it allows the user to view their current progress during an ongoing session in
 * regard to duration, distance and pace, and displays the user's current position on a map
 */
public class RunningTracker extends AppCompatActivity implements IWorkoutSessionListener, OnMapReadyCallback {

    private TextView duration;
    private TextView distanceTravelled;
    private TextView pace;
    private Button startRunning;
    private Button enableGPSButton;
    private Button stopButton;
    private Button pauseResumeButton;
    private Spinner workoutTypeDropdown;
    private LinearLayout workoutTypeDropdownContainer;

    private WorkoutType workoutType;
    private WorkoutSessionService workoutSessionService;
    private GoogleMap googleMap;
    private GPSBroadcastReceiver gpsBroadcastReceiver;
    private ServiceConnection sessionServiceConnection;

    private static final int ACCESS_FINE_LOCATION_PERMISSION = 11;
    public static final String INTENT_WORKOUT_TYPE = "com.mdp.cw4.runningtracker.workoutType";
    public static final String INTENT_STARTED_FROM_SERVICE = "com.mdp.cw4.runningtracker.startedFromService";

    // Different states the action buttons can be in depending on factors related to the session
    // and GPS. For example, if GPS is disable, then then a button to enable GPS can be displayed
    private enum ActionButtonState {
        SESSION_NOT_IN_PROGRESS,
        SESSION_GPS_DISABLED,
        SESSION_IN_PROGRESS_PAUSED,
        SESSION_IN_PROGRESS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running_tracker);
        setupViews();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gpsBroadcastReceiver = new GPSBroadcastReceiver();
        registerReceiver(gpsBroadcastReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));

        switchActionButtons(interpretActionButtonState(false, false));

        sessionServiceConnection = serviceConnection;
        // 0 flag causes this activity to bind to the service if and only if it already exists i.e.
        // If the application has been swiped away and reopened via the notification bar
        bindService(new Intent(this, WorkoutSessionService.class), sessionServiceConnection, 0);
    }

    /**
     * Need to unbind from the service if the activity is destroyed as well as unregister any
     * listeners
     */
    @Override
    protected void onDestroy() {
        if (sessionServiceConnection != null) {
            unbindService(sessionServiceConnection);
            sessionServiceConnection = null;
        }

        if(workoutSessionService != null){
            workoutSessionService.removeSessionListener(this);
        }

        if(gpsBroadcastReceiver != null){
            unregisterReceiver(gpsBroadcastReceiver);
        }

        super.onDestroy();
    }

    /**
     * Add the buttons to the menu in the action bar
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.session_menu, menu);
        return true;
    }

    /**
     * Setup intents for the menu bar buttons
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.statistics:
                startActivity(new Intent(this, Statistics.class));
                return true;
            case R.id.sessions:
                startActivity(new Intent(this, WorkoutSessionHistory.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Initialises the views
     */
    private void setupViews(){
        duration = (TextView) findViewById(R.id.sessionDuration);
        distanceTravelled = (TextView) findViewById(R.id.distanceTravelled);
        pace = (TextView) findViewById(R.id.pace);

        startRunning = (Button) findViewById(R.id.start);
        stopButton = (Button) findViewById(R.id.stop);
        pauseResumeButton = (Button) findViewById(R.id.pause);
        enableGPSButton = (Button) findViewById(R.id.enableGPS);

        workoutTypeDropdown = (Spinner) findViewById(R.id.workoutDropdown);
        workoutTypeDropdownContainer = (LinearLayout) findViewById(R.id.workoutDropdownContainer);

        resetWorkoutCounters();

        // Setup activity type dropdown menu
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.workoutTypes, R.layout.dropdown_style_dark_block);
        adapter.setDropDownViewResource(R.layout.dropdown_style_light);

        workoutTypeDropdown.setSelection(0);
        workoutTypeDropdown.setAdapter(adapter);
        workoutTypeDropdown.getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * Starts a new workout session. First it checks the workout type based on the one selected
     * by the user with the workout dropdown
     * @param view  View used to initiate a new workout session
     */
    public void startNewWorkoutSession(View view){

        switch(workoutTypeDropdown.getSelectedItemPosition()){
            case 0:
                workoutType = WorkoutType.RUNNING;
                break;
            case 1:
                workoutType = WorkoutType.WALKING;
                break;
            case 2:
                workoutType = WorkoutType.CYCLING;
                break;
        }

        resetWorkoutCounters();

        // If permission has not been granted then request it, otherwise start and bind to a session
        // service which is responsible for monitoring the progress of the workout session
        int requestPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        if(requestPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION);
        }else{

            if(GPSEnabled()){

                Intent intent = new Intent(this, WorkoutSessionService.class);
                intent.putExtra(INTENT_WORKOUT_TYPE, workoutType);
                startService(intent);
                sessionServiceConnection = serviceConnection;
                bindService(intent, sessionServiceConnection, BIND_AUTO_CREATE);

                switchActionButtons(ActionButtonState.SESSION_IN_PROGRESS);

                if(googleMap != null){
                    onMapReady(googleMap);
                }
            }
        }
    }

    /**
     * Request the user for permission to use the location based services
     * If permission is granted and GPS is enabled then prepare the map, otherwise show a dialog
     * which informs the user why the permission is required
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults){
        switch (requestCode) {
            case ACCESS_FINE_LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && GPSEnabled() && googleMap != null) {
                    onMapReady(googleMap);
                }else{
                    createPermissionDialog().show();
                }
            }
        }
    }

    /**
     * Sets the workout counters to zero
     */
    private void resetWorkoutCounters(){
        distanceTravelled.setText(ValueFormatter.formatDistance(0));
        duration.setText(ValueFormatter.formatDuration(0));
        pace.setText(ValueFormatter.formatAverageSpeed(0));
    }

    /**
     * Checks whether GPS is enabled
     * @return  True if GPS is enabled, false otherwise
     */
    private boolean GPSEnabled(){
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return (locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
    }

    /**
     * Creates a Dialog which informs the user why the location permission is required
     */
    private AlertDialog createPermissionDialog(){
        AlertDialog.Builder permissionDialogBuilder = new AlertDialog.Builder(this);
        permissionDialogBuilder.setTitle("Permission Required");
        permissionDialogBuilder.setMessage("Location permission is required to start a new workout session");

        permissionDialogBuilder.setPositiveButton("Okay", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        return permissionDialogBuilder.create();
    }

    /**
     * Uses the status of the GPS, whether an workout session is ongoing, and if so whether it is
     * paused or not do decipher which state the action buttons should be in
     * @param sessionInProgress     Whether an workout session is currently ongoin
     * @param sessionRunning        Whether the session is running or paused
     * @return                      The current action button state
     */
    private ActionButtonState interpretActionButtonState(boolean sessionInProgress, boolean sessionRunning){
        if(!GPSEnabled()) {
            return ActionButtonState.SESSION_GPS_DISABLED;
        }

        if(!sessionInProgress) {
            return ActionButtonState.SESSION_NOT_IN_PROGRESS;
        }

        if(sessionRunning){
            return ActionButtonState.SESSION_IN_PROGRESS;
        }

        return ActionButtonState.SESSION_IN_PROGRESS_PAUSED;
    }

    /**
     * Controls the hiding and displaying of the different action buttons depending on the current
     * action button state
     * For example, if GPS is disable, the user cannot record a new workout session, therefore the
     * start button should be hidden whilst an 'enable GPS' button should be displayed
     * @param actionButtonState     Current action button state
     */
    private void switchActionButtons(ActionButtonState actionButtonState){

        startRunning.setVisibility(View.GONE);
        stopButton.setVisibility(View.GONE);
        enableGPSButton.setVisibility(View.GONE);
        pauseResumeButton.setVisibility(View.GONE);
        workoutTypeDropdownContainer.setVisibility(View.GONE);

        switch (actionButtonState){
            case SESSION_GPS_DISABLED:
                enableGPSButton.setVisibility(View.VISIBLE);
                break;
            case SESSION_IN_PROGRESS:
                pauseResumeButton.setText(getString(R.string.pause));
                stopButton.setVisibility(View.VISIBLE);
                pauseResumeButton.setVisibility(View.VISIBLE);
                break;
            case SESSION_IN_PROGRESS_PAUSED:
                pauseResumeButton.setText(getString(R.string.resume));
                stopButton.setVisibility(View.VISIBLE);
                pauseResumeButton.setVisibility(View.VISIBLE);
                break;
            default:
                startRunning.setVisibility(View.VISIBLE);
                workoutTypeDropdownContainer.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Responsible for setting up the map
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        final float mapZoomLevel = 17;

        int requestPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);

        // Need to check and request if necessary the location permission in order to use this feature
        if(requestPermission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_PERMISSION);
        }else{

            // Get the last know location if available. If not, the location will be updated on the first
            // location update by WorkoutSessionService
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if(locationManager != null){
                Location GPSLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                if(GPSLocation != null){
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(GPSLocation.getLatitude(), GPSLocation.getLongitude()), mapZoomLevel));
                }else if(networkLocation != null){
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(networkLocation.getLatitude(), networkLocation.getLongitude()), mapZoomLevel));
                }
            }
            googleMap.setMyLocationEnabled(true);
        }

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    /**
     * This method is called from a background thread, therefore the update of the text needs to be passed
     * back to the UI thread
     * @param durationMilliseconds     The length of time the user has been carrying out the ongoing
     *                                 workout
     */
    @Override
    public void onDurationUpdated(final long durationMilliseconds) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                duration.setText(ValueFormatter.formatDuration(durationMilliseconds));
            }
        });
    }

    /**
     * If the distance the user has travelled has changed, update the distance travelled counter
     * @param distance  Current distance travelled
     */
    @Override
    public void onDistanceUpdated(int distance) {
        distanceTravelled.setText(ValueFormatter.formatDistance(distance));
    }

    /**
     * If the current pace of the user has changed, update the pace counter
     * @param pace  Current pace of the user
     */
    @Override
    public void onPaceUpdated(double pace) {
        this.pace.setText(ValueFormatter.formatAverageSpeed(pace));
    }

    /**
     * If the user's location has changed, move the map camera to keep them centred on the map
     * @param latitude      Current latitude
     * @param longitude     Current longitude
     */
    @Override
    public void onLocationUpdated(double latitude, double longitude) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
    }

    public void pauseResumeSession(View view){
        if(workoutSessionService.isSessionRunning()){
            workoutSessionService.pauseSession();
            pauseResumeButton.setText("Resume");
        }else{
            workoutSessionService.resumeSession();
            pauseResumeButton.setText("Pause");
        }
    }

    /**
     * If the user stops an workout session, unbind from the session service, store the workout
     * session and change the action buttons so that the user can start another if they wish
     */
    public void stopSession(View view){
        if(sessionServiceConnection != null){
            unbindService(sessionServiceConnection);
            workoutSessionService.stopSession();
            workoutSessionService.addSessionToDatabase();
            workoutSessionService.stopForeground(true);
            workoutSessionService.stopSelf();
            sessionServiceConnection = null;
            Toast.makeText(this, "Session stored", Toast.LENGTH_LONG).show();
        }

        switchActionButtons(ActionButtonState.SESSION_NOT_IN_PROGRESS);
    }

    /**
     * Upon binding to the WorkoutSessionService, register this workout so as a sessionListener so that
     * it can receive regular updates such as the duration and distance travelled
     */
    public ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            workoutSessionService = ((WorkoutSessionService.SessionBinder) iBinder).getSessionService();
            workoutSessionService.addSessionListener(RunningTracker.this);
            switchActionButtons(interpretActionButtonState(
                    workoutSessionService.isSessionInProgress(), workoutSessionService.isSessionRunning()));
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            workoutSessionService = null;
        }
    };

    /**
     * Inform the user if GPS gets disabled
     */
    private AlertDialog createGPSDisabledDialog(){
        final AlertDialog.Builder GPSDisabledDialogBuilder = new AlertDialog.Builder(this);
        GPSDisabledDialogBuilder.setTitle("GPS Location Required");
        GPSDisabledDialogBuilder.setMessage("GPS must be enabled to add a new session.\nEnable now?");

        GPSDisabledDialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                openLocationSettings();
            }
        });

        GPSDisabledDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        return GPSDisabledDialogBuilder.create();
    }

    /**
     * Required for the enable GPS button
     */
    public void openLocationSettings(View view){
        openLocationSettings();
    }

    /**
     * Opens the devices location settings
     */
    private void openLocationSettings(){
        Intent intent = new Intent(
                android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    /**
     * Broadcast receiver used to listen for when GPS gets enabled or disabled.
     * If GPS gets disabled, then the user should be informed and the action controls should be
     * updated to account for this (an enable GPS button should be displayed)
     */
    public class GPSBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {

                if(!GPSEnabled()){
                    createGPSDisabledDialog().show();
                    if(workoutSessionService != null){
                        workoutSessionService.pauseSession();
                    }
                }

                if(workoutSessionService != null){
                    switchActionButtons(interpretActionButtonState(workoutSessionService.isSessionInProgress(),
                            workoutSessionService.isSessionRunning()));
                }else{
                    switchActionButtons(interpretActionButtonState(false, false));
                }
            }
        }
    }
}
