package com.sneha.flashaid;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.*;
import com.google.firebase.database.*;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserDashboardActivity extends AppCompatActivity {

    private MapView map;
    LinearLayout navHospitals, navTrips,ambulanceTracking;
    Button btnRequestAmbulance;

    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int GPS_REQUEST_CODE = 2002;
    private DatabaseReference liveDriversRef;
    private HashMap<String, Marker> driverMarkers = new HashMap<>();

    private GeoPoint userLocation;
    private FusedLocationProviderClient fusedClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔧 Load OSMDroid configuration
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        Configuration.getInstance().setUserAgentValue(getPackageName());

        setContentView(R.layout.activity_user_dashboard);

        map = findViewById(R.id.map);
        navHospitals = findViewById(R.id.navHospitals);
        btnRequestAmbulance = findViewById(R.id.btnRequestAmbulance);
        ambulanceTracking = findViewById(R.id.ambulanceTracking);
        navTrips = findViewById(R.id.navTrips);

        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        mapController.setZoom(15.0);

        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        liveDriversRef = FirebaseDatabase.getInstance().getReference("liveDrivers");

        // 📍 Request permissions
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.INTERNET
        });

        // 📍 Ensure GPS is enabled first, then show map
        checkIfGpsEnabledAndFetchLocation(mapController);

        // 🚑 Show live drivers
        fetchLiveDrivers();

        btnRequestAmbulance.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, PatientRequestActivity.class);
            startActivity(intent);
        });

        navTrips.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, UserTripsActivity.class);
            startActivity(intent);
        });

        ambulanceTracking.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, AmbulanceTrackingActivity.class);
            startActivity(intent);

        });

        navHospitals.setOnClickListener(v -> {
            Intent intent = new Intent(UserDashboardActivity.this, NavHospitalActivity.class);
            startActivity(intent);

        });
    }

    // ✅ Checks if GPS is enabled — prompts user if not
    private void checkIfGpsEnabledAndFetchLocation(IMapController mapController) {
        LocationRequest locationRequest = new LocationRequest.Builder(0)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .build();

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest)
                        .setAlwaysShow(true);

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(builder.build())
                .addOnSuccessListener(locationSettingsResponse -> getCurrentLocationAndShow(mapController))
                .addOnFailureListener(e -> {
                    if (e instanceof ResolvableApiException) {
                        try {
                            ((ResolvableApiException) e).startResolutionForResult(
                                    UserDashboardActivity.this, GPS_REQUEST_CODE);
                        } catch (Exception ex) {
                            Toast.makeText(this, "Please enable GPS manually.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "GPS not available on this device.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ✅ Fetch user location and show marker
    private void getCurrentLocationAndShow(IMapController mapController) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }

        fusedClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                showUserMarker(mapController, location);
            } else {
                // Request a fresh location update
                LocationRequest locationRequest = new LocationRequest.Builder(0)
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .setWaitForAccurateLocation(true)
                        .setMaxUpdates(1)
                        .build();

                fusedClient.requestLocationUpdates(locationRequest,
                        new LocationCallback() {
                            @Override
                            public void onLocationResult(@NonNull LocationResult locationResult) {
                                Location freshLocation = locationResult.getLastLocation();
                                if (freshLocation != null) {
                                    showUserMarker(mapController, freshLocation);
                                } else {
                                    Toast.makeText(UserDashboardActivity.this,
                                            "Unable to get current location. Please enable GPS.",
                                            Toast.LENGTH_SHORT).show();
                                }
                                fusedClient.removeLocationUpdates(this);
                            }
                        }, getMainLooper());
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Location error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ✅ Handle GPS dialog result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GPS_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                IMapController mapController = map.getController();
                checkIfGpsEnabledAndFetchLocation(mapController);
            } else {
                Toast.makeText(this, "GPS is required to show your location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showUserMarker(IMapController mapController, Location location) {
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        userLocation = new GeoPoint(lat, lon);
        mapController.setCenter(userLocation);

        Marker userMarker = new Marker(map);
        userMarker.setPosition(userLocation);
        userMarker.setTitle("You are here");
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(userMarker);
        map.invalidate();
    }

    // 🚑 Load all live drivers
    private void fetchLiveDrivers() {
        liveDriversRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                map.getOverlays().clear();
                driverMarkers.clear();

                List<GeoPoint> allPoints = new ArrayList<>();

                // Add user marker again
                if (userLocation != null) {
                    Marker userMarker = new Marker(map);
                    userMarker.setPosition(userLocation);
                    userMarker.setTitle("You are here");
                    userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    map.getOverlays().add(userMarker);
                    allPoints.add(userLocation);
                }

                // Add all live drivers 🚑
                for (DataSnapshot driverSnapshot : snapshot.getChildren()) {
                    Double lat = driverSnapshot.child("lat").getValue(Double.class);
                    Double lon = driverSnapshot.child("lon").getValue(Double.class);
                    String status = driverSnapshot.child("status").getValue(String.class);

                    if (lat != null && lon != null && "online".equals(status)) {
                        GeoPoint driverPoint = new GeoPoint(lat, lon);
                        Marker driverMarker = new Marker(map);
                        driverMarker.setPosition(driverPoint);
                        driverMarker.setTitle("Available Ambulance 🚑");
                        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        Bitmap icon = makeAmbulanceMarker();
                        driverMarker.setIcon(new BitmapDrawable(getResources(), icon));

                        map.getOverlays().add(driverMarker);
                        driverMarkers.put(driverSnapshot.getKey(), driverMarker);
                        allPoints.add(driverPoint);
                    }
                }

                if (allPoints.size() > 1) {
                    BoundingBox box = BoundingBox.fromGeoPointsSafe(allPoints);
                    map.zoomToBoundingBox(box, true);
                }

                map.invalidate();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserDashboardActivity.this, "Failed to load drivers", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap makeAmbulanceMarker() {
        int size = 100;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(0xFFFF0000);
        canvas.drawCircle(size / 2f, size / 2f, size / 2.2f, paint);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_map_ambulance);
        if (drawable != null) {
            int iconSize = 60;
            int left = (size - iconSize) / 2;
            int top = (size - iconSize) / 2;
            drawable.setBounds(left, top, left + iconSize, top + iconSize);
            drawable.draw(canvas);
        }
        return bmp;
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            Toast.makeText(this,
                    granted ? "Permissions granted" : "Permissions denied. Map may not work properly.",
                    Toast.LENGTH_SHORT).show();
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
