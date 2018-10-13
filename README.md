# Activity Tracker
Activity tracker is an Android application that allows users to track, store and view their workout sessions.

This application was developed during my undergraduate degree as part of the coursework for a module in mobile device programming.

## Tasks
The application consists of four main components as outline below

### Workout Session
From this activity, users can start a new workout session.
![Alt text](/src/newSession.png?raw=true "Workout Session")
LEFT: Users can select the type of workout session e.g. running, cycling or walking, from the drop down at the bottom of the app. They can also view information related to previous workout sessions using either the statistics view or list view.
CENTRE: During the workout, duration, distance and pace are updated regularly.
RIGHT: An 'Enable GPS' button is automatically displayed and the session is paused if GPS is disabled

### Statistics
The statistics activity provides the user with an overall insight of previous workout sessions completed during the selected year, month and day. The graph has the option of displaying either the total distance travelled or the total duration of sessions for each month of the selected year. Additionally, the total distance, duration, and average speed for the selected month and day are also provided. The user can easily select a particular date from any month and year using the button at the bottom of the app.

![Alt text](/src/statistics.png?raw=true "Statistics")

LEFT: The graph can show either the total distance travelled or duration of all sessions for each month of the year separated by the type of workout. This activity also displays the distance, duration and average pace for both the selected month and date. CENTRE: Series lines automatically scale up or down to make use of the whole graph when the user selects/deselects different workout types. Additionally, the month and daily counters are updated. RIGHT: Users can switch between distance and duration using the drop down as well as select any year, month and date.

### Previous Workout Sessions

The Session History activity allows the user to access information on completed workout sessions.
Filter options are also provided which allow the user to:
• View sessions that took place on a particular day or during a particular month/year
• View only a certain type of workout session such as running
• Sort the workout session by date, distance or duration
These filter options allow the user to carry out useful queries such as finding the longest distance they have cycled during a specific month.
Finally, the user can click on any session to display additional information such as the route plotted on the map as shown below.

![Alt text](/src/previousSessions.png?raw=true "Session History")

LEFT: List of all previous workout sessions. CENTRE: Users can filter the list of workout sessions by day, month, year and workout typem as well as sort by date, distance and duration in either ascending or descending order. RIGHT: ViewWorkoutSession allows users to view the route they took for the selected workout session
