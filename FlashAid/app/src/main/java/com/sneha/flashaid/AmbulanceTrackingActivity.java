package com.sneha.flashaid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AmbulanceTrackingActivity extends AppCompatActivity {

    private TextView txtMessage;
    private ProgressBar progressBar;

    private DatabaseReference dbRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_tracking);

        txtMessage = findViewById(R.id.txtMessage);
        progressBar = findViewById(R.id.progressBar);

        dbRef = FirebaseDatabase.getInstance().getReference();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (userId == null) {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        checkActiveTracking();
    }

    private void checkActiveTracking() {
        progressBar.setVisibility(View.VISIBLE);
        txtMessage.setVisibility(View.GONE);

        dbRef.child("tracking")
                .orderByChild("userId") // ✅ Make sure each tracking node includes userId
                .equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        boolean foundActive = false;

                        for (DataSnapshot trackSnap : snapshot.getChildren()) {
                            String status = trackSnap.child("status").getValue(String.class);

                            if ("active".equalsIgnoreCase(status)) {
                                foundActive = true;

                                String requestId = trackSnap.child("requestId").getValue(String.class);
                                Double patientLat = trackSnap.child("patientLat").getValue(Double.class);
                                Double patientLon = trackSnap.child("patientLon").getValue(Double.class);

                                if (requestId != null && patientLat != null && patientLon != null) {
                                    // ✅ Launch same LiveTrackingActivity to simulate same map
                                    Intent intent = new Intent(AmbulanceTrackingActivity.this, UserLiveTrackingActivity.class);
                                    intent.putExtra("requestId", requestId);
                                    intent.putExtra("latitude", patientLat);
                                    intent.putExtra("longitude", patientLon);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(AmbulanceTrackingActivity.this, "Invalid tracking data", Toast.LENGTH_SHORT).show();
                                }

                                break;
                            }
                        }

                        if (!foundActive) {
                            txtMessage.setVisibility(View.VISIBLE);
                            txtMessage.setText("No live tracking available");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                        txtMessage.setVisibility(View.VISIBLE);
                        txtMessage.setText("Error: " + error.getMessage());
                    }
                });
    }
}
