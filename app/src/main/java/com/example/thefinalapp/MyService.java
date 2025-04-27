package com.example.thefinalapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MyService extends Service {
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = "LocationService";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private Looper serviceLooper;
    private HandlerThread handlerThread;
    private DatabaseReference databaseReference;
    private String userUid;

    private final ConcurrentLinkedQueue<Location> locationQueue = new ConcurrentLinkedQueue<>();
    private boolean isProcessing = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        handlerThread = new HandlerThread("LocationThread");
        handlerThread.start();
        serviceLooper = handlerThread.getLooper();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get user UID from MainActivity
        if (intent != null) {
            userUid = intent.getStringExtra("USER_UID");
        }

        // Fallback to current Firebase user if UID not provided
        if (userUid == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userUid = user.getUid();
            } else {
                Log.e(TAG, "No user UID available - stopping service");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Initialize database reference with user-specific path
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("Final_App")
                .child(userUid)
                .child("locations");

        if (!hasLocationPermission()) {
            Log.e(TAG, "Location permission missing - stopping service");
            stopSelf();
            return START_NOT_STICKY;
        }

        setupLocationUpdates();
        return START_STICKY;
    }

    private void setupLocationUpdates() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = createLocationRequest();
        locationCallback = createLocationCallback();

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    serviceLooper
            );
            Log.d(TAG, "Location updates started for user: " + userUid);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission revoked", e);
            stopSelf();
        }
    }

    private LocationRequest createLocationRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                    .setMinUpdateIntervalMillis(1000)
                    .setMinUpdateDistanceMeters(5)
                    .build();
        } else {
            return LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(1000)
                    .setFastestInterval(1000)
                    .setSmallestDisplacement(5);
        }
    }

    private LocationCallback createLocationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || locationResult.getLocations().isEmpty()) return;

                for (Location currentLocation : locationResult.getLocations()) {
                    Log.d(TAG, String.format("Received location: %.6f,%.6f (Accuracy: %.1fm)",
                            currentLocation.getLatitude(),
                            currentLocation.getLongitude(),
                            currentLocation.getAccuracy()));

                    if (shouldProcessLocation(currentLocation)) {
                        lastLocation = currentLocation;
                        locationQueue.add(currentLocation);
                        processLocationQueue();
                    }
                }
            }
        };
    }

    private boolean shouldProcessLocation(Location newLocation) {
        if (lastLocation == null) return true;

        float distance = lastLocation.distanceTo(newLocation);
        boolean shouldProcess = distance >= 5;

        if (!shouldProcess) {
            Log.d(TAG, "Skipping update - movement too small: " + distance + "m");
        }
        return shouldProcess;
    }

    private void processLocationQueue() {
        if (isProcessing) return;
        isProcessing = true;

        new Thread(() -> {
            try {
                while (!locationQueue.isEmpty()) {
                    Location location = locationQueue.poll();
                    if (location != null) {
                        uploadLocation(location);
                    }
                }
            } finally {
                isProcessing = false;
            }
        }).start();
    }

    private void uploadLocation(Location location) {
        LocationData locationData = createLocationData(location);

        databaseReference.push().setValue(locationData)
                .addOnSuccessListener(unused ->
                        Log.d(TAG, "Location saved to Firebase"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save location", e);
                    if (e.getMessage().contains("Permission denied")) {
                        Log.e(TAG, "Firebase write denied - check security rules");
                    }
                });
    }

    private LocationData createLocationData(Location location) {
        Date date = new Date(location.getTime());
        return new LocationData(
                location.getLatitude(),
                location.getLongitude(),
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(date),
                new SimpleDateFormat("EEEE", Locale.getDefault()).format(date)
        );
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking Active")
                .setContentText("Saving your location data securely")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service stopping");
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void chumma(){
       

    }
}