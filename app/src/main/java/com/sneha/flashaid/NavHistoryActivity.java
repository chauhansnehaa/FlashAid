package com.sneha.flashaid;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.sneha.flashaid.adapters.HistoryAdapter;
import com.sneha.flashaid.models.RequestModel;

import java.text.SimpleDateFormat;
import java.util.*;

public class NavHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<RequestModel> historyList;
    private DatabaseReference requestRef;
    private String currentDriverId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nav_history);

        recyclerView = findViewById(R.id.historyRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);

        currentDriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        requestRef = FirebaseDatabase.getInstance().getReference("requests");

        fetchCompletedRequests();
    }

    private void fetchCompletedRequests() {
        requestRef.orderByChild("driverId").equalTo(currentDriverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        historyList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            RequestModel req = ds.getValue(RequestModel.class);
                            if (req != null && "completed".equalsIgnoreCase(req.getStatus())) {
                                historyList.add(req);
                            }
                        }

                        if (historyList.isEmpty()) {
                            Toast.makeText(NavHistoryActivity.this, "No completed requests yet.", Toast.LENGTH_SHORT).show();
                        }

                        Collections.reverse(historyList); // newest first
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(NavHistoryActivity.this, "Failed to load history.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
