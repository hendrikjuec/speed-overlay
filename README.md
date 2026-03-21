# Speed Overlay Pro (v1.2)

Ein hochpräziser, schwebender Geschwindigkeitsassistent für Android, der Echtzeit-Tempolimits aus OpenStreetMap (OSM) mit deiner aktuellen GPS-Geschwindigkeit kombiniert.

## ✨ Hauptmerkmale (v1.2)

### 🧠 Intelligente Datenverarbeitung
- **Predictive Pre-fetching:** Dynamischer Suchradius (bis zu 600m), der sich linear mit deiner Geschwindigkeit skaliert. Sieht bis zu 12 Sekunden in die Zukunft voraus.
- **Smart Road Filtering:** Gewichtet Straßentypen (z.B. Autobahn vs. Wohngebiet) basierend auf der aktuellen Geschwindigkeit, um Fehl-Anzeigen auf Parallelstraßen zu vermeiden.
- **Junction Mode:** Erhöhte Sensitivität an Kreuzungen (< 25 km/h), um Abbiegevorgänge zu antizipieren und das neue Limit sofort zu erfassen.
- **Offline Resilience:** Lokaler Cache für bis zu 100 Straßen-Segmente überbrückt Funklöcher von bis zu 10 Minuten.

### ⚡ Performance & Akku
- **Adaptive Battery Saver:** Drosselt GPS-Intervalle und pausiert Sensoren bei niedrigem Akkustand (< 20%) automatisch (einstellbar).
- **Sensor-Fusion:** Nutzt Beschleunigungssensor und Gyroskop für eine absolut stabile 0 km/h Anzeige im Stillstand (kein GPS-Wandern).
- **Ultra-Low-Latency GPS:** Erhöhte Abtastrate für verzögerungsfreie Anzeige.

### 🛡 Sicherheit & Interaktion
- **Zusatzschilder:** Anzeige von Gefahrenstellen, Schulzonen, unbegrenzten Abschnitten und variablen Schilderbrücken.
- **Interaktives Overlay:** Ton-Status Anzeige und Mute per Long-Click mit visuellem Feedback (Flash).
- **Autostart:** Startet automatisch bei Bluetooth-Verbindung oder Stromverbindung (ideal für Head-Units).

## 🛠 Architektur & Technik
- **Stack:** Kotlin, Coroutines, Retrofit, FusedLocationProvider.
- **Modularität:** Sauber getrennt in `data`, `logic`, `service` und `ui`.
- **Anpassbarkeit:** Alle Parameter zentral in `Config.kt` steuerbar.

## 🚀 Installation & Start
1. App öffnen und Berechtigungen (Standort, Overlay) erteilen.
2. In den Einstellungen gewünschte Autostart-Optionen wählen.
3. Auf "Start Service" tippen.

## 🧪 Tests ausführen
Die Core-Logik ist durch Unit-Tests in `SpeedProcessorTest` und `SpeedRepositoryTest` abgesichert:
```bash
./gradlew test
```

## ⚖️ Rechtlicher Hinweis
Die Nutzung von Blitzer-Warnfunktionen ist in einigen Ländern während der Fahrt untersagt. Die Funktion ist standardmäßig deaktiviert und die Nutzung erfolgt auf eigene Gefahr.
