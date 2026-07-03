package com.sneha.flashaid;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.sneha.flashaid.adapters.RequestAdapter;
import com.sneha.flashaid.models.RequestModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserTripsActivity extends AppCompatActivity {

    private RecyclerView recyclerTrips;
    private List<RequestModel> requestList;
    private RequestAdapter adapter;
    private DatabaseReference requestsRef;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_trips);

        recyclerTrips = findViewById(R.id.recyclerTrips);
        recyclerTrips.setLayoutManager(new LinearLayoutManager(this));

        requestList = new ArrayList<>();
        adapter = new RequestAdapter(this, requestList);
        recyclerTrips.setAdapter(adapter);

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestsRef = FirebaseDatabase.getInstance().getReference("requests");

        loadRequests();
    }

    private void loadRequests() {
        requestsRef.orderByChild("userId").equalTo(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        requestList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            RequestModel model = snap.getValue(RequestModel.class);
                            if (model != null) {
                                model.requestId = snap.getKey();
                                requestList.add(model);
                            }
                        }
                        Collections.reverse(requestList); // newest first
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(UserTripsActivity.this,
                                "Failed to load requests: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
