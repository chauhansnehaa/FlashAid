package com.sneha.flashaid.adapters;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.sneha.flashaid.R;
import com.sneha.flashaid.models.RequestModel;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private final Context context;
    private final List<RequestModel> requestList;

    public RequestAdapter(Context context, List<RequestModel> requestList) {
        this.context = context;
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_trip, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        holder.tvPatientName.setText("Patient: " + request.getPatientName());
        holder.tvSeverity.setText("Severity: " + request.getSeverity());
        holder.tvSymptoms.setText("Symptoms: " + request.getSymptoms());
        holder.tvStatus.setText("Status: " + request.getStatus());

        // color for status
        String status = request.getStatus().toLowerCase();
        if (status.equals("pending")) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.orange));
        } else if (status.equals("accepted")) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.green));
        } else if (status.equals("completed")) {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gray));
        } else {
            holder.tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvSeverity, tvSymptoms, tvStatus;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
