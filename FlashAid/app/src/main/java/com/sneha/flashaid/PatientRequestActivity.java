package com.sneha.flashaid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;

public class PatientRequestActivity extends AppCompatActivity {

    private EditText etName, etAge, etContact, etSymptoms;
    private Button btnMild, btnModerate, btnCritical, btnSubmit;
    private String selectedSeverity = "";
    private DatabaseReference dbRef;
    private FusedLocationProviderClient fusedLocationClient;
    private double userLat, userLon;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_request);

        etName = findViewById(R.id.etName);
        etAge = findViewById(R.id.etAge);
        etContact = findViewById(R.id.etContact);
        etSymptoms = findViewById(R.id.etSymptoms);
        btnMild = findViewById(R.id.btnMild);
        btnModerate = findViewById(R.id.btnModerate);
        btnCritical = findViewById(R.id.btnCritical);
        btnSubmit = findViewById(R.id.btnSubmit);

        dbRef = FirebaseDatabase.getInstance().getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 🔹 Severity Toggle Logic
        btnMild.setOnClickListener(v -> selectSeverity("Mild"));
        btnModerate.setOnClickListener(v -> selectSeverity("Moderate"));
        btnCritical.setOnClickListener(v -> selectSeverity("Critical"));

        // 🔹 Handle Submit Button Click
        btnSubmit.setOnClickListener(v -> checkLocationPermissionAndSubmit());
    }

    // ✅ Check for permission safely
    private void checkLocationPermissionAndSubmit() {
        // ✅ Step 1: Check for location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE
            );
            return;
        }

        // ✅ Step 2: Check if GPS (Location Services) is enabled
        com.google.android.gms.location.LocationRequest locationRequest =
                new com.google.android.gms.location.LocationRequest.Builder(0)
                        .setPriority(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY)
                        .setWaitForAccurateLocation(true)
                        .setMaxUpdates(1)
                        .build();

        com.google.android.gms.location.LocationSettingsRequest.Builder builder =
                new com.google.android.gms.location.LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true); // Show GPS enable dialog

        com.google.android.gms.location.SettingsClient settingsClient =
                com.google.android.gms.location.LocationServices.getSettingsClient(this);

        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> {
                    // ✅ Step 3: Try to get last known location
                    fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                        if (location != null) {
                            userLat = location.getLatitude();
                            userLon = location.getLongitude();
                            submitEmergencyRequest();
                        } else {
                            // 🔹 Step 4: Request a fresh location update
                            fusedLocationClient.requestLocationUpdates(locationRequest,
                                    new com.google.android.gms.location.LocationCallback() {
                                        @Override
                                        public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                                            Location newLoc = locationResult.getLastLocation();
                                            if (newLoc != null) {
                                                userLat = newLoc.getLatitude();
                                                userLon = newLoc.getLongitude();
                                                submitEmergencyRequest();
                                            } else {
                                                Toast.makeText(PatientRequestActivity.this,
                                                        "Unable to get accurate location. Please enable GPS.",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                            fusedLocationClient.removeLocationUpdates(this);
                                        }
                                    }, getMainLooper());
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    // ⚠️ Step 5: GPS is disabled — prompt user to enable it
                    if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                        try {
                            ((com.google.android.gms.common.api.ResolvableApiException) e)
                                    .startResolutionForResult(PatientRequestActivity.this, 2001);
                        } catch (Exception ex) {
                            Toast.makeText(this, "Please enable GPS manually.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "GPS not available on this device.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2001) {
            if (resultCode == RESULT_OK) {
                // User enabled GPS → retry location request
                checkLocationPermissionAndSubmit();
            } else {
                Toast.makeText(this, "GPS is required to fetch location.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void selectSeverity(String severity) {
        selectedSeverity = severity;

        // Reset all button colors
        btnMild.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnModerate.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        btnCritical.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));

        // Highlight selected button
        switch (severity) {
            case "Mild":
                btnMild.setBackgroundColor(getResources().getColor(R.color.orange));
                break;
            case "Moderate":
                btnModerate.setBackgroundColor(getResources().getColor(R.color.orange));
                break;
            case "Critical":
                btnCritical.setBackgroundColor(getResources().getColor(R.color.orange));
                break;
        }
    }

    private void submitEmergencyRequest() {
        String name = etName.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String contact = etContact.getText().toString().trim();
        String symptoms = etSymptoms.getText().toString().trim();

        if (name.isEmpty() || age.isEmpty() || contact.isEmpty() || selectedSeverity.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔹 Find nearest online driver
        findNearestDriver(driverId -> {
            if (driverId == null) {
                Toast.makeText(this, "No nearby drivers found!", Toast.LENGTH_SHORT).show();
                return;
            }

            String requestId = dbRef.child("requests").push().getKey();
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("userId", userId);
            requestData.put("driverId", driverId);
            requestData.put("patientName", name);
            requestData.put("age", age);
            requestData.put("contact", contact);
            requestData.put("severity", selectedSeverity);
            requestData.put("symptoms", symptoms);
            requestData.put("status", "pending");
            requestData.put("latitude", userLat);
            requestData.put("longitude", userLon);
            requestData.put("timestamp", System.currentTimeMillis());

            dbRef.child("requests").child(requestId).setValue(requestData)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Request sent to nearest driver!", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(PatientRequestActivity.this, UserDashboardActivity.class);
                        intent.putExtra("requestStatus", "pending");
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });
    }

    private void findNearestDriver(OnDriverFoundListener listener) {
        dbRef.child("liveDrivers").get().addOnCompleteListener(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                listener.onDriverFound(null);
                return;
            }

            String nearestDriverId = null;
            double nearestDistance = Double.MAX_VALUE;

            for (DataSnapshot driverSnap : task.getResult().getChildren()) {
                String status = driverSnap.child("status").getValue(String.class);
                if (status == null || !status.equals("online")) continue;

                Double lat = driverSnap.child("lat").getValue(Double.class);
                Double lon = driverSnap.child("lon").getValue(Double.class);

                if (lat == null || lon == null) continue;

                double distance = calculateDistance(userLat, userLon, lat, lon);
                if (distance < nearestDistance && distance <= 7.0) { // within 7km radius
                    nearestDistance = distance;
                    nearestDriverId = driverSnap.getKey();
                }
            }

            listener.onDriverFound(nearestDriverId);
        });
    }


    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    interface OnDriverFoundListener {
        void onDriverFound(String driverId);
    }

    // ✅ Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissionAndSubmit();
            } else {
                Toast.makeText(this, "Location permission is required to send requests.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
