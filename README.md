# Speed Overlay Pro

Ein hochpräziser, schwebender Geschwindigkeitsassistent für Android, der Echtzeit-Tempolimits aus OpenStreetMap (OSM) mit deiner aktuellen GPS-Geschwindigkeit kombiniert.

## ✨ Hauptmerkmale
- **Modernes Material 3 Dashboard:** Intuitive Steuerung im Card-Design mit voller Unterstützung für **Dunkelmodus (Dark Mode)**.
- **Mehrsprachigkeit (Internationalisierung):** Volle Unterstützung für **Deutsch, Englisch, Spanisch, Französisch und Italienisch**. Automatische Erkennung der Systemsprache beim ersten Start.
- **Verbesserte UI-Steuerung:** Auswahl von Sprache und Dark Mode über moderne **Material 3 Dropdown-Menüs** (Exposed Dropdowns).
- **Barrierefreiheit (Accessibility):** Volle TalkBack-Unterstützung durch optimierte Inhaltsbeschreibungen (Content Descriptions) für alle UI-Elemente.
- **Visuelle Anpassbarkeit:**
    - **Skalierbares Overlay:** Größe von 50% bis 200% anpassbar (inkl. automatischer Anpassung der Trefferfläche).
    - **Einstellbare Transparenz (Alpha):** Das Overlay kann für bessere Durchsicht semitransparent geschaltet werden.
    - **Abgestimmte Farbpalette:** Mathematisch harmonisierte Material 3 Farben für Light und Dark Mode.
- **Präzise Tempolimits:** Nutzt die Overpass API (OSM) mit intelligenter Richtungslogik.
- **Audio-Warnsystem:** Akustisches Signal bei Überschreitung des Limits (Toleranz konfigurierbar).
- **System-Integration:**
    - **Erweiterter Dark Mode:** Auswahl zwischen Systemstandard, permanentem Hell- oder Dunkel-Modus.
    - **Akku-Optimierung:** Integrierter Check und Verknüpfung zu den Systemeinstellungen für maximale Zuverlässigkeit im Hintergrund.
- **Smarte Automatisierung:**
    - **Bluetooth-Start:** Wähle spezifische Geräte (Anzeige mit Klarname), bei deren Verbindung das Overlay automatisch startet.
- **Stabilität:** Effektive Unterdrückung von GPS-Jitter im Stillstand (Tafel bleibt stabil bei 0 km/h).

## 🛠 Technische Highlights
- **Architektur:** Sauber getrennt in `data`, `logic`, `service` und `ui`.
- **Technologien:** Kotlin Coroutines, Retrofit 3, Fused Location Provider, Material 3, Robolectric & MockK für Unit Tests.
- **Testabdeckung:** Hohe Qualitätssicherung durch >90% Testabdeckung der Geschäftslogik.

## 🚀 Installation & Start
1. App öffnen und Berechtigungen für **Standort** und **Overlay** erteilen.
2. Gewünschte Toleranz, Einheit, Größe und Transparenz einstellen.
3. Bluetooth-Gerät für den Autostart auswählen (optional).
4. Auf "Start Service" tippen.

## 🧪 Tests ausführen
Um die Integrität der Logik zu prüfen, führe folgenden Befehl aus:
```bash
./gradlew test
```

## ⚖️ Sicherheit & Datenschutz (EU/DSGVO)
- **Haftungsausschluss:** Die Nutzung erfolgt auf eigene Gefahr. Tempolimits basieren auf OpenStreetMap-Daten und können veraltet oder fehlerhaft sein. Achten Sie immer vorrangig auf die physischen Straßenschilder.
- **Datenschutz:** Standortdaten werden lokal verarbeitet. Koordinaten werden anonymisiert zur Abfrage der Limits an die Overpass API gesendet. Es werden keine personenbezogenen Daten oder GPS-Protokolle auf externen Servern gespeichert.
- **Attribution:** Tempolimit-Daten © OpenStreetMap-Mitwirkende.
