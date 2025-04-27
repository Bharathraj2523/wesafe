package com.example.thefinalapp;

import android.app.ActivityManager;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Home_page extends AppCompatActivity {
    private Button startPredictBtn;
    private Button stopPredictBtn;
    private boolean isPredicting = false;

    private static final int PERMISSION_REQUEST_CODE = 101;
    private Button startLearningBtn;
    private TextView learningStatusText;
    private ProgressBar learningProgress;
    private boolean isLearningActive = false;
    private String userUid;
    private String userEmail;
    private ImageButton profileIcon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        // Initialize views
        startLearningBtn = findViewById(R.id.startLearningBtn);
        learningStatusText = findViewById(R.id.learningStatusText);
        learningProgress = findViewById(R.id.learningProgress);
        profileIcon = findViewById(R.id.profileIcon);

        profileIcon.setOnClickListener(view ->{
            OnProfileIconClicked();
        });
        // Get user data from Login activity
        Intent intent = getIntent();
        userUid = intent.getStringExtra("USER_UID");
        userEmail = intent.getStringExtra("USER_EMAIL");

        // Verify we have a valid user
        if (userUid == null) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                userUid = currentUser.getUid();
                userEmail = currentUser.getEmail();
            } else {
                // No valid user - return to Login
                Toast.makeText(this, "Authentication required", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, Login.class));
                finish();
                return;
            }
        }

        Intent api_intent = new Intent(this,ApiSyncService.class);
        api_intent.putExtra("USER_UID", userUid);
        ContextCompat.startForegroundService(this, api_intent);

        Log.d("Home_page", "Starting for user: " + userEmail);

        // Check if service is already running when activity starts
        isLearningActive = isMyServiceRunning(MyService.class);
        updateUI();

        startLearningBtn.setOnClickListener(v -> {
            if (!isLearningActive) {
                startLearningProcess();
            } else {
                stopLearningProcess();
            }
        });

        startPredictBtn = findViewById(R.id.startPredictBtn);
        stopPredictBtn = findViewById(R.id.stopPredictBtn);

// Update UI for prediction
        isPredicting = isMyServiceRunning(LocationService.class);
        updatePredictUI();

        startPredictBtn.setOnClickListener(v -> {
            if (!isPredicting) {
                startPredictService();
            }
        });

        stopPredictBtn.setOnClickListener(v -> {
            if (isPredicting) {
                stopPredictService();
            }
        });

    }

    private void startPredictService() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        Intent intent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, intent);
        isPredicting = true;
        updatePredictUI();
        Toast.makeText(this, "Prediction Started", Toast.LENGTH_SHORT).show();
    }

    private void stopPredictService() {
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
        isPredicting = false;
        updatePredictUI();
        Toast.makeText(this, "Prediction Stopped", Toast.LENGTH_SHORT).show();
    }

    private void updatePredictUI() {
        startPredictBtn.setEnabled(!isPredicting);
        stopPredictBtn.setEnabled(isPredicting);
    }


    // Method to check if service is running
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the state when returning to the activity
        isLearningActive = isMyServiceRunning(MyService.class);
        updateUI();
    }

    private void startLearningProcess() {
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            startLocationService();
            isLearningActive = true;
            updateUI();
            Toast.makeText(this, "Learning started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLearningProcess() {
        stopLocationService();
        isLearningActive = false;
        updateUI();
        Toast.makeText(this, "Learning stopped", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        startLearningBtn.setText(isLearningActive ? "Stop Learning" : "Start Learning");
        learningStatusText.setText(isLearningActive ? "Learning in progress" : "Not currently learning");
        learningStatusText.setTextColor(isLearningActive ?
                ContextCompat.getColor(this, android.R.color.holo_green_dark) :
                ContextCompat.getColor(this, android.R.color.holo_red_dark));
        learningProgress.setVisibility(isLearningActive ? View.VISIBLE : View.GONE);
    }

    private boolean checkPermissions() {
        boolean hasFineLoc = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean hasBackgroundLoc = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            return hasFineLoc && hasBackgroundLoc;
        }
        return hasFineLoc;
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationService() {
        if (userUid == null) {
            Toast.makeText(this, "Cannot start service - no authenticated user", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, MyService.class);
        serviceIntent.putExtra("USER_UID", userUid);
        ContextCompat.startForegroundService(this, serviceIntent);

        Log.d("Home_page", "Started service for user: " + userUid);
    }

    private void stopLocationService() {
        Intent serviceIntent = new Intent(this, MyService.class);
        stopService(serviceIntent);
        Log.d("Home_page", "Location Service Stopped");
    }
    public void OnProfileIconClicked(){
        Intent profileIntent = new Intent(this,ProfilePage.class);
        profileIntent.putExtra("USER_UID",userUid);
        startActivity(profileIntent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (checkPermissions()) {
                startLearningProcess();
            } else {
                Toast.makeText(this,
                        "Location permissions are required for learning features",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Service continues running in background
    }
}