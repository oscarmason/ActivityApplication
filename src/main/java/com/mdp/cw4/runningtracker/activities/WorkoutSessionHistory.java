package com.mdp.cw4.runningtracker.activities;

import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.mdp.cw4.runningtracker.R;
import com.mdp.cw4.runningtracker.ValueFormatter;
import com.mdp.cw4.runningtracker.WorkoutSessionContentProvider;
import com.mdp.cw4.runningtracker.WorkoutType;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

/**
 * <h1>Session History</h1>
 * Session history is an activity responsible for displaying a list of previous workout sessions
 * the user has completed and allows the user to: filter those sessions by date and
 * workout type; and sort by date, distance and session duration
 */
public class WorkoutSessionHistory extends AppCompatActivity {

    private SessionContentObserver sessionContentObserver;
    private ListView sessionList;
    private Spinner dateDropdown;
    private Spinner monthDropdown;
    private Spinner yearDropdown;
    private Spinner sortByDropdown;
    private Spinner sortByDirectionDropdown;
    private CheckBox runningCheckbox;
    private CheckBox walkingCheckbox;
    private CheckBox cyclingCheckbox;
    private ConstraintLayout filterOptionsInnerContainer;
    private Button openCloseFilterOptionsButton;
    private boolean filterOptionsVisible = true;

    private int sessionTypeIcons[] = { R.drawable.runner, R.drawable.walking, R.drawable.cyclist };

    public static final String SESSION_INTENT_ID = "com.mdp.cw4.runningtracker.sessionintentid";
    private static final int NO_FILTER_POSITION = 0;
    private static final int DESCENDING = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_session_history);

        sessionContentObserver = new SessionContentObserver(new Handler());
        getContentResolver().registerContentObserver(WorkoutSessionContentProvider.Contract.SESSION_URI,
                true, sessionContentObserver);

        setupViews();
        populateListOfWorkoutSessions();
        setupFilterControls();
        toggleFilterOptions();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        getContentResolver().unregisterContentObserver(sessionContentObserver);
    }

    @Override
    public void onBackPressed() {
        if(filterOptionsVisible){
            toggleFilterOptions();
        }else{
            super.onBackPressed();
        }
    }

    /**
     * Initialises the views and sets their required listeners where relevant
     */
    private void setupViews(){
        filterOptionsInnerContainer = (ConstraintLayout) findViewById(R.id.filterOptionsInnerContainer);

        dateDropdown = (Spinner) findViewById(R.id.dateDropdown);
        monthDropdown = (Spinner) findViewById(R.id.monthDropdown);
        yearDropdown = (Spinner) findViewById(R.id.yearDropdown);
        sortByDropdown = (Spinner) findViewById(R.id.sortBy);
        sortByDirectionDropdown = (Spinner) findViewById(R.id.sortByDirection);
        sortByDropdown.setSelection(0);
        sortByDirectionDropdown.setSelection(0);

        openCloseFilterOptionsButton = (Button) findViewById(R.id.openCloseFilterOptionsButton);
        openCloseFilterOptionsButton.setOnClickListener(openCloseFilterOptionsClickListener);

        runningCheckbox = (CheckBox) findViewById(R.id.runningCheckbox);
        walkingCheckbox = (CheckBox) findViewById(R.id.walkingCheckBox);
        cyclingCheckbox = (CheckBox) findViewById(R.id.cyclingCheckBox);

        sessionList = (ListView) findViewById(R.id.sessionList);

        runningCheckbox.setOnCheckedChangeListener(checkedChangeListener);
        walkingCheckbox.setOnCheckedChangeListener(checkedChangeListener);
        cyclingCheckbox.setOnCheckedChangeListener(checkedChangeListener);
    }

    /**
     * Delegates the opening and closing of the filter options
     */
    private View.OnClickListener openCloseFilterOptionsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            toggleFilterOptions();
        }
    };

    /**
     * Controls the opening and closing of the filter options
     */
    private void toggleFilterOptions(){
        filterOptionsVisible = !filterOptionsVisible;

        if(filterOptionsVisible){
            filterOptionsInnerContainer.setVisibility(View.VISIBLE);
        }else{
            filterOptionsInnerContainer.setVisibility(View.GONE);
        }

        openCloseFilterOptionsButton.setText(filterOptionsVisible
                ? getString(R.string.closeFilterOptions) : getString(R.string.openFilterOptions));
    }

    /**
     * Populates the filter drop down menus with their required values and sets their listeners
     */
    private void setupFilterControls(){

        populateDropDown(sortByDropdown, R.array.sortByOptions);
        populateDropDown(sortByDirectionDropdown, R.array.sortByDirection);
        populateDropDown(monthDropdown, R.array.months);
        populateYearDropDown();
        populateDateDropdown();

        dateDropdown.setOnItemSelectedListener(dropdownListener);
        monthDropdown.setOnItemSelectedListener(dropdownListener);
        yearDropdown.setOnItemSelectedListener(dropdownListener);
        sortByDropdown.setOnItemSelectedListener(dropdownListener);
        sortByDirectionDropdown.setOnItemSelectedListener(dropdownListener);
    }

    /**
     * Populates a dropdown with an array of strings and sets their style
     * @param dropdown          Dropdown to be populated
     * @param arrayResourceId   ResourceId to the array of strings
     */
    private void populateDropDown(Spinner dropdown, int arrayResourceId){
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                arrayResourceId, R.layout.dropdown_style_light);
        adapter.setDropDownViewResource(R.layout.dropdown_style_light);

        dropdown.setSelection(NO_FILTER_POSITION);
        dropdown.setAdapter(adapter);
    }


    /**
     * Populates the year dropdown with years ranging from the earliest in the database to the
     * current year
     */
    private void populateYearDropDown(){
        Cursor cursor = getContentResolver().query(WorkoutSessionContentProvider.Contract.START_YEAR_URI,
                null, null, null, null);

        if(cursor == null){
            populateDropDown(yearDropdown, R.array.years);
            return;
        }

        int minimumYear = 0;
        if(cursor.moveToNext()) {
            minimumYear = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MINIMUM_YEAR));
        }

        if(minimumYear == 0){
            populateDropDown(yearDropdown, R.array.years);

        }else {
            // Get the current year and calculate the number of years to put in dropdown
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            int numberOfYears = currentYear - minimumYear + 1;

            // Need an extra position for the "all dates" option
            String[] years = new String[numberOfYears + 1];
            years[0] = "Year";

            // Add all the years to the list
            for (int i = 0; i < numberOfYears; i++) {
                years[i + 1] = String.valueOf(currentYear - i);
            }

            // Populate and style the dropdown
            ArrayAdapter<String> adapter = new ArrayAdapter<>(WorkoutSessionHistory.this,
                    R.layout.dropdown_style_light, years);
            adapter.setDropDownViewResource(R.layout.dropdown_style_light);
            yearDropdown.setAdapter(adapter);
            yearDropdown.setSelection(NO_FILTER_POSITION);
        }
        cursor.close();
    }

    /**
     * The date drown down needs to be populated programmatically as opposed to loading directly
     * from an array of strings because the number of dates to show depends on the month and year
     * selected
     */
    private void populateDateDropdown(){
        // Get the number of days in the selected month
        int year;
        if(yearDropdown.getSelectedItemPosition() == 0){
            year = Calendar.getInstance().get(Calendar.YEAR);
        }else{
            try{
                year = Integer.parseInt(yearDropdown.getSelectedItem().toString());
            }catch(NumberFormatException e){
                year = Calendar.getInstance().get(Calendar.YEAR);
            }
        }

        Calendar calendar = new GregorianCalendar(year,
                monthDropdown.getSelectedItemPosition() - 1, 1);
        int numberOfDaysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Before repopulating we want to store the previously selected date so it can be
        // reselected
        int currentDateSelected = dateDropdown.getSelectedItemPosition();

        // Need an extra position for the "all dates" option
        String[] dates = new String[numberOfDaysInMonth + 1];

        dates[0] = "Date";

        // Add the days to the list
        for (int d = 1; d <= numberOfDaysInMonth; d++){
            dates[d] = String.valueOf(d);
        }

        // Populate and style the list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(WorkoutSessionHistory.this,
                R.layout.dropdown_style_light, dates);
        adapter.setDropDownViewResource(R.layout.dropdown_style_light);
        dateDropdown.setAdapter(adapter);

        // If the number of days in the month has decreased, for example, if the user has gone from
        // May to April, we need to account for this. In such scenarios the last day of the month
        // will be selected
        currentDateSelected = Math.min(currentDateSelected, numberOfDaysInMonth);
        dateDropdown.setSelection(currentDateSelected);
    }

    /**
     * Listens when the user changes the status of a checked box and delegates the task to the
     * relevant function
     */
    private CompoundButton.OnCheckedChangeListener checkedChangeListener =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    populateListOfWorkoutSessions();
                }
            };

    /**
     * Listens when the user changes the status of a checked box and delegates the task of
     * repopulating the list of workout sessions to apply the changes
     *
     * Note that if the selection change has come from a dropdown menu which is not the date, the
     * the number of dates for the selected month and year may have changes. Therefore, the date
     * dropdown gets repopulated
     */
    private AdapterView.OnItemSelectedListener dropdownListener =
            new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                    if(adapterView.getId() != R.id.dateDropdown){
                        populateDateDropdown();
                    }
                    populateListOfWorkoutSessions();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            };

    /**
     * Populates the list view with details about past workout sessions the user has completed
     * If no activities are selected i.e. their checkboxes are not checked, then there is nothing
     * to add to the list so simply return.
     */
    private void populateListOfWorkoutSessions(){

        if(!runningCheckbox.isChecked() && !walkingCheckbox.isChecked() && !cyclingCheckbox.isChecked()) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, android.R.id.text1, new String[]{});

            sessionList.setAdapter(adapter);
            return;
        }

        Cursor cursor = getWorkoutSessionsFromContentProvider();
        if(cursor == null){
            return;
        }

        String[] columnsToDisplay = new String[]{
                WorkoutSessionContentProvider.Contract.DATE,
                WorkoutSessionContentProvider.Contract.MONTH,
                WorkoutSessionContentProvider.Contract.YEAR,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION,
                WorkoutSessionContentProvider.Contract.WORKOUT_TYPE,
                WorkoutSessionContentProvider.Contract.HOUR,
                WorkoutSessionContentProvider.Contract.MINUTE,
                WorkoutSessionContentProvider.Contract._ID,
        };

        final SimpleCursorAdapter simpleCursorAdapter = new SimpleCursorAdapter(this, R.layout.single_session_entry,
                cursor, columnsToDisplay,
                new int[] { R.id.date, R.id.month, R.id.year, R.id.distance, R.id.duration,
                        R.id.sessionTypeImage, R.id.hour, R.id.minute }, 0);

        simpleCursorAdapter.setViewBinder(workoutSessionViewBinder);

        sessionList.setAdapter(simpleCursorAdapter);

        sessionList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(WorkoutSessionHistory.this, ViewWorkoutSession.class);
                intent.putExtra(SESSION_INTENT_ID, sessionList.getAdapter().getItemId(i));
                startActivity(intent);
            }
        });
    }

    /**
     * Assigns the incoming values from the cursor to their relevant View in the entry
     */
    private SimpleCursorAdapter.ViewBinder workoutSessionViewBinder = new SimpleCursorAdapter.ViewBinder() {
        final int DATE_COLUMN_INDEX = 1;
        final int MONTH_COLUMN_INDEX = 2;
        final int DISTANCE_COLUMN_INDEX = 4;
        final int DURATION_COLUMN_INDEX = 5;
        final int SESSION_MODE_COLUMN_INDEX = 6;
        final int HOUR_COLUMN_INDEX = 7;
        final int MINUTE_COLUMN_INDEX = 8;

        /**
         *
         * @param view              View within the list entry
         * @param cursor            Cursor object containing the values return from the database
         * @param columnNumber      Index of the column within the cursor object.
         *                          This is set upon the initial query
         */
        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnNumber) {

            int value = cursor.getInt(columnNumber);
            switch (columnNumber){
                case DATE_COLUMN_INDEX:
                    TextView textView = (TextView) view;
                    textView.setText(ValueFormatter.formatDateOfMonth(value));
                    return true;
                case MONTH_COLUMN_INDEX:
                    textView = (TextView) view;
                    textView.setText(ValueFormatter.formatMonth(value));
                    return true;
                case DISTANCE_COLUMN_INDEX:
                    textView = (TextView) view;
                    textView.setText(ValueFormatter.formatDistance(value));
                    return true;
                case DURATION_COLUMN_INDEX:
                    textView = (TextView) view;
                    textView.setText(ValueFormatter.formatDuration(value));
                    return true;
                case SESSION_MODE_COLUMN_INDEX:
                    ImageView imageView = (ImageView) view;
                    if(value < sessionTypeIcons.length){
                        imageView.setImageResource(sessionTypeIcons[value]);
                    }
                    return true;
                case HOUR_COLUMN_INDEX:
                    textView = (TextView) view;
                    textView.setText(String.format(Locale.ENGLISH, "%02d", value));
                    return true;
                case MINUTE_COLUMN_INDEX:
                    textView = (TextView) view;
                    String minute = ":" + String.format(Locale.ENGLISH, "%02d", value);
                    textView.setText(minute);
                    return true;

            }
            return false;
        }
    };

    public Cursor getWorkoutSessionsFromContentProvider() {
        // Predefined sort order used to easily create a query based on the selected positions of
        // the dropdown menus
        final String[] sortOrdersDesc = {
                WorkoutSessionContentProvider.Contract.YEAR + " DESC, "
                        + WorkoutSessionContentProvider.Contract.MONTH + " DESC, "
                        + WorkoutSessionContentProvider.Contract.DATE + " DESC, "
                        + WorkoutSessionContentProvider.Contract.HOUR + " DESC, "
                        + WorkoutSessionContentProvider.Contract.MINUTE + " DESC",
                WorkoutSessionContentProvider.Contract.DISTANCE + " DESC",
                WorkoutSessionContentProvider.Contract.DURATION + " DESC"
        };

        final String[] sortOrdersAsc = {
                WorkoutSessionContentProvider.Contract.YEAR + ", "
                        + WorkoutSessionContentProvider.Contract.MONTH + ", "
                        + WorkoutSessionContentProvider.Contract.DATE + ", "
                        + WorkoutSessionContentProvider.Contract.HOUR + ", "
                        + WorkoutSessionContentProvider.Contract.MINUTE,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION
        };

        String[] columns = new String[]{
                WorkoutSessionContentProvider.Contract._ID,
                WorkoutSessionContentProvider.Contract.DATE,
                WorkoutSessionContentProvider.Contract.MONTH,
                WorkoutSessionContentProvider.Contract.YEAR,
                WorkoutSessionContentProvider.Contract.DISTANCE,
                WorkoutSessionContentProvider.Contract.DURATION,
                WorkoutSessionContentProvider.Contract.WORKOUT_TYPE,
                WorkoutSessionContentProvider.Contract.HOUR,
                WorkoutSessionContentProvider.Contract.MINUTE
        };

        // Parse the queries for the date filters and types of activities selected
        List<String> selectionArguments = new ArrayList<>();
        String selection = parseDateSelections(selectionArguments);
        String workoutSelection = parseWorkoutSelection();
        selectionArguments = addWorkoutSelectionArgs(selectionArguments);

        // If no date selections were made, then the selection is just whatever activities are
        // selected
        // If date filters have been applied, the add the additional workout filters to the end
        if(selection == null){
            selection = workoutSelection;
        }else if(workoutSelection != null){
            selection += " AND (" + workoutSelection + ")";
        }

        // Get the relevant sort order based on the item position of the selected item in the menu
        String sortOrder;
        if(sortByDirectionDropdown.getSelectedItemPosition() == DESCENDING){
            sortOrder = sortOrdersDesc[Math.max(0, sortByDropdown.getSelectedItemPosition())];
        }else{
            sortOrder = sortOrdersAsc[Math.max(0, sortByDropdown.getSelectedItemPosition())];
        }

        return getContentResolver().query(WorkoutSessionContentProvider.Contract.SESSION_URI, columns,
                selection, selectionArguments.toArray(new String[selectionArguments.size()]), sortOrder);
    }

    /**
     * Prepares the selection query and arguments if the user filters by date
     * @param selectionArguments    List of selection arguments to add to
     * @return                      Selection query
     */
    private String parseDateSelections(List<String> selectionArguments){
        String selection = null;

        if(dateDropdown.getSelectedItemPosition() > 0){
            selectionArguments.add(String.valueOf(dateDropdown.getSelectedItemPosition()));
            selection = WorkoutSessionContentProvider.Contract.DATE + " = ? ";
        }

        if(monthDropdown.getSelectedItemPosition() > 0){
            // Need to subtract one because months start with zero in Java
            selectionArguments.add(String.valueOf(monthDropdown.getSelectedItemPosition() - 1));
            if(selection == null){
                selection = WorkoutSessionContentProvider.Contract.MONTH + " = ? ";
            }else{
                selection += " AND " + WorkoutSessionContentProvider.Contract.MONTH + " = ? ";
            }
        }

        if(yearDropdown.getSelectedItemPosition() > 0){
            selectionArguments.add(String.valueOf(yearDropdown.getSelectedItem()));
            if(selection == null){
                selection = WorkoutSessionContentProvider.Contract.YEAR + " = ? ";
            }else{
                selection += " AND " + WorkoutSessionContentProvider.Contract.YEAR + " = ? ";
            }
        }

        return selection;
    }

    /**
     * Prepares the selection query and arguments if the user filters by workout type
     * @return  Selection query
     */
    private String parseWorkoutSelection(){
        int numberOfSelectedWorkoutTypes = calculateNumberOfSelectedSessionTypes();

        // If no workout types have been selected then simple return null
        if(numberOfSelectedWorkoutTypes == 0){
            return null;
        }

        // There will be at least one selection
        String selection = WorkoutSessionContentProvider.Contract.WORKOUT_TYPE + " = ? ";

        // For every subsequent selection add another clause
        for(int i = 1; i < numberOfSelectedWorkoutTypes; i++){
            selection += "OR " + WorkoutSessionContentProvider.Contract.WORKOUT_TYPE + " = ? ";
        }

        return selection;
    }

    /**
     * Calculates the number of selected session types based on the checkboxes
     * @return  Number of selection session types
     */
    private int calculateNumberOfSelectedSessionTypes() {
        int running = runningCheckbox.isChecked() ? 1 : 0;
        int walking = walkingCheckbox.isChecked() ? 1 : 0;
        int cycling = cyclingCheckbox.isChecked() ? 1 : 0;
        return running + walking + cycling;
    }

    /**
     * Adds the relevant selection arguments to the list depending on whether the workout type has been
     * selected or not by the user
     * @param selectionArgs List of selection arguments to add to. This will be passed on to the
     *                      content provider when carrying out the query
     * @return              List of selection arguments
     */
    private List<String> addWorkoutSelectionArgs(List<String> selectionArgs){
        if(runningCheckbox.isChecked()){
            selectionArgs.add(String.valueOf(WorkoutType.RUNNING.getworkoutTypeID()));
        }

        if(walkingCheckbox.isChecked()){
            selectionArgs.add(String.valueOf(WorkoutType.WALKING.getworkoutTypeID()));
        }

        if(cyclingCheckbox.isChecked()){
            selectionArgs.add(String.valueOf(WorkoutType.CYCLING.getworkoutTypeID()));
        }

        return selectionArgs;
    }

    /**
     * <h1>Session Content Observer</h1>
     * Observes changes to the session database
     */
    class SessionContentObserver extends ContentObserver {

        SessionContentObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            this.onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            populateListOfWorkoutSessions();
        }
    }
}
