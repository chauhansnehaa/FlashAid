package com.sneha.flashaid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DriverDashboardActivity extends AppCompatActivity {

    private MapView map;
    private TextView statusText;
    private Button btnGoLive;
    private LinearLayout navRequests, navTracking, navHistory;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    private FusedLocationProviderClient fusedLocationClient;
    private boolean isLive = false;
    private String driverId;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;
    private static final int ALL_PERMISSIONS_REQUEST_CODE = 202;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 303; // ✅ Added

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        setContentView(R.layout.activity_driver_dashboard);

        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, "Unexpected error: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference();
        driverId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "unknown";

        checkDriverStatus();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // UI elements
        map = findViewById(R.id.mapDriver);
        statusText = findViewById(R.id.statusText);
        btnGoLive = findViewById(R.id.btnGoLive);
        navRequests = findViewById(R.id.navRequests);
        navTracking = findViewById(R.id.navTracking);
        navHistory = findViewById(R.id.navHistory);

        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(15.0);

        // Request all runtime permissions (foreground, background, fine/coarse)
        handleAllPermissions();

        // ✅ Ask for POST_NOTIFICATIONS permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                );
            }
        }

        btnGoLive.setOnClickListener(v -> {
            try {
                if (isLive) {
                    // Go Offline
                    if (driverId == null || driverId.equals("unknown")) {
                        Toast.makeText(this, "Invalid driver ID", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    userRef.child("liveDrivers").child(driverId).removeValue()
                            .addOnSuccessListener(unused -> {
                                statusText.setText("Status: Offline");
                                btnGoLive.setText("Go Live");
                                isLive = false;
                                Toast.makeText(this, "You are now Offline", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                } else {
                    // Go Live → Generate mock location safely
                    getCurrentLocation(location -> {
                        if (location == null) {
                            Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        GeoPoint mockPoint = generateRandomMockLocation(
                                location.getLatitude(), location.getLongitude(), 7
                        );

                        if (mockPoint == null) {
                            Toast.makeText(this, "Error generating mock location", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Map<String, Object> liveData = new HashMap<>();
                        liveData.put("lat", mockPoint.getLatitude());
                        liveData.put("lon", mockPoint.getLongitude());
                        liveData.put("status", "online");

                        userRef.child("liveDrivers").child(driverId).setValue(liveData)
                                .addOnSuccessListener(unused -> {
                                    updateDriverMarker(mockPoint);
                                    statusText.setText("Status: Live");
                                    btnGoLive.setText("Go Offline");
                                    isLive = true;
                                    Toast.makeText(this, "You are now Live", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this,
                                        "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        navRequests.setOnClickListener(v -> {
            startActivity(new Intent(DriverDashboardActivity.this, NavRequestsActivity.class));
        });

        navTracking.setOnClickListener(v -> {
            Intent intent = new Intent(DriverDashboardActivity.this, NavTrackingActivity.class);
            startActivity(intent);

        });

        navHistory.setOnClickListener(v -> {
            Intent intent = new Intent(DriverDashboardActivity.this, NavHistoryActivity.class);
            startActivity(intent);

        });
    }

    private void updateDriverMarker(GeoPoint point) {
        map.getOverlays().clear();
        Marker driverMarker = new Marker(map);
        driverMarker.setPosition(point);
        driverMarker.setTitle("My Mock Location");
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(driverMarker);
        map.getController().setCenter(point);
        map.invalidate();
    }

    private void checkDriverStatus() {
        userRef.child("liveDrivers").child(driverId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        // Driver is already live
                        Double lat = snapshot.child("lat").getValue(Double.class);
                        Double lon = snapshot.child("lon").getValue(Double.class);

                        if (lat != null && lon != null) {
                            GeoPoint livePoint = new GeoPoint(lat, lon);
                            updateDriverMarker(livePoint);
                            statusText.setText("Status: Live");
                            btnGoLive.setText("Go Offline");
                            isLive = true;
                        }
                    } else {
                        // Driver is offline
                        statusText.setText("Status: Offline");
                        btnGoLive.setText("Go Live");
                        isLive = false;
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching status: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private GeoPoint generateRandomMockLocation(double baseLat, double baseLon, double radiusKm) {
        try {
            Random random = new Random();
            double radiusInDegrees = radiusKm / 111.32;

            double u = random.nextDouble();
            double v = random.nextDouble();
            double w = radiusInDegrees * Math.sqrt(u);
            double t = 2 * Math.PI * v;
            double latOffset = w * Math.cos(t);
            double lonOffset = w * Math.sin(t) / Math.cos(Math.toRadians(baseLat));

            double newLat = baseLat + latOffset;
            double newLon = baseLon + lonOffset;

            return new GeoPoint(newLat, newLon);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void getCurrentLocation(OnLocationFetchedListener listener) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                listener.onLocationFetched(location);
            } else {
                // Fallback: request new location update
                com.google.android.gms.location.LocationRequest locationRequest =
                        new com.google.android.gms.location.LocationRequest.Builder(
                                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000)
                                .setMaxUpdates(1)
                                .build();

                fusedLocationClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null
                ).addOnSuccessListener(newLocation -> {
                    if (newLocation != null) {
                        listener.onLocationFetched(newLocation);
                    } else {
                        Toast.makeText(this, "Still unable to fetch current location", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private interface OnLocationFetchedListener {
        void onLocationFetched(Location location);
    }

    // ✅ Combined Permission Handler for Foreground + Background + Foreground Service
    private void handleAllPermissions() {
        String[] permissions;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.FOREGROUND_SERVICE,
                    Manifest.permission.FOREGROUND_SERVICE_LOCATION
            };
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == ALL_PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "All permissions are required for full functionality", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show();
            }
        }
        // ✅ Handle POST_NOTIFICATIONS permission
        else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied — notifications may not appear", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }
}
