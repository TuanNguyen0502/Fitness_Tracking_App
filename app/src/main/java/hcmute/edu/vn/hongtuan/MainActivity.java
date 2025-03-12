package hcmute.edu.vn.hongtuan;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.hongtuan.model.StepViewModel;

public class MainActivity extends AppCompatActivity {
    private TextView textView_stepCount, textView_distance, textView_calories, textView_goal;
    private Button button_reset, button_setGoal, button_setHeight, button_start, button_stop;
    private StepViewModel stepViewModel;
    private StepDatabaseHelper stepDatabaseHelper;
    private int stepCount = 0, goal, height = 170;
    private float distance = 0, calories = 0;
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

        // Initialize UI components
        initialize();
        goal = stepDatabaseHelper.getGoal(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        textView_goal.setText("🎯 Goal: " + goal + " steps");
        // Start step counting
        startStepCounting();

        stepViewModel = new ViewModelProvider(this).get(StepViewModel.class);
        StepCounterService.setViewModel(stepViewModel); // Pass ViewModel to service
        // Observe LiveData and update UI
        stepViewModel.getStepCount().observe(this, steps -> textView_stepCount.setText(steps.toString()));
        stepViewModel.getDistance().observe(this, distance -> textView_distance.setText("🚶 Distance: " + String.format(Locale.getDefault(), "%.2f km", distance)));
        stepViewModel.getCalories().observe(this, calories -> textView_calories.setText("🔥 Calories Burned: " + String.format(Locale.getDefault(), "%.2f kcal", calories)));
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
            updateDatabase();
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
        button_start = findViewById(R.id.button_start);
        button_start.setOnClickListener(v -> {
            startStepCounting();
        });
        button_stop = findViewById(R.id.button_stop);
        button_stop.setOnClickListener(v -> {
            stopStepCounting();
        });
    }

    private void startStepCounting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 100);
                return;
            }
        }
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        }
        Toast.makeText(this, "Step counting started!", Toast.LENGTH_SHORT).show();
    }

    private void stopStepCounting() {
        Intent serviceIntent = new Intent(this, StepCounterService.class);
        stopService(serviceIntent);
        Toast.makeText(this, "Step counting stopped!", Toast.LENGTH_SHORT).show();
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
}