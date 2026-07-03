package com.sneha.flashaid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.*;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.*;
import com.google.firebase.database.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Spinner roleSpinner;
    private Button submitBtn;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FusedLocationProviderClient fusedLocationClient;

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("users");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        submitBtn = findViewById(R.id.submitBtn);

        // Role spinner setup
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"User", "Driver"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        submitBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String role = roleSpinner.getSelectedItem().toString();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            loginOrRegister(email, password, role);
        });
    }

    private void loginOrRegister(String email, String password, String role) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        navigateToRoleScreen(role);
                    } else {
                        registerNewUser(email, password, role);
                    }
                });
    }

    private void registerNewUser(String email, String password, String role) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        if (role.equals("Driver")) {
                            requestLocationAndSaveDriver(uid, email, role);
                        } else {
                            // Regular user (patient)
                            userRef.child(uid).setValue(new UserModel(email, role));
                            Toast.makeText(this, "User account created!", Toast.LENGTH_SHORT).show();
                            navigateToRoleScreen(role);
                        }
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void requestLocationAndSaveDriver(String uid, String email, String role) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double baseLat, baseLon;

            if (location != null) {
                baseLat = location.getLatitude();
                baseLon = location.getLongitude();
            } else {
                // fallback = AC Patil College coordinates
                baseLat = 19.0416;
                baseLon = 73.0696;
            }

            // Generate random location within 7km
            double[] mockCoords = generateRandomLocation(baseLat, baseLon, 7);

            Map<String, Object> driverData = new HashMap<>();
            driverData.put("email", email);
            driverData.put("role", role);
            driverData.put("latitude", mockCoords[0]);
            driverData.put("longitude", mockCoords[1]);

            userRef.child(uid).setValue(driverData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Driver registered with mock location!", Toast.LENGTH_SHORT).show();
                        navigateToRoleScreen(role);
                    });
        });
    }

    // Generates random lat/lon within radius km
    private double[] generateRandomLocation(double baseLat, double baseLon, int radiusKm) {
        Random random = new Random();
        double radiusMeters = radiusKm * 1000;

        double randomDist = random.nextDouble() * radiusMeters;
        double randomAngle = random.nextDouble() * 2 * Math.PI;

        double deltaLat = (randomDist * Math.cos(randomAngle)) / 111320f;
        double deltaLon = (randomDist * Math.sin(randomAngle)) / (111320f * Math.cos(Math.toRadians(baseLat)));

        double newLat = baseLat + deltaLat;
        double newLon = baseLon + deltaLon;

        return new double[]{newLat, newLon};
    }

    private void navigateToRoleScreen(String role) {
        Intent intent;
        if (role.equals("Driver")) {
            intent = new Intent(LoginActivity.this, DriverDashboardActivity.class);
        } else {
            intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    public static class UserModel {
        public String email, role;

        public UserModel() {}
        public UserModel(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }
}
