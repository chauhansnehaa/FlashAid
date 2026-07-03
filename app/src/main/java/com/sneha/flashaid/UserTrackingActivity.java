package com.sneha.flashaid;

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

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class UserTrackingActivity extends AppCompatActivity {

    private MapView mapView;
    private Marker driverMarker, patientMarker;
    private DatabaseReference dbRef;
    private String userId;
    private double patientLat, patientLon;

    private ValueEventListener trackingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        setContentView(R.layout.activity_live_tracking);

        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);

        dbRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch user's last known patient location from requests table
        fetchPatientLocation();
    }

    private void fetchPatientLocation() {
        dbRef.child("requests").orderByChild("userId").equalTo(userId)
                .limitToLast(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot requestSnap : snapshot.getChildren()) {
                            patientLat = requestSnap.child("latitude").getValue(Double.class);
                            patientLon = requestSnap.child("longitude").getValue(Double.class);
                            setupMap();
                            startTrackingListener();
                            return;
                        }
                        Toast.makeText(UserTrackingActivity.this, "No active tracking found", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserTrackingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupMap() {
        IMapController controller = mapView.getController();
        controller.setZoom(15.0);
        GeoPoint patientPoint = new GeoPoint(patientLat, patientLon);

        // Add patient marker
        patientMarker = new Marker(mapView);
        patientMarker.setPosition(patientPoint);
        patientMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        patientMarker.setTitle("Your Location");
        patientMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_patient_marker));
        mapView.getOverlays().add(patientMarker);

        mapView.getController().setCenter(patientPoint);
    }

    private void startTrackingListener() {
        trackingListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(UserTrackingActivity.this, "Tracking ended", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Double driverLat = snapshot.child("driverLat").getValue(Double.class);
                Double driverLon = snapshot.child("driverLon").getValue(Double.class);
                String status = snapshot.child("status").getValue(String.class);

                if (driverLat != null && driverLon != null) {
                    GeoPoint driverPoint = new GeoPoint(driverLat, driverLon);
                    updateDriverMarker(driverPoint);
                    drawPolyline(driverPoint, new GeoPoint(patientLat, patientLon));
                }

                if ("completed".equalsIgnoreCase(status)) {
                    Toast.makeText(UserTrackingActivity.this, "Ambulance has arrived 🚑", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(UserTrackingActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        dbRef.child("userTracking").child(userId).addValueEventListener(trackingListener);
    }

    private void updateDriverMarker(GeoPoint driverPoint) {
        if (driverMarker == null) {
            driverMarker = new Marker(mapView);
            driverMarker.setTitle("Ambulance");
            driverMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            driverMarker.setIcon(ContextCompat.getDrawable(this, R.drawable.ic_map_ambulance));
            mapView.getOverlays().add(driverMarker);
        }
        driverMarker.setPosition(driverPoint);
        mapView.getController().setCenter(driverPoint);
        mapView.invalidate();
    }

    private void drawPolyline(GeoPoint start, GeoPoint end) {
        mapView.getOverlays().removeIf(o -> o instanceof Polyline); // remove old route

        List<GeoPoint> points = new ArrayList<>();
        points.add(start);
        points.add(end);

        Polyline line = new Polyline();
        line.setPoints(points);
        line.setColor(0xAA2196F3);
        line.setWidth(8f);

        mapView.getOverlays().add(line);
        mapView.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trackingListener != null) {
            dbRef.child("userTracking").child(userId).removeEventListener(trackingListener);
        }
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
