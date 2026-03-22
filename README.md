# Speed Overlay Pro (v1.3)

Ein hochpräziser, schwebender Geschwindigkeitsassistent für Android, der Echtzeit-Tempolimits aus OpenStreetMap (OSM) mit deiner aktuellen GPS-Geschwindigkeit kombiniert.

## ✨ Hauptmerkmale (v1.3)

### 🧠 Intelligente Datenverarbeitung
- **Predictive Pre-fetching:** Dynamischer Suchradius (bis zu 600m), der sich linear mit deiner Geschwindigkeit skaliert. Sieht bis zu 12 Sekunden in die Zukunft voraus.
- **Smart Road Filtering:** Fortgeschrittenes Scoring-Modell berücksichtigt Distanz, Fahrtrichtung (Heading) und Straßentyp, um Fehl-Anzeigen auf Parallelstraßen zu vermeiden.
- **Junction Mode:** Erhöhte Sensitivität an Kreuzungen (< 25 km/h) mit erweiterter Richtungstoleranz.
- **Offline Resilience:** Lokaler Cache für bis zu 100 Straßen-Segmente überbrückt Funklöcher von bis zu 10 Minuten.

### ⚡ Stillstand-Präzision (Neu in v1.3)
- **Kalman-Filter:** Mathematische Glättung der GPS-Geschwindigkeit zur Eliminierung von Rauschen und Sprüngen.
- **Sensor-Fusion:** Kombiniert Beschleunigungssensor und Gyroskop für eine absolut stabile 0 km/h Anzeige im Stillstand.
- **Accuracy Filtering:** Ignoriert GPS-Fixes mit schlechter Genauigkeit (> 25m), um "Wandern" in Häuserschluchten zu verhindern.

### 📓 Automatisches Logbuch
- **Abweichungsetappen:** Automatische Aufzeichnung von Fahrten, bei denen das Tempolimit signifikant überschritten wurde (> 15 km/h).
- **In-App Analyse:** Neues UI-Modul zum Einsehen und Verwalten der letzten 50 Etappen (Dauer, Max-Speed, Ø-Speed).
- **Smarte Finalisierung:** Intelligente Cooldown-Logik erkennt das Ende einer Etappe automatisch.

### 🛡 Sicherheit & Interaktion
- **Zusatzschilder:** Anzeige von Gefahrenstellen, Schulzonen und variablen Schilderbrücken.
- **Interaktives Overlay:** Ton-Status Anzeige und Mute per Long-Click.
- **Mehrsprachig:** Unterstützung für DE, EN, ES, FR, IT (dynamisch umschaltbar).

## 🚀 Installation & Start
1. App öffnen und Berechtigungen (Standort, Overlay) erteilen.
2. In den Einstellungen gewünschte Autostart-Optionen wählen.
3. Auf "Start Service" tippen.

## 🧪 Tests ausführen
Umfangreiche Unit-Tests für Logik und Datenhaltung:
```bash
./gradlew test
```

## ⚖️ Rechtlicher Hinweis
Die Nutzung von Blitzer-Warnfunktionen ist in einigen Ländern während der Fahrt untersagt. Die Funktion ist standardmäßig deaktiviert und die Nutzung erfolgt auf eigene Gefahr.
