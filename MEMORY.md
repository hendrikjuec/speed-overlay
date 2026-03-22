# =============================================================================
# MEMORY.md - Speed Overlay Pro (v1.3)
# =============================================================================

## 🚗 Project Context
Android-App zur schwebenden Anzeige von Echtzeit-Tempolimits (OpenStreetMap/Overpass API) kombiniert mit GPS-Geschwindigkeit.

### 🛠 Tech Stack
- **Language:** Kotlin (2.1.0)
- **Concurrency:** Kotlin Coroutines (Dispatchers.IO, SupervisorJob)
- **Networking:** Retrofit 3 + Gson
- **Location:** Google Play Services FusedLocationProvider (Priority: High Accuracy)
- **Storage:** Room Database (Logbook), SharedPreferences (Settings)
- **UI:** Jetpack Compose (Modern Material 3 Design)
- **Architecture:** MVVM + Clean Architecture (Data, Logic, Service, UI)
- **Dependency Injection:** Hilt / Dagger

### 🔒 Security Context
- **Data Classification:** Public (OpenStreetMap data)
- **Secrets Management:** Keystore für API-Keys (falls vorhanden), aktuell keine harten Secrets im Code.
- **Privacy:** GPS-Daten werden nur lokal verarbeitet. Logbuch-Daten werden ausschließlich auf dem Gerät gespeichert.

### 🧠 Core Principles (v1.3)
1. **Predictive Pre-fetching:** Dynamischer Radius basierend auf Geschwindigkeit (`Radius = 100m + (Speed * 12s)`).
2. **Smart Road Filtering:** Gewichtung von Straßentypen (Motorway vs. Service) je nach Tempo.
3. **Junction Mode:** Erhöhte Heading-Toleranz (< 25 km/h) zur Antizipation von Abbiegevorgängen.
4. **Adaptive Battery Saver:** Drosselung von GPS und Sensoren bei Akku < 20% (nicht ladend).
5. **Offline Resilience:** Lokaler Puffer (100 Segmente) für Funklöcher.
6. **Automated Deviation Logbook:** Aufzeichnung signifikanter Tempoüberschreitungen (> 15 km/h) inkl. Metadaten (Room DB).
7. **Stillstand-Präzision:** Kalman-Filter (1D) zur Glättung + Sensor-Fusion (Gyro/Accel) für stabile 0 km/h Anzeige.

### 🛠 Quality Gates
- Conventional Commits enforced.
- Pre-commit Hooks (Trailing-Whitespace, Large-Files, Branch-Protection).
- TDD für Core Logic (`SpeedProcessor`, `OsmParser`, `SpeedRepository`).
- Unit-Tests mit Robolectric & MockK.

### 🏷 Release History
- **v1.1:** UI-Update & Motion Detector
- **v1.2:** Predictive Logic, Smart Filtering, Junction Mode, Battery Saver & Deviation Logbook
- **v1.3 (Current):** Kalman Filter, Sensor Fusion, Jetpack Compose UI, Room DB Integration, Refactored Core Logic
