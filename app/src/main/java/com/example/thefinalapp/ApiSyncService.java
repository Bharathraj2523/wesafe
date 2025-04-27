package com.example.thefinalapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiSyncService extends Service {
    private static final String CHANNEL_ID = "ApiSyncChannel";
    private static final int NOTIFICATION_ID = 2;
    private static final String TAG = "ApiSyncService";
    private static final long SYNC_INTERVAL_MINUTES = 15; // Sync every 15 minutes

    private HandlerThread handlerThread;
    private BackendApiService backendApi;
    private String userUid;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());
        handlerThread = new HandlerThread("ApiSyncThread");
        handlerThread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            userUid = intent.getStringExtra("USER_UID");
        }

        if (userUid == null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                userUid = user.getUid();
            } else {
                Log.e(TAG, "No user UID - stopping service");
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        // Initialize Retrofit with String converter
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://192.168.170.155:5000/") // Your Flask server
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();

        backendApi = retrofit.create(BackendApiService.class);
        startPeriodicSync();

        return START_STICKY;
    }

    private void startPeriodicSync() {
        new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    sendFirebaseUrlToServer();
                    runPythonFunction();
                    TimeUnit.MINUTES.sleep(SYNC_INTERVAL_MINUTES);
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Sync thread interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    private void runPythonFunction() {
        try {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(this));
            }
            Python py = Python.getInstance();
            String path = getFilesDir().getAbsolutePath();
            PyObject result = py.getModule("predicting").callAttr("fetch_summary_and_save", path);
            String status = result.toString();


        } catch (Exception e) {
            Log.e(TAG, "Python error: ", e);

        }
    }

    private void sendFirebaseUrlToServer() {
        String firebaseBaseUrl = "https://wsafe-bc890-default-rtdb.asia-southeast1.firebasedatabase.app/";
        String fullUrl = firebaseBaseUrl + "Final_App/" + userUid + "/locations";

        backendApi.sendFirebaseUrl(fullUrl).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "URL sent successfully: " + response.body());
                } else {
                    Log.e(TAG, "Failed to send URL: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e(TAG, "API call failed", t);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "API Sync Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Syncing Data to Backend")
                .setContentText("Sending Firebase URL to server")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        if (handlerThread != null) {
            handlerThread.quitSafely();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}