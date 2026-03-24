# Speed Overlay

Ein intelligentes Android-Overlay und Widget, das das aktuelle Tempolimit basierend auf OpenStreetMap-Daten anzeigt. Optimiert für modernste Android-Versionen (14, 15 & 16).

## Features
- **Echtzeit-Overlay**: Überlagert Navigations-Apps wie Google Maps mit aktueller Geschwindigkeit und Limit.
- **Home-Screen Widget**: Konsistente Anzeige von Geschwindigkeit und Schildern direkt auf dem Launcher.
- **Intelligente OSM-Analyse**:
    - Erkennt explizite Schilder (maxspeed).
    - Berücksichtigt Zonen (30er Zonen, Spielstraßen).
    - Implementiert implizite Regeln (z.B. 50 innerorts, 100 außerorts in DE).
- **Visuelle Logik (Schilder)**:
    - **Roter Rand**: Verifiziertes Limit (Schilder erkannt).
    - **Grauer Rand**: Vermutetes Limit basierend auf Straßentyp (Confidence-Logik).
    - **Sonder-Icons**: Korrekte Darstellung für Unbegrenzt (Schild 282), Variable Limits und Innerorts (Haus-Icon).
- **Warnsystem**: Pulsierendes Overlay und akustische Beeps bei Überschreitung (einstellbare Toleranz).
- **Automatisierung**: Autostart bei Bluetooth-Verbindung, Stromzufuhr oder Geräteneustart.
- **Multilingual**: Volle Unterstützung für DE, EN, ES, FR, IT (inkl. In-App Language Switching).

## Architektur & Tech-Stack
- **Jetpack Compose**: Deklarative UI für Overlay und Einstellungen.
- **Foreground Service**: Stabiler Hintergrundbetrieb mit Android 14+ `location` Typ-Deklaration.
- **Kalman-Filter**: Hochpräzise Geschwindigkeitsglättung zur Vermeidung von GPS-Jitter.
- **Hilt & Room**: Saubere Dependency Injection und effizientes Offline-Caching von Straßendaten.
- **DataStore**: Reaktive Einstellungsverwaltung.

## Berechtigungen (Onboarding)
Die App führt den Nutzer durch ein sicheres Setup für:
- **Standort (Fein)**: Zwingend für die Geschwindigkeitsberechnung.
- **Overlay**: Erlaubt die Anzeige über anderen Apps.
- **Benachrichtigungen**: Erforderlich ab Android 13 für den Dienst-Status.
- **Akku-Optimierung**: Optional, empfohlen für unterbrechungsfreies Tracking.

## Entwicklung & Tests
Die Codebasis ist "Lint-clean" und für API 36 optimiert.
Unit-Tests decken den Algorithmus, das Repository und die UI-Provider ab:
```bash
./gradlew testDebugUnitTest
```

## Rechtliches
Die Nutzung erfolgt auf eigene Gefahr. Tempolimits basieren auf OSM-Daten und können von der Realität abweichen. Achten Sie immer auf physische Verkehrszeichen.
