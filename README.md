# Speed Overlay Pro

Ein hochpräziser, schwebender Geschwindigkeitsassistent für Android, der Echtzeit-Tempolimits aus OpenStreetMap (OSM) mit deiner aktuellen GPS-Geschwindigkeit kombiniert.

## ✨ Hauptmerkmale (v1.4)
- **Ultra-Low-Latency GPS:** Erhöhte Abtastrate (2.5 Hz) für verzögerungsfreie Geschwindigkeitsanzeige.
- **Smarte Schild-Vorschau (Pre-fetching):** Erweiterter Suchradius (150m) antizipiert kommende Tempolimits.
- **Sensor-Fusion (Neu):** Nutzt Beschleunigungssensor und Gyroskop für eine absolut stabile 0 km/h Anzeige im Stillstand (kein GPS-Wandern).
- **Zusatzschilder & Sicherheit:**
    - Anzeige von **Gefahrenstellen** und **Schulzonen**.
    - Spezielle Icons für **unbegrenzte** Autobahnabschnitte und **variable Schilderbrücken**.
    - Optionale **Blitzer-Warnung** (eigenverantwortlich aktivierbar).
- **Interaktives Overlay:**
    - **Ton-Status Icon:** Kleine Glocke direkt im Overlay zeigt an, ob die akustische Warnung aktiv ist.
    - **Mute per Long-Click:** Stummschalten der Audio-Warnung direkt auf dem Overlay mit visuellem Feedback (Flash).
- **Erweiterter Autostart:** Startet automatisch bei **Stromverbindung** (ideal für Head-Units/Fahrzeuge ohne Bluetooth).
- **Hardware-Optimiert:** Läuft auf allen Geräten von Android 9 (Head-Units) bis Android 14+.

## 🛠 Technische Highlights
- **Richtungsabhängige Logik:** Gleicht die Fahrtrichtung (Bearing) mit der Straßengeometrie ab, um falsche Limits auf Parallelstraßen zu vermeiden.
- **Automatisches UI-Sync:** Einstellungsänderungen am Overlay (z.B. Mute) werden in Echtzeit mit dem Dashboard synchronisiert.
- **Zentralisierte Konfiguration:** Alle Parameter über `Config.kt` steuerbar.
- **Architektur:** Sauber getrennt in `data`, `logic`, `service` und `ui` (mit eigenem `OverlayManager`).

## 🚀 Installation & Start
1. App öffnen und Berechtigungen erteilen.
2. In den Einstellungen "Start bei Stromverbindung" oder "Bluetooth" wählen.
3. Auf "Start Service" tippen.

## ⚖️ Rechtlicher Hinweis
Die Nutzung von Blitzer-Warnfunktionen ist in einigen Ländern während der Fahrt untersagt. Die Funktion ist standardmäßig deaktiviert und die Nutzung erfolgt auf eigene Gefahr.

## 🧪 Tests ausführen
Um die Integrität der Logik zu prüfen, führe folgenden Befehl aus:
```bash
./gradlew test
```
