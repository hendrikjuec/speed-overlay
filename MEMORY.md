# =============================================================================
# MEMORY.md - Speed Overlay Pro (v1.2)
# =============================================================================

## 🚗 Project Context
Android-App zur schwebenden Anzeige von Echtzeit-Tempolimits (OpenStreetMap/Overpass API) kombiniert mit GPS-Geschwindigkeit.

### 🛠 Tech Stack
- **Language:** Kotlin (1.9.x)
- **Concurrency:** Kotlin Coroutines (Dispatchers.IO, SupervisorJob)
- **Networking:** Retrofit 2 + Gson
- **Location:** Google Play Services FusedLocationProvider (Priority: High Accuracy)
- **Storage:** SharedPreferences (SettingsManager, LogManager)
- **UI:** Custom WindowManager Overlay (ConstraintLayout)
- **Architecture:** Layered Architecture (Data, Logic, Service, UI)

### 🔒 Security Context
- **Data Classification:** Public (OpenStreetMap data)
- **Secrets Management:** Keystore für API-Keys (falls vorhanden), aktuell keine harten Secrets im Code.
- **Privacy:** GPS-Daten werden nur lokal verarbeitet. Logbuch-Daten werden ausschließlich auf dem Gerät gespeichert.

### 🧠 Core Principles (v1.2)
1. **Predictive Pre-fetching:** Dynamischer Radius basierend auf Geschwindigkeit (`Radius = 100m + (Speed * 12s)`).
2. **Smart Road Filtering:** Gewichtung von Straßentypen (Motorway vs. Service) je nach Tempo.
3. **Junction Mode:** Erhöhte Heading-Toleranz (< 25 km/h) zur Antizipation von Abbiegevorgängen.
4. **Adaptive Battery Saver:** Drosselung von GPS und Sensoren bei Akku < 20% (nicht ladend).
5. **Offline Resilience:** Lokaler Puffer (100 Segmente) für Funklöcher.
6. **Automated Deviation Logbook:** Aufzeichnung signifikanter Tempoüberschreitungen (> 15 km/h) inkl. Metadaten.

### 🛠 Quality Gates
- Conventional Commits enforced.
- Pre-commit Hooks (Trailing-Whitespace, Large-Files, Branch-Protection).
- TDD für Core Logic (`SpeedProcessor`, `OsmParser`, `SpeedRepository`).

### 🏷 Release History
- **v1.1:** UI-Update & Motion Detector
- **v1.2 (Current):** Predictive Logic, Smart Filtering, Junction Mode, Battery Saver & Deviation Logbook
