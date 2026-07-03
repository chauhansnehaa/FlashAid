package com.sneha.flashaid;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class UserLiveTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private Marker driverMarker, patientMarker;
    private DatabaseReference dbRef;
    private String requestId, userId, trackingId, driverId;
    private double patientLat, patientLon;
    private GeoPoint patientPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        setContentView(R.layout.activity_live_tracking);

        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        dbRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        patientLat = getIntent().getDoubleExtra("latitude", 0);
        patientLon = getIntent().getDoubleExtra("longitude", 0);
        requestId = getIntent().getStringExtra("requestId");

        setupPatientMarker();

        // Get tracking info (driverId and trackingId)
        dbRef.child("tracking")
                .orderByChild("requestId")
                .equalTo(requestId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot trackSnap : snapshot.getChildren()) {
                            trackingId = trackSnap.getKey();
                            driverId = trackSnap.child("driverId").getValue(String.class);
                            break;
                        }

                        if (driverId != null) {
                            observeDriverLocation();
                        } else {
                            Toast.makeText(UserLiveTrackingActivity.this, "Driver not found!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserLiveTrackingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupPatientMarker() {
        IMapController controller = mapView.getController();
        controller.setZoom(14.5);
        patientPoint = new GeoPoint(patientLat, patientLon);

        patientMarker = new Marker(mapView);
        patientMarker.setPosition(patientPoint);
        patientMarker.setTitle("Patient Location");
        patientMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable patientIcon = ContextCompat.getDrawable(this, R.drawable.ic_patient_marker);
        if (patientIcon != null) {
            patientMarker.setIcon(patientIcon);
        }

        mapView.getOverlays().add(patientMarker);
        startPatientMarkerPulse(patientMarker);
        mapView.getController().setCenter(patientPoint);
    }

    private void observeDriverLocation() {
        dbRef.child("liveDrivers").child(driverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Double lat = snapshot.child("lat").getValue(Double.class);
                        Double lon = snapshot.child("lon").getValue(Double.class);

                        if (lat != null && lon != null) {
                            GeoPoint driverPoint = new GeoPoint(lat, lon);

                            if (driverMarker == null) {
                                addDriverMarker(driverPoint);
                                fetchRoadRoute(lat, lon, patientLat, patientLon);
                            } else {
                                driverMarker.setPosition(driverPoint);
                                mapView.getController().animateTo(driverPoint);
                                mapView.invalidate();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserLiveTrackingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addDriverMarker(GeoPoint startPoint) {
        driverMarker = new Marker(mapView);
        driverMarker.setTitle("Ambulance");
        driverMarker.setPosition(startPoint);
        driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_ambulance));
        mapView.getOverlays().add(driverMarker);

        // Center map between driver and patient
        BoundingBox box = BoundingBox.fromGeoPointsSafe(List.of(patientPoint, startPoint));
        mapView.zoomToBoundingBox(box, true);
    }

    private void fetchRoadRoute(double startLat, double startLon, double endLat, double endLon) {
        new Thread(() -> {
            try {
                String urlStr = "https://router.project-osrm.org/route/v1/driving/"
                        + startLon + "," + startLat + ";" + endLon + "," + endLat
                        + "?overview=full&geometries=geojson";

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray coords = json.getJSONArray("routes")
                        .getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates");

                List<GeoPoint> routePoints = new ArrayList<>();
                for (int i = 0; i < coords.length(); i++) {
                    JSONArray point = coords.getJSONArray(i);
                    routePoints.add(new GeoPoint(point.getDouble(1), point.getDouble(0)));
                }

                runOnUiThread(() -> drawPolyline(routePoints));

            } catch (Exception e) {
                runOnUiThread(() ->
                        Toast.makeText(UserLiveTrackingActivity.this, "Route fetch error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void drawPolyline(List<GeoPoint> routePoints) {
        Polyline polyline = new Polyline();
        polyline.setPoints(routePoints);
        polyline.setWidth(8f);
        polyline.setColor(0xAA2196F3);
        mapView.getOverlays().add(polyline);
        mapView.invalidate();
    }

    // 💓 Pulse animation for patient marker
    private void startPatientMarkerPulse(Marker marker) {
        ValueAnimator pulse = ValueAnimator.ofFloat(1f, 1.3f, 1f);
        pulse.setDuration(1200);
        pulse.setRepeatCount(ValueAnimator.INFINITE);

        Drawable originalIcon = marker.getIcon();
        pulse.addUpdateListener(anim -> {
            float scale = (float) anim.getAnimatedValue();
            if (originalIcon != null) {
                int width = (int) (originalIcon.getIntrinsicWidth() * scale);
                int height = (int) (originalIcon.getIntrinsicHeight() * scale);

                Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                        ((BitmapDrawable) originalIcon).getBitmap(),
                        width,
                        height,
                        true
                );
                marker.setIcon(new BitmapDrawable(getResources(), scaledBitmap));
                mapView.invalidate();
            }
        });
        pulse.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
