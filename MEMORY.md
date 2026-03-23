# =============================================================================
# MEMORY.md - Speed Overlay Pro (v1.4)
# =============================================================================

## 🚗 Project Context
Android-App zur Anzeige von Echtzeit-Tempolimits (OSM) und GPS-Geschwindigkeit über anderen Apps.

### 🛠 Tech Stack (Summary)
- **Core:** Kotlin 2.1, Coroutines, Hilt (DI)
- **UI:** Jetpack Compose (M3), Overlay-Service
- **Data:** Retrofit 3 (Networking), Room (Logbook), DataStore (Settings)
- **Location:** Google Play Services (FusedLocationProvider)

### 🧠 Logic & Principles (v1.4)
- **Smart Logic:** Predictive Pre-fetching, Road Filtering, Junction Mode, Sensor-Fusion & Kalman-Filter.
- **Resilience:** Adaptive Battery Saver, Offline Resilience (100 Segments).
- **Compliance:** Automated Deviation Logbook (> 15 km/h).

### 🏷 Release History
- **v1.1 - v1.3:** Einführung von Compose UI, Predictive Logic, Battery Saver, Kalman Filter & Room Integration.
- **v1.4 (Current):** Stabilisierung & Maintenance Release. Optimierung der Codebase und Vorbereitung auf weitere Features.

### 🛠 Quality Gates
- Conventional Commits, Pre-commit Hooks, TDD/Unit-Tests (Robolectric & MockK).
