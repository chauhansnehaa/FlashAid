# 🚑 FlashAid

FlashAid is an Android app that connects patients in an emergency with nearby ambulance drivers in real time. Patients can raise an emergency request with their location and symptoms, nearby drivers can accept the request, and both sides can track each other live on a map until the patient reaches the hospital.

## Features

- **Two user roles** — sign in as a **Patient/User** or an **Ambulance Driver** from a single login screen (Firebase Authentication).
- **Emergency requests** — patients submit their name, age, contact number, symptoms, and severity, along with their current GPS location.
- **Driver dashboard** — drivers go "live" to receive nearby emergency requests and accept/reject them.
- **Live GPS tracking** — a foreground location service streams the driver's position to Firebase Realtime Database so the patient can track the ambulance's live location on an OpenStreetMap view.
- **Nearby hospitals** — look up nearby hospitals with directions on the map.
- **Trip / request history** — patients and drivers can review past trips and requests.
- **Push-style status updates & notifications** for ride status changes.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| Platform | Android (minSdk 26, targetSdk/compileSdk 36) |
| Auth & Realtime data | Firebase Authentication, Firebase Realtime Database |
| Maps | [osmdroid](https://github.com/osmdroid/osmdroid) (OpenStreetMap) |
| Location | Google Play Services Location (FusedLocationProviderClient) |
| Networking | OkHttp |
| Build system | Gradle (Kotlin DSL) |

## Project Structure

```
FlashAid/
├── app/
│   ├── src/main/java/com/sneha/flashaid/
│   │   ├── LoginActivity.java            # Role-based sign in / sign up
│   │   ├── UserDashboardActivity.java    # Patient home screen
│   │   ├── PatientRequestActivity.java   # Raise an emergency request
│   │   ├── UserLiveTrackingActivity.java # Patient's live view of assigned ambulance
│   │   ├── DriverDashboardActivity.java  # Driver home screen ("go live")
│   │   ├── NavRequestsActivity.java      # Incoming requests for drivers
│   │   ├── LiveTrackingActivity.java     # Driver's live navigation view
│   │   ├── TrackingService.java          # Foreground service streaming GPS location
│   │   ├── NavHospitalActivity.java      # Nearby hospitals
│   │   ├── NavHistoryActivity.java       # Trip/request history
│   │   ├── adapters/                     # RecyclerView adapters
│   │   └── models/                       # Data models (RequestModel, HospitalModel)
│   └── src/main/res/                     # Layouts, drawables, strings, themes
└── build.gradle.kts / settings.gradle.kts
```

## Getting Started

### Prerequisites

- Android Studio (Koala or newer recommended)
- JDK 11
- A Firebase project with **Authentication** (Email/Password) and **Realtime Database** enabled

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/<your-username>/FlashAid.git
   cd FlashAid
   ```

2. **Add your Firebase config**
   This repo does not include `google-services.json` since it contains project-specific keys. Create your own Firebase project at the [Firebase Console](https://console.firebase.google.com/), register an Android app with package name `com.sneha.flashaid`, download the generated `google-services.json`, and place it at:
   ```
   app/google-services.json
   ```
   A template with the expected fields is provided at `app/google-services.json.example`.

3. **Open in Android Studio**
   Open the project root, let Gradle sync, and run on an emulator or device (minSdk 26+).

4. **Permissions**
   The app requests fine/coarse/background location, foreground service, and notification permissions at runtime — grant these for live tracking to work correctly.

## Notes

- Maps are rendered with `osmdroid`, so no Google Maps API key is required.
- Live tracking is powered by a foreground `TrackingService` that writes location updates to Firebase Realtime Database, which both the patient and driver apps listen to.

## License

No license has been specified yet. Add a `LICENSE` file (e.g. MIT) if you'd like others to be able to reuse this code.
