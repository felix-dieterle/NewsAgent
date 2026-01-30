# NewsAgent

Eine Android-App, die Sie in konfigurierbaren Intervallen über Nachrichten auf dem Laufenden hält und KI-gestützte Zusammenfassungen mit Glaubwürdigkeitsprüfung bereitstellt.

## Features

### ✨ Hauptfunktionen

- **📰 Nachrichtenaggregation**: Automatisches Abrufen von Top-Schlagzeilen aus verschiedenen Quellen
- **🆓 Kostenlose Nachrichtensuche**: 
  - RSS-basierte Suche (100% kostenlos, keine API-Keys): Tagesschau, Heise, Spiegel, Zeit
  - GNews API (100 Anfragen/Tag, einfache Registrierung)
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
- **⚡ Performance-Optimierungen**:
  - Multi-Level Caching (HTTP + Anwendungsebene)
  - Intelligente Rate-Limitierung für API-Schutz
  - Parallele Artikelverarbeitung
  - 60-70% Reduzierung der API-Kosten
  - Bis zu 80% schnellere Artikelverarbeitung

## Architektur

### Komponenten

#### 📁 Models (`models/`)
- `NewsArticle.kt` - Datenmodell für Nachrichtenartikel
- `NewsSummary.kt` - KI-generierte Zusammenfassungen
- `CredibilityScore.kt` - Glaubwürdigkeitsbewertung
- `NewsUpdateConfig.kt` - Konfigurationseinstellungen

#### 🌐 API Interfaces (`api/`)
- `NewsApi.kt` - Integration mit News API (newsapi.org)
- `FreeNewsApi.kt` - Integration mit GNews API (kostenlos, einfache Registrierung)
- `RssFeedParser.kt` - RSS-Feed-Parser (100% kostenlos, keine Registrierung)
- `OpenRouterApi.kt` - KI-Integration über OpenRouter
- `CredibilityApi.kt` - Schnittstelle für Glaubwürdigkeitsprüfung

#### 🔧 Services (`services/`)
- `NewsRepository.kt` - Verwaltung von Nachrichtendaten mit Caching
- `AiSummaryService.kt` - KI-Zusammenfassungsgenerierung mit Rate-Limiting
- `CredibilityCheckService.kt` - Glaubwürdigkeitsprüfung (Heuristik + API)
- `TextToSpeechService.kt` - Audio-Zusammenfassungen
- `NewsUpdateWorker.kt` - Hintergrund-Worker für periodische Updates

#### 🛠️ Utilities
- `cache/CacheManager.kt` - Multi-Level-Caching mit TTL-Verwaltung
- `utils/RateLimiter.kt` - API-Quota-Schutz und Kostenoptimierung
- `utils/Logger.kt` - Umfassendes Logging-System

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

### API-Token

Die App benötigt je nach gewählter Nachrichtenquelle einen oder zwei API-Token:

#### 1. Nachrichtenquelle wählen
In den Einstellungen wählen Sie zunächst Ihre bevorzugte Nachrichtenquelle:
- **NewsAPI.org**: 100 Anfragen/Tag kostenlos, registrieren auf [newsapi.org](https://newsapi.org)
- **GNews.io**: 100 Anfragen/Tag kostenlos, registrieren auf [gnews.io](https://gnews.io)
- **RSS Feeds**: 100% kostenlos, keine Registrierung erforderlich

#### 2. News API Token
- Wenn Sie NewsAPI.org wählen: Geben Sie Ihren NewsAPI.org Token ein
- Wenn Sie GNews.io wählen: Geben Sie Ihren GNews.io Token ein
- Wenn Sie RSS Feeds wählen: Kein Token erforderlich
- **Wichtig**: Der Token muss von der gewählten Quelle stammen!

#### 3. AI API Token
- Registrieren Sie sich auf [openrouter.ai](https://openrouter.ai)
- Holen Sie sich einen API-Token für KI-Zusammenfassungen
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
  - Debug APKs werden als Artifacts hochgeladen und können direkt installiert werden
- **CD (Continuous Deployment):** Automatische APK-Releases bei jedem Merge in `main`
  - Signierte APKs werden in GitHub Releases veröffentlicht
  - APKs sind direkt auf Android-Geräten installierbar

#### APK-Downloads

Sie können vorkompilierte APKs von diesem Repository herunterladen:

1. **Aus Workflow-Artifacts** (neueste Builds):
   - Gehen Sie zu "Actions" → Wählen Sie einen erfolgreichen Workflow
   - Laden Sie das "debug-apk" Artifact herunter
   - Entpacken Sie die ZIP-Datei und installieren Sie die APK

2. **Aus Releases** (offizielle Versionen):
   - Gehen Sie zu "Releases" → Wählen Sie die neueste Version
   - Laden Sie die APK-Datei herunter
   - Installieren Sie sie direkt auf Ihrem Android-Gerät

**Hinweis:** Bei der Installation müssen Sie möglicherweise "Installation aus unbekannten Quellen" in den Android-Einstellungen erlauben.

Weitere Details finden Sie in [CI_CD_DOCUMENTATION.md](CI_CD_DOCUMENTATION.md).

## Verwendung

### Erste Schritte

1. **App öffnen** und zu den Einstellungen navigieren
2. **Nachrichtenquelle wählen**:
   - NewsAPI.org (benötigt Token)
   - GNews.io (benötigt Token)
   - RSS Feeds (kostenlos, kein Token)
3. **API-Token eingeben**:
   - News API Token (für gewählte Quelle, außer RSS)
   - AI API Token (OpenRouter für KI-Zusammenfassungen)
4. **Update-Intervall konfigurieren** (Standard: 60 Minuten)
5. **Features aktivieren/deaktivieren**:
   - Benachrichtigungen
   - Automatische Zusammenfassungen
   - Glaubwürdigkeitsprüfung
6. **Einstellungen speichern**

### Features nutzen

#### Nachrichten anzeigen
- Hauptbildschirm zeigt aktuelle Nachrichten
- Tippen Sie auf das Aktualisierungssymbol für manuelle Updates
- Artikel werden mit Glaubwürdigkeitsbewertung angezeigt

#### Kostenlose Nachrichtensuche
- **100% Kostenlos (RSS)**: Nutzen Sie "RSS Nachrichten" im Menü für völlig kostenlose Nachrichten
  - Keine Registrierung erforderlich
  - Keine API-Schlüssel benötigt
  - Verwendet öffentliche RSS-Feeds (Tagesschau, Heise, Spiegel, Zeit)
  - Suche in RSS-Feeds über die Suchleiste
- **GNews API**: Alternative kostenlose Suchfunktion über das Menü "Kostenlose Suche"
  - 100 kostenlose Suchanfragen pro Tag
  - Einfache Registrierung bei GNews.io
  - Erweiterte Suchfunktionen

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
Die App bietet eine Schnittstelle zur Integration mit Fact-Checking-APIs. Die Standardkonfiguration verwendet:
```
credibility_api_url: https://newscheck123.de/
```
*Hinweis: Diese API ist noch nicht online. Die App verwendet heuristische Prüfungen als Fallback.*

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
news_source                  // Gewählte Nachrichtenquelle: "newsapi", "gnews", oder "rss"
news_api_token               // News API Token (gespeichert unabhängig von der Quelle)
news_api_key                 // NewsAPI.org Token (nur wenn newsapi gewählt)
gnews_api_token              // GNews.io Token (nur wenn gnews gewählt)
openrouter_api_key           // OpenRouter API Key für AI-Zusammenfassungen
credibility_api_url          // Credibility API URL (Standard: https://newscheck123.de/)
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
- Überprüfen Sie, dass Sie die richtige **Nachrichtenquelle** in den Einstellungen gewählt haben
- Stellen Sie sicher, dass Ihr News API Token von der **gewählten Quelle** stammt:
  - NewsAPI.org Token funktioniert nur mit NewsAPI.org
  - GNews.io Token funktioniert nur mit GNews.io
- Für RSS Feeds wird kein Token benötigt
- Prüfen Sie die Internetverbindung
- Überprüfen Sie das API-Limit (NewsAPI.org/GNews.io: 100 Anfragen/Tag)
- Überprüfen Sie die Logs in den Einstellungen für Details

### Zusammenfassungen werden nicht generiert
- Überprüfen Sie den AI API Token (OpenRouter) in den Einstellungen
- Stellen Sie sicher, dass "Automatische Zusammenfassungen" aktiviert ist
- Prüfen Sie das API-Guthaben bei OpenRouter
- Überprüfen Sie die Logs in den Einstellungen für Details

### Keine Benachrichtigungen
- Aktivieren Sie Benachrichtigungen in den App-Einstellungen
- Prüfen Sie Android-Systemeinstellungen für App-Benachrichtigungen
- Stellen Sie sicher, dass das Update-Intervall nicht zu lang ist

## Datenschutz

- Die App speichert API-Token lokal auf dem Gerät (verschlüsselt durch Android)
- Caching erfolgt nur im Speicher (nicht persistent)
- Alle API-Anfragen gehen direkt an die jeweiligen Dienste
- Keine Daten werden an Dritte weitergegeben
- Die gewählte Nachrichtenquelle bestimmt, welcher Dienst verwendet wird:
  - NewsAPI.org: Anfragen gehen an newsapi.org
  - GNews.io: Anfragen gehen an gnews.io
  - RSS: Anfragen gehen an öffentliche RSS-Feeds (Tagesschau, Heise, etc.)

## Performance & Kosteneffizienz

Die App ist für maximale Effizienz und minimale Kosten optimiert:

- **Multi-Level Caching**: Reduziert API-Aufrufe um 60-70%
- **Rate-Limiting**: Schützt vor versehentlicher Quota-Überschreitung
- **Parallele Verarbeitung**: Bis zu 80% schnellere Artikelverarbeitung
- **Intelligente TTLs**: Artikel (15 Min), Zusammenfassungen (24h), Glaubwürdigkeit (24h)

Siehe [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) für Details.

## Dokumentation

- [ARCHITECTURE.md](ARCHITECTURE.md) - Technische Architektur und Komponenten
- [PERFORMANCE_OPTIMIZATION.md](PERFORMANCE_OPTIMIZATION.md) - Performance-Verbesserungen
- [CI_CD_DOCUMENTATION.md](CI_CD_DOCUMENTATION.md) - CI/CD Pipeline Details
- [.github/copilot-instructions.md](.github/copilot-instructions.md) - Entwicklungs-Guidelines

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