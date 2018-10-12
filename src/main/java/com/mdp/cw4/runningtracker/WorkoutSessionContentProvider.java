package com.mdp.cw4.runningtracker;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import static com.mdp.cw4.runningtracker.WorkoutSessionContentProvider.Contract.*;

public class WorkoutSessionContentProvider extends ContentProvider {

    private WorkoutSessionDatabase workoutSessionDatabase;
    private static final int VERSION_NUMBER = 5;
    public static final String WORKOUT_SESSION_DATABASE_NAME = "sessionDB";
    static final String SESSION_TABLE_NAME = "sessionTable";

    private static UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(Contract.AUTHORITY, WORKOUT_SESSION_DATABASE_NAME, 1);
        uriMatcher.addURI(Contract.AUTHORITY, WORKOUT_SESSION_DATABASE_NAME + "/#", 2);
        uriMatcher.addURI(Contract.AUTHORITY, MONTHLY_SUMMARY_DISTANCE, 3);
        uriMatcher.addURI(Contract.AUTHORITY, MONTHLY_SUMMARY_DURATION, 4);
        uriMatcher.addURI(Contract.AUTHORITY, MINIMUM_YEAR, 5);
        uriMatcher.addURI(Contract.AUTHORITY, MAX_TOTAL_DISTANCE_FOR_YEAR, 6);
        uriMatcher.addURI(Contract.AUTHORITY, MAX_TOTAL_DURATION_FOR_YEAR, 7);
    }

    @Override
    public boolean onCreate() {
        workoutSessionDatabase = new WorkoutSessionDatabase(getContext(), WORKOUT_SESSION_DATABASE_NAME, null, VERSION_NUMBER);
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = getReadableDatabase();

        if(database == null){
            return null;
        }

        switch (uriMatcher.match(uri)) {
            case 2:
                selection = "_ID = ?";
                selectionArgs = new String[] { uri.getLastPathSegment() };
            case 1:
                return database.query(SESSION_TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
            case 5:
                String query =
                    "SELECT MIN(" + Contract.YEAR + ") AS " + Contract.MINIMUM_YEAR +
                    " FROM " + SESSION_TABLE_NAME;
                return database.rawQuery(query, null);

            // Queries for returning the total amount for a variable i.e. distance, for each month
            case 3:
                query = parseQueryMaxTotalByMonthWithSessionType(Contract.DISTANCE, selectionArgs);
                return database.rawQuery(query, selectionArgs);

            case 4:
                query = parseQueryMaxTotalByMonthWithSessionType(Contract.DURATION, selectionArgs);
                return database.rawQuery(query, selectionArgs);

            // Queries for returning the maximum variable i.e. distance obtained during a month
            // over the whole year
            case 6:
                query = parseQueryMaxTotalForYearWithSessionType(Contract.DISTANCE, selectionArgs);
                return database.rawQuery(query, selectionArgs);
            case 7:
                query = parseQueryMaxTotalForYearWithSessionType(Contract.DURATION, selectionArgs);
                return database.rawQuery(query, selectionArgs);
            default:
                return null;
        }
    }

    /**
     * Parses a query to find the total amount achieved for a particular variable for each month
     * of the year, for example returning for each month how much the year have travelled
     * @param dependentVariable     Variable to find the total of
     */
    private String parseQueryMaxTotalVariableByMonth(String dependentVariable){
        return "SELECT SUM(" + dependentVariable + ") AS '" + MONTHLY_TOTAL + "', " + Contract.MONTH +
                " FROM " + SESSION_TABLE_NAME +
                " WHERE " + Contract.YEAR + " = ? " +
                " GROUP BY " + Contract.MONTH + ";";
    }

    /**
     * Parses a query to find the total amount achieved for a particular variable for each month
     * of the year, for example returning for each month how much the year have travelled.
     * This includes an additional selection clause to filter by workout session type
     * @param dependentVariable     Variable to find the total of
     * @param selectionArgs         Workout session selection arguments
     */
    private String parseQueryMaxTotalByMonthWithSessionType(String dependentVariable,
                                                            String[] selectionArgs) {
        if(selectionArgs == null || selectionArgs.length <= 1){
            return parseQueryMaxTotalVariableByMonth(dependentVariable);
        }

        String additionalWhereClause = parseSessionWhereClause(selectionArgs);

        return "SELECT SUM(" + dependentVariable + ") AS '" + MONTHLY_TOTAL + "', " + Contract.MONTH +
                " FROM " + SESSION_TABLE_NAME +
                " WHERE " + Contract.YEAR + " = ? " + additionalWhereClause +
                " GROUP BY " + Contract.MONTH + ";";
    }

    /**
     * Parses a query to find the maximum value obtained for a particular variable i.e. distance or
     * duration during a month for the whole selected year
     * @param dependentVariable     Variable to find the maximum of
     */
    private String parseQueryMaxTotalVariableForYear(String dependentVariable){
        return "SELECT MAX(" + MONTHLY_TOTAL + ") AS " + MONTHLY_TOTAL + " FROM (" +
                "SELECT SUM(" + dependentVariable + ") AS '" + MONTHLY_TOTAL + "'" +
                " FROM " + SESSION_TABLE_NAME +
                " WHERE " + Contract.YEAR + " = ? " +
                " GROUP BY " + Contract.MONTH + ", " + Contract.WORKOUT_TYPE + ");";
    }

    /**
     * Parses a query to find the maximum value obtained for a particular variable i.e. distance or
     * duration during a month for the whole selected year.
     * This includes an additional selection clause to filter by workout session type
     * @param dependentVariable     Variable to find the maximum of
     * @param selectionArgs         Workout session selection arguments
     */
    private String parseQueryMaxTotalForYearWithSessionType(String dependentVariable, String[] selectionArgs){
        if(selectionArgs == null || selectionArgs.length <= 1){
            return parseQueryMaxTotalVariableForYear(dependentVariable);
        }

        String additionalWhereClause = parseSessionWhereClause(selectionArgs);

        return "SELECT MAX(" + MONTHLY_TOTAL + ") AS " + MONTHLY_TOTAL + " FROM (" +
                "SELECT SUM(" + dependentVariable + ") AS '" + MONTHLY_TOTAL + "'" +
                " FROM " + SESSION_TABLE_NAME +
                " WHERE " + Contract.YEAR + " = ? " + additionalWhereClause +
                " GROUP BY " + Contract.MONTH + ", " + Contract.WORKOUT_TYPE + ");";
    }

    /**
     * Generates a selection clause to account for each workout type selection argument
     * @param selectionArgs     Workout session selection arguments
     */
    private String parseSessionWhereClause(String[] selectionArgs){
        String additionalWhereClause = "";

        if(selectionArgs.length <= 1){
            return additionalWhereClause;
        }

        additionalWhereClause += "AND (" + Contract.WORKOUT_TYPE + " = ? ";

        for(int i = 2; i < selectionArgs.length; i++){
            additionalWhereClause += "OR " + Contract.WORKOUT_TYPE + " = ? ";
        }

        additionalWhereClause += ")";

        return additionalWhereClause;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        SQLiteDatabase database = getWritableDatabase();

        if(database == null){
            return null;
        }

        long id;
        switch (uriMatcher.match(uri)){
            case 1:
            default:
                id = database.insert(SESSION_TABLE_NAME, null, contentValues);
                break;
        }

        Uri newUri = ContentUris.withAppendedId(uri, id);

        getContext().getContentResolver().notifyChange(newUri, null);

        database.close();

        return newUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = getWritableDatabase();
        int rowsDeleted = 0;

        if(database == null){
            return -1;
        }

        try {
            switch (uriMatcher.match(uri)) {
                case 2:
                    selection = "_ID = ?";
                    selectionArgs = new String[] { uri.getLastPathSegment() };
                case 1:
                    rowsDeleted = database.delete(SESSION_TABLE_NAME, selection, selectionArgs);
                    getContext().getContentResolver().notifyChange(uri, null);
                default:
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        database.close();

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    /**
     * Wrapper function to handle catch potential exceptions that can occur when opening a database
     * @return If an exception is not thrown, the function will return the database, otherwise it
     *         will return null
     */
    private SQLiteDatabase getWritableDatabase(){
        SQLiteDatabase database = null;
        try{
            database = workoutSessionDatabase.getWritableDatabase();
        }catch(SQLiteException e){
            e.printStackTrace();
        }

        return database;
    }

    /**
     * Wrapper function to handle catch potential exceptions that can occur when opening a database
     * @return If an exception is not thrown, the function will return the database, otherwise it
     *         will return null
     */
    private SQLiteDatabase getReadableDatabase(){
        SQLiteDatabase database = null;
        try{
            database = workoutSessionDatabase.getReadableDatabase();
        }catch(SQLiteException e){
            e.printStackTrace();
        }

        return database;
    }

    public static class Contract {
        public static final String AUTHORITY = "com.mdp.cw4.runningtracker.WorkoutSessionContentProvider";

        public static final String MINIMUM_YEAR = "startYear";
        public static final String MONTHLY_SUMMARY_DISTANCE = "yearlySummaryDistance";
        public static final String MONTHLY_SUMMARY_DURATION = "yearlySummaryDuration";

        public static final String _ID = "_id";
        public static final String DISTANCE = "distance";
        public static final String DATE = "date";
        public static final String MONTHLY_TOTAL = "monthlyTotal";
        public static final String MAX_TOTAL_DISTANCE_FOR_YEAR = "maxTotalDistanceForYear";
        public static final String MAX_TOTAL_DURATION_FOR_YEAR = "maxTotalDurationForYear";
        public static final String MONTH = "month";
        public static final String YEAR = "year";
        public static final String HOUR = "hour";
        public static final String MINUTE = "minute";
        public static final String DURATION = "time";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String WORKOUT_TYPE = "sessionMode";

        public static final Uri SESSION_URI = Uri.parse("content://" + AUTHORITY + "/" + WORKOUT_SESSION_DATABASE_NAME);
        public static final Uri MONTHLY_SUMMARY_DISTANCE_URI = Uri.parse("content://" + AUTHORITY + "/" + MONTHLY_SUMMARY_DISTANCE);
        public static final Uri MONTHLY_SUMMARY_DURATION_URI = Uri.parse("content://" + AUTHORITY + "/" + MONTHLY_SUMMARY_DURATION);
        public static final Uri START_YEAR_URI = Uri.parse("content://" + AUTHORITY + "/" + MINIMUM_YEAR);
        public static final Uri MAX_TOTAL_DISTANCE_FOR_YEAR_URI = Uri.parse("content://" + AUTHORITY + "/" + MAX_TOTAL_DISTANCE_FOR_YEAR);
        public static final Uri MAX_TOTAL_DURATION_FOR_YEAR_URI = Uri.parse("content://" + AUTHORITY + "/" + MAX_TOTAL_DURATION_FOR_YEAR);
    }
}
