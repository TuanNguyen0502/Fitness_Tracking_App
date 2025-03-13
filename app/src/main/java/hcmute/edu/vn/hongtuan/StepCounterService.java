package hcmute.edu.vn.hongtuan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.hongtuan.model.StepDatabaseHelper;
import hcmute.edu.vn.hongtuan.model.StepViewModel;

public class StepCounterService extends Service implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private NotificationManager notificationManager;
    private final String CHANNEL_ID = "step_channel";
    private StepDatabaseHelper stepDatabaseHelper;
    private static int stepCount, goal, height = 170;
    private static float distance, calories;
    private long lastUpdate = 0;
    private float lastX, lastY, lastZ;
    private static final float STEP_THRESHOLD = 2.0f;  // Vertical movement sensitivity
    private static final float MOVE_THRESHOLD = 2.0f;  // Forward movement sensitivity
    private static final float SWAY_THRESHOLD = 2.0f;  // Sideways movement limit
    private static final float WALKING_THRESHOLD = 1200f;  // Adjust based on testing
    private static final float RUNNING_THRESHOLD = 2500f;  // Running requires higher speed
    private static final float STEP_LENGTH = 0.2F; // the ratio between a person's stride length and their height is usually equal to 0.43
    private static final float CALORIES_RUNNING = 0.1F; // the average person burns 0.1 calories per step
    private static final float CALORIES_WALKING = 0.04F; // the average person burns 0.04 calories per step
    private static StepViewModel stepViewModel;


    @Override
    public void onCreate() {
        super.onCreate();
        stepDatabaseHelper = new StepDatabaseHelper(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();

        // Register the accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        // Check if the device has an accelerometer sensor
        if (stepSensor != null) {
            // Register the listener
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI);
        }

        // Start the service as a foreground service
        startForeground(1, createNotification("Steps: 0"));

        // Get data from database
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        stepCount = stepDatabaseHelper.getSteps(today);
        goal = stepDatabaseHelper.getGoal(today);
        distance = stepDatabaseHelper.getDistance(today);
        calories = stepDatabaseHelper.getCalories(today);
        updateLiveData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Ensures the service restarts if the system kills it
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // Check if the sensor type is accelerometer
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // current time in milliseconds
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 100) { // 100 milliseconds = 0.1 seconds
                long diffTime = curTime - lastUpdate; // time difference between now and last update
                lastUpdate = curTime;

                float x = sensorEvent.values[0]; // x-axis (forward and backward)
                float y = sensorEvent.values[1]; // y-axis (sideways)
                float z = sensorEvent.values[2]; // z-axis (vertical)
                float deltaX = Math.abs(x - lastX); // Forward movement change
                float deltaY = Math.abs(y - lastY); // Sideways movement change
                float deltaZ = Math.abs(z - lastZ); // Vertical movement change
                // Speed formula (scaled for better readability)
                float speed = (float) Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) / diffTime * 10000;

                // Detect step only if both Z-axis and X-axis movement are above threshold
                if (deltaZ > STEP_THRESHOLD && (deltaX > MOVE_THRESHOLD || deltaY > SWAY_THRESHOLD)) {
                    stepCount++;
                    // Calculate Distance (Assume user height is 1.7 meters)
                    distance = calculateDistance(stepCount, height);
                    // Calculate Calories Burned
                    if (speed > RUNNING_THRESHOLD) {
                        calories = calculateCalories(stepCount, true); // Running if speed is high
                    } else {
                        calories = calculateCalories(stepCount, false); // Walking if speed is moderate
                    }

                    updateDatabase();
                    updateLiveData();
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
            updateNotification("Steps: " + stepCount + " | Distance: " + distance + " km | Calories: " + calories);

        }
    }

    public static void resetStepCount() {
        stepCount = 0;
        goal = 0;
        distance = 0;
        calories = 0;
        height = 170;
        if (stepViewModel != null) {
            stepViewModel.setStepCount(stepCount);
            stepViewModel.setGoal(goal);
            stepViewModel.setDistance(distance);
            stepViewModel.setCalories(calories);
        }
    }

    public static void setGoal(int newGoal) {
        goal = newGoal;
        if (stepViewModel != null) {
            stepViewModel.setGoal(goal);
        }
    }

    public static void setHeight(int newHeight) {
        height = newHeight;
    }

    public static void setViewModel(StepViewModel viewModel) {
        stepViewModel = viewModel;
    }

    public float calculateDistance(int steps, int height) {
        return steps * height * STEP_LENGTH / 1000;  // Convert to kilometers
    }

    public float calculateCalories(int steps, boolean isRunning) {
        return steps * (isRunning ? CALORIES_RUNNING : CALORIES_WALKING);
    }

    private void updateLiveData() {
        if (stepViewModel != null) {
            stepViewModel.setStepCount(stepCount);
            stepViewModel.setGoal(goal);
            stepViewModel.setDistance(distance);
            stepViewModel.setCalories(calories);
        }
    }

    private void updateDatabase() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        stepDatabaseHelper.updateSteps(today, stepCount);
        stepDatabaseHelper.updateDistance(today, distance);
        stepDatabaseHelper.updateCalories(today, calories);
        stepDatabaseHelper.updateGoal(goal);
    }

    private Notification createNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Counter")
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_steps) // Replace with your icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String content) {
        Notification notification = createNotification(content);
        notificationManager.notify(1, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Step Counter Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
