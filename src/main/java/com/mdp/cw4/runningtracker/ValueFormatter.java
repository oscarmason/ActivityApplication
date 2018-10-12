package com.mdp.cw4.runningtracker;

import java.util.Locale;

/**
 * <h1>Value Formatter</h1>
 * Value Formatter is used to keep a consistent format of values throughout the application; it
 * assigns the format displayed to users for values such as time and distance
 */
public class ValueFormatter {

    public static String formatDuration(long durationMilliseconds){

        if(durationMilliseconds >= 0) {
            final int MILLISECONDS_IN_HOUR = 3600000;
            final int MILLISECONDS_IN_MINUTE = 60000;
            final int MILLISECONDS_IN_SECOND = 1000;
            final int SECONDS_IN_MINUTE = 60;
            final int MINUTES_IN_HOUR = 60;

            int hours = (int) (durationMilliseconds / MILLISECONDS_IN_HOUR);
            int minutes = (int) (durationMilliseconds / MILLISECONDS_IN_MINUTE % MINUTES_IN_HOUR);
            int seconds = (int) (durationMilliseconds / MILLISECONDS_IN_SECOND % SECONDS_IN_MINUTE);

            return String.format(Locale.ENGLISH, "%02d", hours) + ":"
                    + String.format(Locale.ENGLISH, "%02d", minutes)
                    + ":" + String.format(Locale.ENGLISH, "%02d", seconds);
        }

        return "-- sec";
    }

    public static String formatDistance(int distanceMetres){
        final float METRES_IN_KILOMETRE = 1000;
        if(distanceMetres >= 0){
            return String.format(Locale.ENGLISH, "%.2f", distanceMetres/METRES_IN_KILOMETRE) + " km";
        }

        return "-- km";
    }

    public static String formatAverageSpeed(double metresPerSecond){
        if(metresPerSecond >= 0){
            return String.format(Locale.ENGLISH, "%.2f", metresPerSecond) + " m/s";
        }
        return "-- m/s";
    }

    public static String formatYear(int year){
        return String.valueOf(year);
    }

    /**
     * In keeping with the Java format, the months start at zero i.e. Jan = 0, Feb = 1 ... Dec = 11
     */
    public static String formatMonth(int month){
        final String[] months = new String[] { "January", "February", "March", "April", "May",
                "June", "July", "August", "September", "October", "November", "December" };
        return months[month];
    }

    /**
     * In keeping with the Java format, the months start at zero i.e. Jan = 0, Feb = 1 ... Dec = 11
     */
    public static String formatCompleteDate(int year, int month, int date){
        return date + " " + formatMonth(month) + " " + year;
    }

    public static String formatTime(int hour, int minute){
        return hour + ":" + minute;
    }

    public static String formatDateOfMonth(int date){
        String postfix;

        switch(date){
            case 1:
            case 21:
            case 31:
                postfix = "st";
                break;
            case 2:
            case 22:
                postfix = "nd";
                break;
            case 3:
            case 23:
                postfix = "rd";
                break;
            default:
                postfix = "th";
                break;
        }

        return date + postfix;
    }
}
