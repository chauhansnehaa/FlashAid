package com.sneha.flashaid.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sneha.flashaid.R;
import com.sneha.flashaid.models.RequestModel;

import java.util.List;

public class DriverRequestAdapter extends RecyclerView.Adapter<DriverRequestAdapter.ViewHolder> {

    private List<RequestModel> requestList;
    private OnAcceptListener listener;

    public interface OnAcceptListener {
        void onAccept(RequestModel request);
    }

    public DriverRequestAdapter(List<RequestModel> requestList, OnAcceptListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel request = requestList.get(position);

        holder.tvPatientName.setText("Patient: " + request.getPatientName());
        holder.tvSeverity.setText("Severity: " + request.getSeverity());
        holder.tvSymptoms.setText("Symptoms: " + request.getSymptoms());
        holder.tvStatus.setText("Status: " + request.getStatus());
        holder.tvLocation.setText("📍 Lat: " + request.getLatitude() + ", Lon: " + request.getLongitude());

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(request));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPatientName, tvSeverity, tvSymptoms, tvStatus, tvLocation;
        Button btnAccept;

        ViewHolder(View itemView) {
            super(itemView);
            tvPatientName = itemView.findViewById(R.id.tvPatientName);
            tvSeverity = itemView.findViewById(R.id.tvSeverity);
            tvSymptoms = itemView.findViewById(R.id.tvSymptoms);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnAccept = itemView.findViewById(R.id.btnAccept);
        }
    }
}
