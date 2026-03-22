# Speed Overlay

Ein intelligentes Android-Overlay, das das aktuelle Tempolimit basierend auf OpenStreetMap-Daten anzeigt.

## Features
- **Echtzeit-Anzeige**: Überlagert Navigations-Apps wie Google Maps.
- **OSM-Integration**: Erkennt Tempolimits basierend auf GPS-Koordinaten und OSM-Tags.
- **Sprachumschaltung**: Unterstützung für Deutsch, Englisch, Spanisch, Französisch und Italienisch. Die App nutzt moderne Android-APIs (LocaleConfig) für konsistente Lokalisierung.
- **EU-Regelwerk**: Implementiert implizite Tempolimits für alle EU-Mitgliedstaaten (z.B. 100 km/h auf deutschen Landstraßen, 80 km/h in Frankreich).
- **Intelligente Warnung**: Akustische und visuelle Warnungen bei Überschreitung (einstellbare Toleranz).
- **Automatisierung**:
    - **Bluetooth-Autostart**: Startet den Dienst automatisch bei Verbindung mit einem spezifischen, in den Einstellungen ausgewählten Bluetooth-Gerät.
    - **Strom-Autostart**: Startet die App automatisch beim Anschließen an eine Stromquelle.
- **Notification-Control**: Schnelles Stoppen des Dienstes direkt über die Benachrichtigung (optimiert für Android 13+).
- **Dark Mode**: Volle Unterstützung für systemweiten Dunkelmodus.

## Architektur
- **Jetpack Compose**: Moderne UI-Entwicklung.
- **AppCompat & DataStore**: Kombinierte Nutzung von `AppCompatDelegate` für Lokalisierung und Jetpack DataStore für Persistenz.
- **Hilt**: Dependency Injection für saubere Testbarkeit.
- **Service-Based**: Hintergrunddienst für stabilen Betrieb während der Navigation.

## Berechtigungen
Die App benötigt folgende Berechtigungen für den vollen Funktionsumfang:
- **Standort**: Zur Bestimmung der Position und des Tempolimits.
- **Overlay**: Zum Anzeigen der Geschwindigkeit über anderen Apps.
- **Bluetooth**: Für die Geräteauswahl beim Autostart (ab Android 12).
- **Benachrichtigungen**: Für die Dienst-Steuerung (ab Android 13).

## Installation & Tests
Führen Sie die Unit-Tests mit Gradle aus:
```bash
./gradlew test
```

Um spezifische Tests für die Einstellungen, Bluetooth-Logik und Lokalisierung auszuführen:
```bash
./gradlew :app:testDebugUnitTest --tests "com.drgreen.speedoverlay.data.SettingsManagerTest"
./gradlew :app:testDebugUnitTest --tests "com.drgreen.speedoverlay.service.BluetoothReceiverTest"
```

## Rechtliches
Die Nutzung erfolgt auf eigene Gefahr. Die App ersetzt nicht den aufmerksamen Blick auf echte Verkehrsschilder.
