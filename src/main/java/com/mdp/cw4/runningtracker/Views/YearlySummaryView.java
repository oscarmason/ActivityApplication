package com.mdp.cw4.runningtracker.Views;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.net.Uri;
import android.view.View;

import com.mdp.cw4.runningtracker.R;
import com.mdp.cw4.runningtracker.WorkoutSessionContentProvider;
import com.mdp.cw4.runningtracker.WorkoutType;

import java.util.HashMap;
import java.util.Map;

public class YearlySummaryView extends View {
    private Path majorGridPath = new Path();
    private Path minorGridPath = new Path();
    private Path[] seriesPaths = new Path[3];

    private Paint majorGridLinePaint = new Paint();
    private Paint minorGridLinePaint = new Paint();
    private Paint[] seriesPaints = new Paint[3];

    private int monthWidth;
    private int seriesLineWidth;
    private int containerHeight;
    private int[] seriesColors = new int[] {
            getContext().getColor(R.color.running),
            getContext().getColor(R.color.walking),
            getContext().getColor(R.color.cycling) };

    private static final int NUMBER_OF_WORKOUT_TYPES = 3;
    private static final int DURATION = 0;

    public YearlySummaryView(Context context){
        super(context);
    }

    /**
     * Initialise the yearly graph
     * @param containerWidth    Width of the container, calculated at runtime once inflated,
     *                          which holds the graph
     * @param containerHeight   Height of the container, calculated at runtime once inflated,
     *                          which holds the graph
     */
    public void init(int containerWidth, int containerHeight){
        setYearlyGraphStyle(containerWidth, containerHeight);
    }

    /**
     * Setup the series paths and paint objects for each workout type
     * @param seriesLineWidth   Width of the series line
     */
    private void setupSeriesPathsAndPaints(int seriesLineWidth){
        for(int i = 0; i < NUMBER_OF_WORKOUT_TYPES; i++){
            seriesPaths[i] = new Path();
            seriesPaints[i] = new Paint();

            seriesPaints[i].setColor(seriesColors[i]);
            seriesPaints[i].setStyle(Paint.Style.STROKE);
            seriesPaints[i].setStrokeCap(Paint.Cap.ROUND);
            seriesPaints[i].setStrokeWidth(seriesLineWidth);
        }
    }

    /**
     * Set the style for the graph which displays the monthly maximum values achieved
     * @param containerWidth    Width of the container, calculated at runtime once inflated,
     *                          which holds the graph
     * @param containerHeight   Height of the container, calculated at runtime once inflated,
     *                          which holds the graph
     */
    private void setYearlyGraphStyle(int containerWidth, int containerHeight){
        this.containerHeight = containerHeight;
        int gridLineWidth;

        monthWidth = containerWidth / 12;
        seriesLineWidth = containerWidth / 40;
        gridLineWidth = Math.max(containerWidth / 360, 2);

        setupSeriesPathsAndPaints(seriesLineWidth);

        majorGridLinePaint.setColor(getContext().getColor(R.color.yearlySummaryMajorGridLine));
        majorGridLinePaint.setStyle(Paint.Style.STROKE);
        majorGridLinePaint.setStrokeWidth(gridLineWidth);

        minorGridLinePaint.setColor(getContext().getColor(R.color.yearlySummaryMinorGridLine));
        minorGridLinePaint.setStyle(Paint.Style.STROKE);
        minorGridLinePaint.setStrokeWidth(gridLineWidth);

        // Draw the graph's grid lines
        // NOTE: For each line, the width of the line needs to be accounted for to accurately position;
        // therefore we need to subtract half the width of the line

        // Draw a minor grid line half way down the graph
        minorGridPath.moveTo(0, containerHeight * 0.5f  - gridLineWidth * 0.5f);
        minorGridPath.lineTo(containerWidth, containerHeight * 0.5f - gridLineWidth * 0.5f);

        // Draw a major grid line at the top and bottom of the graph
        majorGridPath.moveTo(0, containerHeight   - gridLineWidth * 0.5f);
        majorGridPath.lineTo(containerWidth, containerHeight - gridLineWidth * 0.5f);
        majorGridPath.moveTo(0, gridLineWidth * 0.5f - gridLineWidth * 0.5f);
        majorGridPath.lineTo(containerWidth, gridLineWidth * 0.5f - gridLineWidth * 0.5f);
    }

    /**
     * Draw the yearly graph
     */
    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);
        canvas.drawPath(majorGridPath, majorGridLinePaint);
        canvas.drawPath(minorGridPath, minorGridLinePaint);
        for(int i = 0; i < NUMBER_OF_WORKOUT_TYPES; i++){
            canvas.drawPath(seriesPaths[i], seriesPaints[i]);
        }
    }

    /**
     * Calculate the maximum value achieved for the specified metric (either distance or duration)
     * for each month of the specified year
     * @param year                          Selected year
     * @param workoutType                   Workout type to retrieve information on
     * @param seriesStartXOffsetMultiplier  To improve styling, the position of the line will change
     *                                      depending on how many activities are currently being displayed.
     *                                      If one series is being displayed, the value should be 0 to
     *                                      centre that line. If two are displayed, -1 should be used for
     *                                      the series to be displayed on the left, and 1 for the right.
     *                                      If three are displayed, then -1.5, 0, and 1 should be used for
     *                                      the leftmost, center, and rightmost series being displayed.
     *
     * @param maxValue                      Maximum value retrieved for any month in the selected year
     *                                      across all activities currently selected
     * @param graphMetricDropdownPosition   Current metric selected i.e distance or duration
     */
    public void calculateYearlySummary(int year, WorkoutType workoutType, float seriesStartXOffsetMultiplier,
                                       double maxValue, int graphMetricDropdownPosition){
        Uri uri;
        Path path;

        // Get the path corresponding to the workout ID
        path = seriesPaths[workoutType.getworkoutTypeID()];

        if(graphMetricDropdownPosition == DURATION){
            uri = WorkoutSessionContentProvider.Contract.MONTHLY_SUMMARY_DISTANCE_URI;
        }else{
            uri = WorkoutSessionContentProvider.Contract.MONTHLY_SUMMARY_DURATION_URI;
        }

        // Clear the previously drawn series line
        path.reset();

        Cursor cursor = getContext().getContentResolver().query(uri, null, null,
                new String[]{ String.valueOf(year), String.valueOf(workoutType.getworkoutTypeID())}, null);

        if(cursor == null) {
            return;
        }

        HashMap<Integer, Integer> monthlyTotals = new HashMap<>();
        int months;
        int totals;

        // Get each monthly total returned by the cursor
        while (cursor.moveToNext()){
            months = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MONTH));
            totals = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutSessionContentProvider.Contract.MONTHLY_TOTAL));

            monthlyTotals.put(months, totals);
        }

        // Draw the series for each month
        if(monthlyTotals.size() != 0) {

            int month;
            int lineHeight;
            for (Map.Entry<Integer, Integer> monthTotal : monthlyTotals.entrySet()) {
                month = monthTotal.getKey();
                lineHeight = (int) ((double) (containerHeight - seriesLineWidth) / maxValue * monthTotal.getValue());

                // Because the round end of the path extends passed the start and end point, this
                // needs to be accounted for using: seriesLineWidth * 0.5f
                // NOTE: The month itself, which can take on values from 0-11, is used to position the
                // series line to its correct position
                path.moveTo(monthWidth * month + monthWidth * 0.5f + seriesStartXOffsetMultiplier * seriesLineWidth * 0.75f,
                        containerHeight - seriesLineWidth * 0.5f);
                path.lineTo(monthWidth * month + monthWidth * 0.5f + seriesStartXOffsetMultiplier * seriesLineWidth * 0.75f,
                        containerHeight - seriesLineWidth * 0.5f - lineHeight);
            }
        }

        cursor.close();
    }

    /**
     * Clear all series lines
     */
    public void resetPaths(){
        for(int i = 0; i < NUMBER_OF_WORKOUT_TYPES; i++){
            seriesPaths[i].reset();
        }
    }
}