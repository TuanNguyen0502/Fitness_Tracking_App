package hcmute.edu.vn.hongtuan;

import android.app.Dialog;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.hongtuan.model.StepDatabaseHelper;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private TextView textView_stepCount, textView_distance, textView_calories, textView_goal;
    private Button button_reset, button_setGoal, button_setHeight;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private StepDatabaseHelper stepDatabaseHelper;
    private int stepCount = 0, goal = 500, height = 170;
    private float distance = 0, calories = 0;
    private long lastUpdate = 0;
    private float lastX, lastY, lastZ;
    private static final float STEP_THRESHOLD = 1.0f;  // Vertical movement sensitivity
    private static final float MOVE_THRESHOLD = 1.5f;  // Forward movement sensitivity
    private static final float SWAY_THRESHOLD = 1.5f;  // Sideways movement limit
    private static final float WALKING_THRESHOLD = 1200f;  // Adjust based on testing
    private static final float RUNNING_THRESHOLD = 2500f;  // Running requires higher speed
    private static final float STEP_LENGTH = 0.43F; // the ratio between a person's stride length and their height is usually equal to 0.43
    private static final float CALORIES_RUNNING = 0.1F; // the average person burns 0.1 calories per step
    private static final float CALORIES_WALKING = 0.04F; // the average person burns 0.04 calories per step
    private static final int DIALOG_GOAL_ID = 0;
    private static final int DIALOG_HEIGHT_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initialize();

        // get the default sensor of the specified type
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            // get the accelerometer sensor
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            // register the sensor listener to listen to the accelerometer sensor
            // the sensor will be registered with a delay of SENSOR_DELAY_NORMAL
            // the listener will be called when the sensor detects a change in the value of the sensor
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }

        getDataFromDatabase();
        setTextViews();

        if (goal == 0) {
            showDialog(DIALOG_GOAL_ID);
        }
        if (height == 0) {
            showDialog(DIALOG_HEIGHT_ID);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
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
                float deltaY = Math.abs(y - lastY);
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
                    } else if (speed > WALKING_THRESHOLD) {
                        calories = calculateCalories(stepCount, false); // Walking if speed is moderate
                    }

                    setTextViews();
                    updateDatabase();
                }
                lastX = x;
                lastY = y;
                lastZ = z;
            }
        }
    }

    private void checkGoal() {
        if (stepCount >= goal) {
            Toast.makeText(getApplicationContext(), "Congratulations! You have reached your goal!", Toast.LENGTH_SHORT).show();
        }
    }

    public float calculateDistance(int steps, int height) {
        float stepLength = height * STEP_LENGTH; // Approximate step length in meters
        return steps * stepLength / 1000;  // Convert to kilometers
    }

    public float calculateCalories(int steps, boolean isRunning) {
        float caloriesPerStep = isRunning ? CALORIES_RUNNING : CALORIES_WALKING;
        return steps * caloriesPerStep;
    }

    private void setTextViews() {
        textView_stepCount.setText(String.valueOf(stepCount));
        textView_distance.setText("🚶 Distance: " + String.format(Locale.getDefault(), "%.2f km", distance));
        textView_calories.setText("🔥 Calories Burned: " + String.format(Locale.getDefault(), "%.2f kcal", calories));
        textView_goal.setText("🎯 Goal: " + goal + " steps");
    }

    private void updateDatabase() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        stepDatabaseHelper.updateSteps(today, stepCount);
        stepDatabaseHelper.updateDistance(today, distance);
        stepDatabaseHelper.updateCalories(today, calories);
    }

    private void getDataFromDatabase() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        stepCount = stepDatabaseHelper.getSteps(today);
        distance = stepDatabaseHelper.getDistance(today);
        calories = stepDatabaseHelper.getCalories(today);
        goal = stepDatabaseHelper.getGoal(today);
    }

    private void initialize() {
        stepDatabaseHelper = new StepDatabaseHelper(this);
        textView_stepCount = findViewById(R.id.textView_stepCount);
        textView_distance = findViewById(R.id.textView_distance);
        textView_calories = findViewById(R.id.textView_calories);
        textView_goal = findViewById(R.id.textView_goal);
        button_reset = findViewById(R.id.button_reset);
        button_reset.setOnClickListener(v -> {
            stepCount = 0;
            distance = 0;
            calories = 0;
            setTextViews();
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            stepDatabaseHelper.updateSteps(today, stepCount);
            stepDatabaseHelper.updateDistance(today, distance);
            stepDatabaseHelper.updateCalories(today, calories);
            showDialog(DIALOG_GOAL_ID);
            showDialog(DIALOG_HEIGHT_ID);
        });
        button_setGoal = findViewById(R.id.button_setGoal);
        button_setGoal.setOnClickListener(v -> {
            showDialog(DIALOG_GOAL_ID);
        });
        button_setHeight = findViewById(R.id.button_setHeight);
        button_setHeight.setOnClickListener(v -> {
            showDialog(DIALOG_HEIGHT_ID);
        });
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_GOAL_ID:
                EditText input_goal = new EditText(this);
                input_goal.setInputType(InputType.TYPE_CLASS_NUMBER); // Only allows numbers
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Set Goal")
                        .setIcon(android.R.drawable.btn_star)
                        .setMessage("How many steps would you like to set as your goal?")
                        .setView(input_goal)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            String userInput = input_goal.getText().toString().trim();
                            // Check if the input is not empty
                            if (!userInput.isEmpty()) {
                                int number = Integer.parseInt(userInput);
                                goal = number;
                                textView_goal.setText("🎯 Goal: " + goal + " steps");
                                stepDatabaseHelper.updateGoal(goal);
                            } else {
                                Toast.makeText(getApplicationContext(), "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            Toast.makeText(getApplicationContext(), "Clicked Cancel!", Toast.LENGTH_SHORT).show();
                        });

                return builder.create();
            case DIALOG_HEIGHT_ID:
                EditText input_height = new EditText(this);
                input_height.setInputType(InputType.TYPE_CLASS_NUMBER); // Only allows numbers
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setTitle("Set Height")
                        .setIcon(android.R.drawable.btn_star)
                        .setMessage("What is your height in centimeters?")
                        .setView(input_height)
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            String userInput = input_height.getText().toString().trim();
                            // Check if the input is not empty
                            if (!userInput.isEmpty()) {
                                int number = Integer.parseInt(userInput);
                                height = number;
                            } else {
                                Toast.makeText(getApplicationContext(), "Please enter a valid number!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, which) -> {
                            Toast.makeText(getApplicationContext(), "Clicked Cancel!", Toast.LENGTH_SHORT).show();
                        });

                return builder1.create();
        }
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
    }
}