# NewsAgent

Eine Android-App, die Sie in konfigurierbaren Intervallen über Nachrichten auf dem Laufenden hält und KI-gestützte Zusammenfassungen mit Glaubwürdigkeitsprüfung bereitstellt.

## Features

### ✨ Hauptfunktionen

- **📰 Nachrichtenaggregation**: Automatisches Abrufen von Top-Schlagzeilen aus verschiedenen Quellen
- **🤖 KI-Zusammenfassungen**: Automatische Generierung von Zusammenfassungen mit OpenRouter (kostenlose/günstige KI-Modelle)
- **🔊 Audio-Zusammenfassungen**: Text-to-Speech Unterstützung für das Anhören von Nachrichtenzusammenfassungen
- **✅ Glaubwürdigkeitsprüfung**: API-Schnittstelle zur Überprüfung der Seriosität und Integrität von Nachrichten
- **⏰ Konfigurierbare Updates**: Einstellbare Intervalle für automatische Nachrichtenupdates
- **🔔 Benachrichtigungen**: Push-Benachrichtigungen bei neuen Nachrichten
- **🎨 Material Design UI**: Moderne und benutzerfreundliche Oberfläche
- **🔍 Erweiterte Fehlerdiagnose**: 
  - Umfassendes Logging-System mit persistenten Log-Dateien
  - Log-Viewer direkt in der App
  - Logs teilen für Support-Anfragen
  - Globaler Crash-Handler für bessere Fehlerbehebung

## Architektur

### Komponenten

#### 📁 Models (`models/`)
- `NewsArticle.kt` - Datenmodell für Nachrichtenartikel
- `NewsSummary.kt` - KI-generierte Zusammenfassungen
- `CredibilityScore.kt` - Glaubwürdigkeitsbewertung
- `NewsUpdateConfig.kt` - Konfigurationseinstellungen

#### 🌐 API Interfaces (`api/`)
- `NewsApi.kt` - Integration mit News API (newsapi.org)
- `OpenRouterApi.kt` - KI-Integration über OpenRouter
- `CredibilityApi.kt` - Schnittstelle für Glaubwürdigkeitsprüfung

#### 🔧 Services (`services/`)
- `NewsRepository.kt` - Verwaltung von Nachrichtendaten
- `AiSummaryService.kt` - KI-Zusammenfassungsgenerierung
- `CredibilityCheckService.kt` - Glaubwürdigkeitsprüfung (Heuristik + API)
- `TextToSpeechService.kt` - Audio-Zusammenfassungen
- `NewsUpdateWorker.kt` - Hintergrund-Worker für periodische Updates

#### 🎨 UI (`ui/`)
- `MainActivity.kt` - Hauptansicht mit Nachrichtenliste
- `NewsDetailActivity.kt` - Detailansicht mit Zusammenfassung und Audio
- `SettingsActivity.kt` - Einstellungen und Konfiguration
- `NewsAdapter.kt` - RecyclerView Adapter

## Setup & Installation

### Voraussetzungen

1. Android Studio Arctic Fox oder neuer
2. Android SDK 24 (Android 7.0) oder höher
3. Kotlin 1.9.20

### API-Schlüssel

Die App benötigt zwei API-Schlüssel:

#### 1. News API
- Registrieren Sie sich auf [newsapi.org](https://newsapi.org)
- Holen Sie sich einen kostenlosen API-Schlüssel
- Geben Sie den Schlüssel in den App-Einstellungen ein

#### 2. OpenRouter API
- Registrieren Sie sich auf [openrouter.ai](https://openrouter.ai)
- Holen Sie sich einen API-Schlüssel
- Kostenlose/günstige Modelle verfügbar:
  - `google/gemini-flash-1.5` (Standard, kostenlos)
  - `meta-llama/llama-3.2-1b-instruct` (sehr günstig)
  - `anthropic/claude-instant-v1` (günstig)

### Build & Run

```bash
# Repository klonen
git clone https://github.com/felix-dieterle/NewsAgent.git
cd NewsAgent

# Projekt in Android Studio öffnen
# Oder via Kommandozeile:
./gradlew assembleDebug

# App installieren
./gradlew installDebug
```

### CI/CD Pipeline

Das Projekt verwendet GitHub Actions für automatisches Bauen und Veröffentlichen:

- **CI (Continuous Integration):** Automatische Builds und Tests bei jedem Pull Request
- **CD (Continuous Deployment):** Automatische APK-Releases bei jedem Merge in `main`

Weitere Details finden Sie in [CI_CD_DOCUMENTATION.md](CI_CD_DOCUMENTATION.md).

## Verwendung

### Erste Schritte

1. **App öffnen** und zu den Einstellungen navigieren
2. **API-Schlüssel eingeben**:
   - News API Schlüssel
   - OpenRouter API Schlüssel
3. **Update-Intervall konfigurieren** (Standard: 60 Minuten)
4. **Features aktivieren/deaktivieren**:
   - Benachrichtigungen
   - Automatische Zusammenfassungen
   - Glaubwürdigkeitsprüfung
5. **Einstellungen speichern**

### Features nutzen

#### Nachrichten anzeigen
- Hauptbildschirm zeigt aktuelle Nachrichten
- Tippen Sie auf das Aktualisierungssymbol für manuelle Updates
- Artikel werden mit Glaubwürdigkeitsbewertung angezeigt

#### Artikel-Details
- Tippen Sie auf einen Artikel für Details
- Sehen Sie die KI-Zusammenfassung und wichtige Punkte
- Nutzen Sie "Zusammenfassung anhören" für Audio-Wiedergabe
- Überprüfen Sie die Glaubwürdigkeitsbewertung

#### Hintergrund-Updates
- App ruft automatisch neue Nachrichten ab (basierend auf Intervall)
- Benachrichtigungen informieren über neue Artikel
- WorkManager stellt zuverlässige Updates sicher

## Glaubwürdigkeitsprüfung

### API-Integration
Die App bietet eine Schnittstelle zur Integration mit Fact-Checking-APIs. Konfigurieren Sie die URL in den Einstellungen:
```
credibility_api_url: https://your-credibility-api.com/
```

### Heuristische Prüfung
Wenn keine API verfügbar ist, nutzt die App heuristische Methoden:

- **Quellenreputation**: Prüft bekannte seriöse deutsche Nachrichtenquellen
- **Sensationalismus**: Erkennt reißerische Sprache
- **Autorenangabe**: Überprüft ob ein Autor genannt ist
- **Bewertung**: Kombinierte Punktzahl von 0.0 bis 1.0

Bekannte vertrauenswürdige Quellen:
- Tagesschau, ZDF, ARD
- Spiegel, Zeit, FAZ, Süddeutsche
- Handelsblatt, Tagesspiegel, Welt
- DPA (Deutsche Presse-Agentur)

## Technische Details

### Verwendete Technologien

- **Kotlin** - Hauptprogrammiersprache
- **Android Jetpack**:
  - WorkManager - Hintergrund-Tasks
  - Lifecycle - Lifecycle-aware components
  - RecyclerView - Effiziente Listen
- **Retrofit** - HTTP Client
- **Gson** - JSON Parsing
- **Coroutines** - Asynchrone Programmierung
- **Material Design** - UI Components

### Architektur-Muster

- **Repository Pattern** - Datenverwaltung
- **Service Layer** - Business-Logik
- **MVVM-Light** - Trennung von UI und Logik

### Berechtigungen

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

## Konfiguration

### SharedPreferences Schlüssel

```kotlin
news_api_key                  // News API Schlüssel
openrouter_api_key           // OpenRouter API Schlüssel
credibility_api_url          // Credibility API URL (optional)
update_interval_minutes      // Update-Intervall (Standard: 60)
enable_notifications         // Benachrichtigungen (Standard: true)
enable_auto_summary          // Auto-Zusammenfassungen (Standard: true)
enable_credibility_check     // Glaubwürdigkeitsprüfung (Standard: true)
max_articles                 // Max. Artikel pro Update (Standard: 10)
country                      // Ländercode (Standard: "de")
language                     // Sprachcode (Standard: "de")
```

## Erweiterungsmöglichkeiten

### Geplante Features

- [ ] Offline-Speicherung mit Room Database
- [ ] Mehrere Nachrichtenquellen
- [ ] Kategoriefilter (Politik, Sport, Wirtschaft, etc.)
- [ ] Favoriten und Lesezeichen
- [ ] Export von Zusammenfassungen
- [ ] Widget für Homescreen
- [ ] Dark Mode Support
- [ ] Mehrsprachige Unterstützung

### API-Erweiterungen

Die App ist vorbereitet für Integration mit:
- Fact-checking APIs (z.B. ClaimBuster, FactMata)
- Verschiedene News-Aggregatoren
- Weitere KI-Anbieter neben OpenRouter

## Fehlerbehebung

### Installation & Logging

Wenn die App nicht installiert werden kann oder abstürzt:
- Siehe [TROUBLESHOOTING_LOGGING.md](TROUBLESHOOTING_LOGGING.md) für detaillierte Hilfe
- Nutzen Sie die **Log-Viewer-Funktion** in den Einstellungen
- Logs können über "Logs teilen" für Support exportiert werden

### Keine Nachrichten werden geladen
- Überprüfen Sie den News API Schlüssel in den Einstellungen
- Prüfen Sie die Internetverbindung
- Überprüfen Sie das API-Limit (News API: 100 Anfragen/Tag im kostenlosen Tier)
- Überprüfen Sie die Logs in den Einstellungen für Details

### Zusammenfassungen werden nicht generiert
- Überprüfen Sie den OpenRouter API Schlüssel
- Stellen Sie sicher, dass "Automatische Zusammenfassungen" aktiviert ist
- Prüfen Sie das API-Guthaben bei OpenRouter
- Überprüfen Sie die Logs in den Einstellungen für Details

### Keine Benachrichtigungen
- Aktivieren Sie Benachrichtigungen in den App-Einstellungen
- Prüfen Sie Android-Systemeinstellungen für App-Benachrichtigungen
- Stellen Sie sicher, dass das Update-Intervall nicht zu lang ist

## Datenschutz

- Die App speichert API-Schlüssel lokal auf dem Gerät
- Keine Nachrichtendaten werden dauerhaft gespeichert (nur im Speicher)
- Alle API-Anfragen gehen direkt an die jeweiligen Dienste
- Keine Daten werden an Dritte weitergegeben

## Lizenz

Dieses Projekt ist Open Source. Siehe LICENSE-Datei für Details.

## Mitwirken

Beiträge sind willkommen! Bitte:
1. Forken Sie das Repository
2. Erstellen Sie einen Feature-Branch
3. Committen Sie Ihre Änderungen
4. Pushen Sie zum Branch
5. Erstellen Sie einen Pull Request

## Support

Bei Fragen oder Problemen:
- Erstellen Sie ein Issue auf GitHub
- Kontaktieren Sie den Entwickler

## Credits

Entwickelt von Felix Dieterle

Verwendet folgende Services:
- [News API](https://newsapi.org) für Nachrichtenaggregation
- [OpenRouter](https://openrouter.ai) für KI-Zusammenfassungen
- Android Text-to-Speech für Audio-Features

---

**Version**: 1.0.0  
**Letzte Aktualisierung**: Januar 2026