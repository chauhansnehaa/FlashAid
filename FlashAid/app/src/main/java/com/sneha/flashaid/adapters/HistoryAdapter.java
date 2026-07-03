package com.sneha.flashaid.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sneha.flashaid.R;
import com.sneha.flashaid.models.RequestModel;
import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<RequestModel> requestList;

    public HistoryAdapter(List<RequestModel> requestList) {
        this.requestList = requestList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RequestModel req = requestList.get(position);

        holder.patientName.setText("Patient: " + req.getPatientName());
        holder.severity.setText("Severity: " + req.getSeverity());
        holder.symptoms.setText("Symptoms: " + req.getSymptoms());
        holder.contact.setText("Contact: " + req.getContact());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date(req.getTimestamp()));
        holder.timestamp.setText("Completed on: " + formattedDate);
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView patientName, severity, symptoms, contact, timestamp;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.tvPatientName);
            severity = itemView.findViewById(R.id.tvSeverity);
            symptoms = itemView.findViewById(R.id.tvSymptoms);
            contact = itemView.findViewById(R.id.tvContact);
            timestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
