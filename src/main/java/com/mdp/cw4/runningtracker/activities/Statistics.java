package com.mdp.cw4.runningtracker.activities;

import android.app.DatePickerDialog;
import android.database.Cursor;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.mdp.cw4.runningtracker.R;
import com.mdp.cw4.runningtracker.ValueFormatter;
import com.mdp.cw4.runningtracker.WorkoutSessionContentProvider;
import com.mdp.cw4.runningtracker.WorkoutType;
import com.mdp.cw4.runningtracker.Views.YearlySummaryView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Provides the user with statistics about their previous activity sessions based.
 * It allows the user to go to a particular year, month and date as well as allows them to filter
 * by activity session type
 */
public class Statistics extends AppCompatActivity {
    private int selectedDate;
    private int selectedMonth;
    private int selectedYear;

    private TextView distanceTravelledSelectedMonth;
    private TextView sessionDurationSelectedMonth;
    private TextView paceSelectedMonth;
    private TextView distanceTravelledSelectedDay;
    private TextView sessionDurationSelectedDay;
    private TextView paceSelectedDay;
    private TextView selectedYearHeader;
    private TextView selectedMonthHeader;
    private TextView selectedDateHeader;
    private TextView majorGridLineValue;

    private CheckBox runningCheckBox;
    private CheckBox walkingCheckBox;
    private CheckBox cyclingCheckBox;

    private Spinner graphMetricDropdown;

    private LinearLayout yearlySummaryContainer;
    private YearlySummaryView yearlySummaryView;
    private ConstraintLayout statisticsContainer;

    private static final String BUNDLE_SELECTED_DATE_KEY = "com.mdp.cw4.runningtracker.selectedDate";
    private static final String BUNDLE_SELECTED_MONTH_KEY = "com.mdp.cw4.runningtracker.selectedMonth";
    private static final String BUNDLE_SELECTED_YEAR_KEY = "com.mdp.cw4.runningtracker.selectedYear";
    private static final int METRIC_DROPDOWN_DISTANCE_POSITION = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_statistics);
        yearlySummaryView = new YearlySummaryView(Statistics.this);

        setupViews(savedInstanceState);
        refreshDateHeaderText();
        refreshSummary();

        statisticsContainer.getViewTreeObserver().addOnGlobalLayoutListener(statisticsLayoutObserver);
    }

    /**
     * If the activity is destroyed and recreated, the date selected by the user should still be kept
     * @param bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle bundle){
        super.onSaveInstanceState(bundle);
        bundle.putInt(BUNDLE_SELECTED_DATE_KEY, selectedDate);
        bundle.putInt(BUNDLE_SELECTED_MONTH_KEY, selectedMonth);
        bundle.putInt(BUNDLE_SELECTED_YEAR_KEY, selectedYear);
    }

    /**
     * Initialise the views
     * @param savedInstanceState    If the activity has been recreated, then use the date previously
     *                              selected by the user
     */
    private void setupViews(Bundle savedInstanceState) {
        distanceTravelledSelectedMonth = (TextView) findViewById(R.id.distanceTravelledSelectedMonth);
        sessionDurationSelectedMonth = (TextView) findViewById(R.id.sessionDurationSelectedMonth);
        paceSelectedMonth = (TextView) findViewById(R.id.averageSpeedSelectedMonth);

        distanceTravelledSelectedDay = (TextView) findViewById(R.id.distanceTravelledSelectedDay);
        sessionDurationSelectedDay = (TextView) findViewById(R.id.sessionDurationSelectedDay);
        paceSelectedDay = (TextView) findViewById(R.id.averageSpeedSelectedDay);

        selectedDateHeader = (TextView) findViewById(R.id.selectedDate);
        selectedMonthHeader = (TextView) findViewById(R.id.selectedMonth);
        selectedYearHeader = (TextView) findViewById(R.id.selectedYear);

        majorGridLineValue = (TextView) findViewById(R.id.majorGridLineValue);

        runningCheckBox = (CheckBox) findViewById(R.id.runningCheckbox);
        walkingCheckBox = (CheckBox) findViewById(R.id.walkingCheckbox);
        cyclingCheckBox = (CheckBox) findViewById(R.id.walkingCheckBox);

        graphMetricDropdown = (Spinner) findViewById(R.id.graphDependentVariableDropdown);

        yearlySummaryContainer = (LinearLayout) findViewById(R.id.yearlySummaryContainer);
        statisticsContainer = (ConstraintLayout) findViewById(R.id.statisticsContainer);

        if (savedInstanceState != null) {
            selectedDate = savedInstanceState.getInt(BUNDLE_SELECTED_DATE_KEY);
            selectedMonth = savedInstanceState.getInt(BUNDLE_SELECTED_MONTH_KEY);
            selectedYear = savedInstanceState.getInt(BUNDLE_SELECTED_YEAR_KEY);
        } else {
            selectedDate = Calendar.getInstance().get(Calendar.DATE);
            selectedMonth = Calendar.getInstance().get(Calendar.MONTH);
            selectedYear = Calendar.getInstance().get(Calendar.YEAR);
        }
    }

    /**
     * The series lines in the yearly graph need to be informed of the maximum height they can draw
     * to. This cannot be calculated until the statisticsContainer has been inflated as the size
     * of the yearly view is dependent on the size of this container.
     * Once this information has been gathered, then draw the yearly graph and assign listeners
     * to the user controls
     */
    private ViewTreeObserver.OnGlobalLayoutListener statisticsLayoutObserver =
        new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                yearlySummaryView.init(yearlySummaryContainer.getMeasuredWidth(), yearlySummaryContainer.getMeasuredHeight());
                yearlySummaryContainer.addView(yearlySummaryView);
                yearlySummaryView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                updateYearlyGraph();

                graphMetricDropdown.setOnItemSelectedListener(graphMetricDropdownListener);
                runningCheckBox.setOnCheckedChangeListener(sessionTypeCheckedListener);
                walkingCheckBox.setOnCheckedChangeListener(sessionTypeCheckedListener);
                cyclingCheckBox.setOnCheckedChangeListener(sessionTypeCheckedListener);
            }
        };

    /**
     * Listens for when the user selects a different metric to be displayed in the graph
     * i.e distance or duration. If this changes, then the yearly graph needs to be updated
     */
    private AdapterView.OnItemSelectedListener graphMetricDropdownListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
            if(yearlySummaryView != null){
                updateYearlyGraph();
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) { }
    };

    /**
     * If the user changes which workout types to display, both the yearly graph and summaries need
     * to be updated
     */
    private CompoundButton.OnCheckedChangeListener sessionTypeCheckedListener =
            new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            updateYearlyGraph();
            refreshSummary();
        }
    };

    private void updateYearlyGraph(){
        yearlySummaryView.resetPaths();

        // Get the maximum value across all months in the selected year. For example, if distance is
        // selected, then find the month with the maximum distance covered and display that distance
        double maxMonthlyTotal = getMaxMonthlyTotal();
        updateMaximumValueIndicator(maxMonthlyTotal);

        // If maxMonthlyTotal is less than or equal to zero, then there are no corresponding sessions
        // for that year so we do not need to do anything
        if(maxMonthlyTotal > 0){
            float[] seriesLineXOffsetMultipliers = calculateSeriesLineXOffsetMultipliers();
            int seriesDrawn = 0;

            // For each workout type, check whether the user wants it to be displayed and if so
            // draw a series line for that workout type. For example, if distance and cycling are
            // selected, draw a series line depicting the distance travelled for each month through
            // cycling
            if(runningCheckBox.isChecked()){
                yearlySummaryView.calculateYearlySummary(selectedYear,
                        WorkoutType.RUNNING,
                        seriesLineXOffsetMultipliers[seriesDrawn], maxMonthlyTotal,
                        graphMetricDropdown.getSelectedItemPosition());
                seriesDrawn++;
            }

            if(walkingCheckBox.isChecked()){
                yearlySummaryView.calculateYearlySummary(selectedYear,
                        WorkoutType.WALKING,
                        seriesLineXOffsetMultipliers[seriesDrawn], maxMonthlyTotal,
                        graphMetricDropdown.getSelectedItemPosition());
                seriesDrawn++;
            }

            if(cyclingCheckBox.isChecked()){
                yearlySummaryView.calculateYearlySummary(selectedYear,
                        WorkoutType.CYCLING,
                        seriesLineXOffsetMultipliers[seriesDrawn], maxMonthlyTotal,
                        graphMetricDropdown.getSelectedItemPosition());
            }
        }

        yearlySummaryView.invalidate();

    }

    /**
     * The position of the series lines depends on how many workout types have been selected
     * If only one is selected, then the line should be central. If two or more then they should
     * share the allocated space for each month.
     * This works by a multiplier, where the values in the array correspond to each selected workout
     * and multiplied by the width of the series line to provide an offset when drawn
     * @return  An array of integers of length N, where N is the number of workout types the user
     *          wishes to be displayed
     */
    private float[] calculateSeriesLineXOffsetMultipliers(){
        int numberOfSelectedSessionTypes = calculateNumberOfSelectedSessionTypes();

        if(numberOfSelectedSessionTypes == 1){
            return new float[] { 0 };
        }else if(numberOfSelectedSessionTypes == 2){
            return new float[] { -1, 1 };
        }else{
            return new float[] { -1.5f, 0, 1.5f };
        }
    }

    /**
     * Calculates the number of workout types the user wishes to be displayed
     * @return  Number of selected workout types
     */
    private int calculateNumberOfSelectedSessionTypes() {
        int running = runningCheckBox.isChecked() ? 1 : 0;
        int walking = walkingCheckBox.isChecked() ? 1 : 0;
        int cycling = cyclingCheckBox.isChecked() ? 1 : 0;
        return running + walking + cycling;
    }

    /**
     * Finds the maximum total selected value across all months. For example, if duration has been
     * selected as the required metric to display, then find the maximum duration for all workout
     * sessions in a month across all months
     * @return      The maximum monthly total for the desired metric across all months in the selected
     *              year
     */
    private double getMaxMonthlyTotal() {
        int maxMonthlyTotal = 0;
        Uri uri;

        if (!anyWorkoutSessionTypesSelected()) {
            return maxMonthlyTotal;
        }

        // Use the uri corresponding to whether the distance metric has been selected or the duration
        if(graphMetricDropdown.getSelectedItemPosition() == METRIC_DROPDOWN_DISTANCE_POSITION){
            uri = WorkoutSessionContentProvider.Contract.MAX_TOTAL_DISTANCE_FOR_YEAR_URI;
        }else{
            uri = WorkoutSessionContentProvider.Contract.MAX_TOTAL_DURATION_FOR_YEAR_URI;
        }

        // Add the selected year as a selection argument
        ArrayList<String> selectionArguments = new ArrayList<>();
        selectionArguments.add(String.valueOf(selectedYear));

        // Add any required workout selection arguments
        String[] finalSelectionArguments = addSelectedActivitiesArguments(selectionArguments);

        Cursor cursor = getContentResolver().query(uri, null, null,
                finalSelectionArguments, null);

        if(cursor == null) {
            return maxMonthlyTotal;
        }

        if (cursor.moveToNext()){
            maxMonthlyTotal = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MONTHLY_TOTAL));
        }

        cursor.close();

        return maxMonthlyTotal;
    }

    /**
     * For each selected workout type, this function adds the id of the relevant workout type
     * to the list of selectionArguments
     * @param selectionArguments    selectionArguments to add to
     * @return                      list of selection arguments with any required arguments added
     */
    private String[] addSelectedActivitiesArguments(List<String> selectionArguments){

        if(runningCheckBox.isChecked()){
            selectionArguments.add(String.valueOf(WorkoutType.RUNNING.getworkoutTypeID()));
        }

        if(walkingCheckBox.isChecked()){
            selectionArguments.add(String.valueOf(WorkoutType.WALKING.getworkoutTypeID()));
        }

        if(cyclingCheckBox.isChecked()){
            selectionArguments.add(String.valueOf(WorkoutType.CYCLING.getworkoutTypeID()));
        }

        return selectionArguments.toArray(new String[selectionArguments.size()]);
    }

    private boolean anyWorkoutSessionTypesSelected(){
        return runningCheckBox.isChecked() || cyclingCheckBox.isChecked() || walkingCheckBox.isChecked();
    }

    /**
     * Displays a calendar allowing the user to select which day, month and year they wish to see
     * information on
     */
    public void openCalendar(View view) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, onDateSetListener,
                selectedYear, selectedMonth, selectedDate);

        datePickerDialog.show();
    }

    /**
     * Refreshes the monthly and daily summaries which show the total distance, duration and pace
     * for the currently selected month and day respectively
     */
    private void refreshSummary(){
        populateSummary(distanceTravelledSelectedDay, sessionDurationSelectedDay, paceSelectedDay,
                getDaySummary(selectedYear, selectedMonth, selectedDate));
        populateSummary(distanceTravelledSelectedMonth, sessionDurationSelectedMonth, paceSelectedMonth,
                getMonthSummary(selectedYear, selectedMonth));
    }

    /**
     * Updates the headers to display the selected date
     */
    private void refreshDateHeaderText(){
        selectedYearHeader.setText(ValueFormatter.formatYear(selectedYear));
        selectedMonthHeader.setText(ValueFormatter.formatMonth(selectedMonth));
        selectedDateHeader.setText(String.valueOf(ValueFormatter.formatDateOfMonth(selectedDate)));
    }

    /**
     * Populates the views which display the distance, duration and pace with their corresponding
     * value based on the values inside the cursor passed in
     * @param cursor    Cursor object containing the list of sessions carried out for a paritcular
     *                  range in time i.e a day or month
     */
    private void populateSummary(TextView distanceTextView, TextView durationTextView, TextView paceTextView,
                                 Cursor cursor){
        final int MILLISECONDS_PER_SECOND = 1000;

        int metresTravelled = 0;
        int sessionDurationMilliseconds = 0;
        double averagePace = 0;

        // Sum the total distance and duration across all sessions in the cursor object
        if(cursor != null && cursor.moveToFirst()) {
            do {
                metresTravelled += cursor.getInt(cursor.getColumnIndexOrThrow(
                        WorkoutSessionContentProvider.Contract.DISTANCE));
                sessionDurationMilliseconds += cursor.getInt(cursor.getColumnIndexOrThrow(
                        WorkoutSessionContentProvider.Contract.DURATION));
            } while (cursor.moveToNext());
        }

        int seconds = sessionDurationMilliseconds / MILLISECONDS_PER_SECOND;

        // Calculate the average pace of the user across all sessions
        if(seconds > 0){
            averagePace = (double) metresTravelled / (double) seconds;
        }

        // Update views with their corresponding values
        distanceTextView.setText(ValueFormatter.formatDistance(metresTravelled));
        durationTextView.setText(ValueFormatter.formatDuration(sessionDurationMilliseconds));
        paceTextView.setText(ValueFormatter.formatAverageSpeed(averagePace));

        if(cursor != null){
            cursor.close();
        }
    }

    /**
     * Gets a cursor object containing sessions which correspond to the date selected by the user
     * and the workout types they wish to take into account
     * @param year      Current year selected
     * @param month     Current month selected
     * @param day       Current day selected
     * @return          Cursor object containing all the relevant sessions based on the user's selections
     */
    public Cursor getDaySummary(int year, int month, int day){
        String[] columns = new String[]{
                WorkoutSessionContentProvider.Contract._ID,
                WorkoutSessionContentProvider.Contract.DATE,
                WorkoutSessionContentProvider.Contract.MONTH,
                WorkoutSessionContentProvider.Contract.YEAR,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION,
        };

        // Populate the preliminary selection arguments
        List<String> selectionArguments = new ArrayList<>();
        selectionArguments.add(String.valueOf(day));
        selectionArguments.add(String.valueOf(month));
        selectionArguments.add(String.valueOf(year));

        // Add any required selection arguments and the corresponding where clause for the types of
        // activities selected by the user
        String[] selectedActivitiesArguments = addSelectedActivitiesArguments(selectionArguments);
        String selectedActivitiesWhereClause = parseSessionWhereClause(selectedActivitiesArguments);

        return getContentResolver().query(WorkoutSessionContentProvider.Contract.SESSION_URI, columns,
                WorkoutSessionContentProvider.Contract.DATE + " = ? AND " +
                WorkoutSessionContentProvider.Contract.MONTH + " = ? AND " +
                WorkoutSessionContentProvider.Contract.YEAR + " = ? " + selectedActivitiesWhereClause,
                selectionArguments.toArray(new String[selectionArguments.size()]), null);
    }

    /**
     * Gets a cursor object containing sessions which correspond to the month and year selected by
     * the user and the workout types they wish to take into account
     * @param year      Current year selected
     * @param month     Current month selected
     * @return          Cursor object containing all the relevant sessions based on the user's selections
     */
    public Cursor getMonthSummary(int year, int month){
        String[] columns = new String[]{
                WorkoutSessionContentProvider.Contract._ID,
                WorkoutSessionContentProvider.Contract.DATE,
                WorkoutSessionContentProvider.Contract.MONTH,
                WorkoutSessionContentProvider.Contract.YEAR,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION,
        };

        // Populate the preliminary selection arguments
        List<String> selectionArguments = new ArrayList<>();
        selectionArguments.add(String.valueOf(month));
        selectionArguments.add(String.valueOf(year));

        // Add any required selection arguments and the corresponding where clause for the types of
        // activities selected by the user
        String[] selectedActivitiesArguments = addSelectedActivitiesArguments(selectionArguments);
        String selectedActivitiesWhereClause = parseSessionWhereClause(selectedActivitiesArguments);

        return getContentResolver().query(WorkoutSessionContentProvider.Contract.SESSION_URI,
                columns, WorkoutSessionContentProvider.Contract.MONTH + " = ? AND " +
                         WorkoutSessionContentProvider.Contract.YEAR + " = ? " + selectedActivitiesWhereClause,
                selectionArguments.toArray(new String[selectionArguments.size()]), null);
    }

    /**
     * Parse the selection where clause to account for the number of workout arguments there are
     * @param selectionArgs     selection arguments
     * @return                  String containing the selection clause
     */
    private String parseSessionWhereClause(String[] selectionArgs){
        String additionalWhereClause = "";

        // If there are no selection arguments, just return the empty string
        if(selectionArgs.length < 1){
            return additionalWhereClause;
        }

        // Add the first clause
        additionalWhereClause += "AND (" + WorkoutSessionContentProvider.Contract.WORKOUT_TYPE + " = ? ";

        // Add any additional clauses
        for(int i = 2; i < selectionArgs.length; i++){
            additionalWhereClause += "OR " + WorkoutSessionContentProvider.Contract.WORKOUT_TYPE + " = ? ";
        }

        additionalWhereClause += ")";

        return additionalWhereClause;
    }

    /**
     * Listens for when the user selects a different date and updates the graph, date headers and summary
     * if this occurs
     */
    private DatePickerDialog.OnDateSetListener onDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker datePicker, int year, int month, int date) {
            selectedYear = year;
            selectedMonth = month;
            selectedDate = date;

            updateYearlyGraph();

            refreshDateHeaderText();
            refreshSummary();
        }
    };

    /**
     * Updates the value of the maximum value indicator to display the largest value achieved for the
     * selected metric across all months
     * @param maxValue  The value to assign to the view
     */
    private void updateMaximumValueIndicator(double maxValue){
        if(graphMetricDropdown.getSelectedItemPosition() == 0){
            majorGridLineValue.setText(ValueFormatter.formatDistance((int) maxValue));
        }else{
            majorGridLineValue.setText(ValueFormatter.formatDuration((int) maxValue));
        }
    }
}