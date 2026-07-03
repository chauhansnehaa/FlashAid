package com.sneha.flashaid.adapters;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.sneha.flashaid.R;
import com.sneha.flashaid.models.HospitalModel;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.ViewHolder> {

    private List<HospitalModel> hospitals;
    private Context context;

    public HospitalAdapter(Context context, List<HospitalModel> hospitals) {
        this.context = context;
        this.hospitals = hospitals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hospital, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HospitalModel hospital = hospitals.get(position);
        holder.txtName.setText(hospital.getName());
        holder.txtAddress.setText(hospital.getAddress());

        holder.btnNavigate.setOnClickListener(v -> {
            double lat = hospital.getLatitude();
            double lon = hospital.getLongitude();

            // Open Google Maps navigation intent
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            try {
                context.startActivity(mapIntent);
            } catch (Exception e) {
                Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lon);
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                context.startActivity(browserIntent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return hospitals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtAddress;
        MaterialButton btnNavigate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtHospitalName);
            txtAddress = itemView.findViewById(R.id.txtHospitalAddress);
            btnNavigate = itemView.findViewById(R.id.btnNavigate);
        }
    }
}