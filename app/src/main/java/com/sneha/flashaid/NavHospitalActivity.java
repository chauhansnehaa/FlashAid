package com.sneha.flashaid;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.sneha.flashaid.adapters.HospitalAdapter;
import com.sneha.flashaid.models.HospitalModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NavHospitalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private Button btnRefresh;
    private static final int REQUEST_LOCATION_PERMISSION = 1;

    private double userLat = 0.0;
    private double userLon = 0.0;
    private List<HospitalModel> hospitalList = new ArrayList<>();
    private HospitalAdapter adapter;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(getPackageName());
        setContentView(R.layout.activity_nav_hospital);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        btnRefresh = findViewById(R.id.btnRefresh);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HospitalAdapter(this, hospitalList);
        recyclerView.setAdapter(adapter);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnRefresh.setOnClickListener(v -> getUserLocation());

        requestLocationPermission(); // First fetch
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getUserLocation();
        }
    }

    private void getUserLocation() {
        progressBar.setVisibility(android.view.View.VISIBLE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setWaitForAccurateLocation(true)
                .setMaxUpdates(1)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    Toast.makeText(NavHospitalActivity.this, "Unable to fetch location", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(android.view.View.GONE);
                    return;
                }

                Location location = locationResult.getLastLocation();
                if (location != null) {
                    userLat = location.getLatitude();
                    userLon = location.getLongitude();
                    fusedLocationClient.removeLocationUpdates(this);
                    new FetchHospitalsTask().execute();
                } else {
                    Toast.makeText(NavHospitalActivity.this, "Location not found", Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(android.view.View.GONE);
                }
            }
        }, getMainLooper());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchHospitalsTask extends AsyncTask<Void, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(Void... voids) {
            String query = String.format(Locale.US,
                    "[out:json];node[amenity=hospital](around:10000,%.6f,%.6f);out;",
                    userLat, userLon);

            String url = "https://overpass-api.de/api/interpreter?data=" + query;
            OkHttpClient client = new OkHttpClient();

            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    JSONObject jsonObject = new JSONObject(response.body().string());
                    return jsonObject.getJSONArray("elements");
                }
            } catch (IOException | org.json.JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONArray elements) {
            progressBar.setVisibility(android.view.View.GONE);

            if (elements == null) {
                Toast.makeText(NavHospitalActivity.this, "Failed to load hospitals", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                hospitalList.clear();
                for (int i = 0; i < elements.length(); i++) {
                    JSONObject hospital = elements.getJSONObject(i);
                    JSONObject tags = hospital.optJSONObject("tags");
                    if (tags == null) continue;

                    String name = tags.has("name") ? tags.getString("name") : "Unnamed Hospital";
                    String address = tags.has("addr:full") ? tags.getString("addr:full") :
                            tags.has("addr:street") ? tags.getString("addr:street") :
                                    "Address not available";

                    double lat = hospital.getDouble("lat");
                    double lon = hospital.getDouble("lon");

                    hospitalList.add(new HospitalModel(name, address, lat, lon));
                }

                adapter.notifyDataSetChanged();

                if (hospitalList.isEmpty()) {
                    Toast.makeText(NavHospitalActivity.this, "No hospitals found within 10 km", Toast.LENGTH_SHORT).show();
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(NavHospitalActivity.this, "Error parsing data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
