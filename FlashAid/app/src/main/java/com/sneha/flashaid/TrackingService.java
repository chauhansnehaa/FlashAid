package com.sneha.flashaid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.util.GeoPoint;

import java.util.List;

public class TrackingService extends Service {

    private DatabaseReference dbRef;
    private String driverId, requestId, trackingId;
    private double patientLat, patientLon;
    private List<GeoPoint> routePoints;
    private boolean isRunning = false;

    private DatabaseReference trackingRef;
    private ValueEventListener stopListener;

    @Override
    public void onCreate() {
        super.onCreate();
        dbRef = FirebaseDatabase.getInstance().getReference();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        driverId = intent.getStringExtra("driverId");
        requestId = intent.getStringExtra("requestId");
        trackingId = intent.getStringExtra("trackingId");
        patientLat = intent.getDoubleExtra("patientLat", 0);
        patientLon = intent.getDoubleExtra("patientLon", 0);
        routePoints = (List<GeoPoint>) intent.getSerializableExtra("routePoints");

        startForegroundService();

        if (trackingId != null) {
            trackingRef = dbRef.child("tracking").child(trackingId);
            addTrackingStopListener();
        }

        if (!isRunning) simulateMovement();
        return START_STICKY;
    }

    private void startForegroundService() {
        String channelId = "tracking_channel";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Ambulance Live Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("🚑 Ambulance Tracking Active")
                .setContentText("Tracking in progress...")
                .setSmallIcon(R.drawable.ic_map_ambulance)
                .build();

        startForeground(1, notification);
    }


    private void showArrivalNotification() {
        String channelId = "arrival_channel";
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 🔊 Get user's current notification sound
            android.net.Uri notificationSound = android.provider.Settings.System.DEFAULT_NOTIFICATION_URI;

            // Recreate channel with proper sound each time
            notificationManager.deleteNotificationChannel(channelId);

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Ambulance Arrival Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500});
            channel.setSound(
                    notificationSound,
                    new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
            );

            notificationManager.createNotificationChannel(channel);
        }

        // Build and show notification
        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("🚑 Ambulance Arrived")
                .setContentText("Ambulance has reached the patient location.")
                .setSmallIcon(R.drawable.ic_map_ambulance)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 500, 250, 500})
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // ensures fallback
                .build();

        notificationManager.notify(2, notification);

        // Redirect to dashboard
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(getApplicationContext(),
                    "Ambulance tracking completed successfully 🚑",
                    Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, DriverDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }, 2000);
    }



    private void simulateMovement() {
        if (routePoints == null || routePoints.isEmpty()) {
            stopSelf();
            return;
        }

        isRunning = true;

        new Thread(() -> {
            try {
                for (GeoPoint current : routePoints) {
                    if (!isRunning) break;

                    updateFirebasePosition(current.getLatitude(), current.getLongitude());

                    // ✅ Distance check every update
                    float[] distance = new float[1];
                    android.location.Location.distanceBetween(
                            current.getLatitude(), current.getLongitude(),
                            patientLat, patientLon,
                            distance
                    );

                    if (distance[0] < 100) { // within 30 meters
                        Log.d("TrackingService", "🎯 Ambulance reached patient location!");

                        // ✅ 1. Mark completed
                        dbRef.child("requests").child(requestId).child("status").setValue("completed")
                                .addOnSuccessListener(aVoid -> {
                                    // ✅ 2. Remove tracking node after status is saved
                                    if (trackingId != null) {
                                        dbRef.child("tracking").child(trackingId).removeValue()
                                                .addOnSuccessListener(v -> {
                                                    showArrivalNotification();
                                                    stopSelf();
                                                })
                                                .addOnFailureListener(e -> Log.e("TrackingService", "Failed to remove tracking node: " + e.getMessage()));
                                    } else {
                                        showArrivalNotification();
                                        stopSelf();
                                    }
                                })
                                .addOnFailureListener(e ->
                                        Log.e("TrackingService", "Failed to mark completed: " + e.getMessage()));
                        break;
                    }


                    Thread.sleep(500); // move every 0.5 sec
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateFirebasePosition(double lat, double lon) {
        dbRef.child("liveDrivers").child(driverId).child("lat").setValue(lat);
        dbRef.child("liveDrivers").child(driverId).child("lon").setValue(lon);

        if (trackingId != null) {
            dbRef.child("tracking").child(trackingId).child("driverLat").setValue(lat);
            dbRef.child("tracking").child(trackingId).child("driverLon").setValue(lon);
        }
    }

    private void addTrackingStopListener() {
        stopListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    stopSelf();
                    if (trackingRef != null && stopListener != null)
                        trackingRef.removeEventListener(stopListener);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("TrackingService", "Listener cancelled: " + error.getMessage());
            }
        };
        trackingRef.addValueEventListener(stopListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (trackingRef != null && stopListener != null) {
            trackingRef.removeEventListener(stopListener);
        }
        isRunning = false;
        Log.d("TrackingService", "Service destroyed and listener removed.");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
