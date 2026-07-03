package com.sneha.flashaid.models;

public class RequestModel {
    public String requestId;
    private String patientName;
    private String age;
    private String contact;
    private String severity;
    private String symptoms;
    private String status;
    private String driverId;
    private String userId;
    private double latitude;
    private double longitude;
    private long timestamp;

    // ✅ Empty constructor (required for Firebase)
    public RequestModel() {}

    // ✅ Full constructor (optional)
    public RequestModel(String requestId, String patientName, String age, String contact,
                        String severity, String symptoms, String status,
                        String driverId, String userId,
                        double latitude, double longitude, long timestamp) {
        this.requestId = requestId;
        this.patientName = patientName;
        this.age = age;
        this.contact = contact;
        this.severity = severity;
        this.symptoms = symptoms;
        this.status = status;
        this.driverId = driverId;
        this.userId = userId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    // ✅ Public getters and setters
    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
