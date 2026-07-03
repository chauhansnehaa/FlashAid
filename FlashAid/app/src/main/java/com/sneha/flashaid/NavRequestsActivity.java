package com.sneha.flashaid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sneha.flashaid.adapters.DriverRequestAdapter;
import com.sneha.flashaid.models.RequestModel;

import java.util.ArrayList;
import java.util.List;

public class NavRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView txtNoRequests;
    private DriverRequestAdapter adapter;
    private List<RequestModel> requestList;

    private FirebaseAuth mAuth;
    private DatabaseReference dbRef;
    private String driverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_nav_requests);

        recyclerView = findViewById(R.id.driverRequestsRecycler);
        txtNoRequests = findViewById(R.id.txtNoRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();

        adapter = new DriverRequestAdapter(requestList, this::onAcceptRequest);
        recyclerView.setAdapter(adapter);

        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();
        driverId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;

        if (driverId == null) {
            Toast.makeText(this, "Invalid driver session", Toast.LENGTH_SHORT).show();
            return;
        }

        fetchAssignedRequests();
    }

    private void fetchAssignedRequests() {
        dbRef.child("requests") // ✅ correct Firebase node
                .orderByChild("driverId")
                .equalTo(driverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot req : snapshot.getChildren()) {
                            RequestModel model = req.getValue(RequestModel.class);
                            if (model != null && "pending".equalsIgnoreCase(model.getStatus())) {
                                model.setRequestId(req.getKey()); // store key for updating
                                requestList.add(model);
                            }
                        }

                        adapter.notifyDataSetChanged();

                        // Show "no requests" text if empty
                        if (requestList.isEmpty()) {
                            txtNoRequests.setVisibility(View.VISIBLE);
                        } else {
                            txtNoRequests.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NavRequestsActivity.this,
                                "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onAcceptRequest(RequestModel request) {
        // Mark the request as accepted
        dbRef.child("requests").child(request.getRequestId())
                .child("status").setValue("accepted")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request accepted successfully", Toast.LENGTH_SHORT).show();

                    // Fetch driver’s current live location from /liveDrivers
                    dbRef.child("liveDrivers").child(driverId)
                            .get()
                            .addOnSuccessListener(snapshot -> {
                                Double driverLat = snapshot.child("lat").getValue(Double.class);
                                Double driverLon = snapshot.child("lon").getValue(Double.class);

                                if (driverLat == null || driverLon == null) {
                                    Toast.makeText(this, "Driver location unavailable", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // ✅ Fetch the userId from the request node
                                String userId = request.getUserId();
                                if (userId == null || userId.isEmpty()) {
                                    Toast.makeText(this, "User ID missing in request", Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                // Create a tracking entry in Firebase
                                String trackingId = dbRef.child("tracking").push().getKey();
                                if (trackingId != null) {
                                    DatabaseReference trackRef = dbRef.child("tracking").child(trackingId);
                                    trackRef.child("driverId").setValue(driverId);
                                    trackRef.child("userId").setValue(userId); // ✅ store user ID
                                    trackRef.child("requestId").setValue(request.getRequestId());
                                    trackRef.child("driverLat").setValue(driverLat);
                                    trackRef.child("driverLon").setValue(driverLon);
                                    trackRef.child("patientLat").setValue(request.getLatitude());
                                    trackRef.child("patientLon").setValue(request.getLongitude());
                                    trackRef.child("status").setValue("active");
                                }

//                                // Launch the live tracking screen
//                                Intent intent = new Intent(NavRequestsActivity.this, LiveTrackingActivity.class);
//                                intent.putExtra("requestId", request.getRequestId());
//                                intent.putExtra("latitude", request.getLatitude());
//                                intent.putExtra("longitude", request.getLongitude());
//                                startActivity(intent);


//                                Intent intent = new Intent(NavRequestsActivity.this, NavTrackingActivity.class);
//                                startActivity(intent);

                                // ✅ Start tracking service after node is created
                                Intent serviceIntent = new Intent(NavRequestsActivity.this, TrackingService.class);
                                serviceIntent.putExtra("trackingId", trackingId);
                                serviceIntent.putExtra("driverId", driverId);
                                startService(serviceIntent);

                                Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error fetching driver location: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

}
