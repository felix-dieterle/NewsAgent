# Setup Guide / Einrichtungsanleitung

## Schnellstart

### 1. Projekt klonen

```bash
git clone https://github.com/felix-dieterle/NewsAgent.git
cd NewsAgent
```

### 2. Android Studio einrichten

1. **Android Studio installieren**
   - Laden Sie Android Studio herunter: https://developer.android.com/studio
   - Installieren Sie Android SDK 24 oder höher
   - Installieren Sie Kotlin Plugin (normalerweise vorinstalliert)

2. **Projekt öffnen**
   - Öffnen Sie Android Studio
   - Wählen Sie "Open an Existing Project"
   - Navigieren Sie zum NewsAgent Ordner
   - Warten Sie, bis Gradle synchronisiert ist

### 3. API-Schlüssel besorgen

#### News API (erforderlich)
1. Besuchen Sie https://newsapi.org
2. Registrieren Sie sich für ein kostenloses Konto
3. Kopieren Sie Ihren API-Schlüssel
4. **Gratis-Tier**: 100 Anfragen/Tag

#### OpenRouter API (für KI-Zusammenfassungen)
1. Besuchen Sie https://openrouter.ai
2. Erstellen Sie ein Konto
3. Gehen Sie zu "API Keys"
4. Erstellen Sie einen neuen API-Schlüssel
5. **Empfohlene Modelle**:
   - `google/gemini-flash-1.5` - Kostenlos
   - `meta-llama/llama-3.2-1b-instruct` - Sehr günstig
   - Siehe https://openrouter.ai/models für Preise

### 4. App konfigurieren

#### Option A: Über die App (empfohlen)
1. Bauen und installieren Sie die App
2. Öffnen Sie die App
3. Gehen Sie zu Einstellungen (Menü → Einstellungen)
4. Geben Sie Ihre API-Schlüssel ein:
   - News API Schlüssel
   - OpenRouter API Schlüssel
5. Konfigurieren Sie weitere Einstellungen:
   - Update-Intervall (Standard: 60 Minuten)
   - Benachrichtigungen aktivieren/deaktivieren
   - Automatische Zusammenfassungen
   - Glaubwürdigkeitsprüfung
6. Speichern Sie die Einstellungen

#### Option B: Vorab-Konfiguration
Erstellen Sie eine `local.properties` Datei im Projekt-Root (wird nicht committet):

```properties
# API Keys
news.api.key=IHR_NEWS_API_SCHLÜSSEL
openrouter.api.key=IHR_OPENROUTER_API_SCHLÜSSEL

# Optional: Credibility API
credibility.api.url=https://your-credibility-api.com/
credibility.api.key=IHR_CREDIBILITY_API_SCHLÜSSEL
```

Dann fügen Sie Code in `MainActivity.onCreate()` hinzu:

```kotlin
// In MainActivity.kt onCreate()
private fun initializeFromLocalProperties() {
    val properties = Properties()
    val localPropertiesFile = File(rootDir, "local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(FileInputStream(localPropertiesFile))
        
        val prefs = getSharedPreferences("newsagent_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            properties.getProperty("news.api.key")?.let {
                putString("news_api_key", it)
            }
            properties.getProperty("openrouter.api.key")?.let {
                putString("openrouter_api_key", it)
            }
            apply()
        }
    }
}
```

### 5. App bauen

#### Via Android Studio
1. Klicken Sie auf "Build" → "Make Project"
2. Warten Sie bis der Build abgeschlossen ist
3. Klicken Sie auf "Run" → "Run 'app'"

#### Via Kommandozeile
```bash
# Debug Build
./gradlew assembleDebug

# Release Build (signiert)
./gradlew assembleRelease

# Installieren auf verbundenem Gerät
./gradlew installDebug
```

### 6. App ausführen

#### Auf Emulator
1. Starten Sie AVD Manager in Android Studio
2. Erstellen Sie einen virtuellen Geräte (API 24+)
3. Starten Sie den Emulator
4. Klicken Sie auf "Run"

#### Auf echtem Gerät
1. Aktivieren Sie Developer-Optionen auf Ihrem Android-Gerät
2. Aktivieren Sie USB-Debugging
3. Verbinden Sie das Gerät via USB
4. Klicken Sie auf "Run"

## Erweiterte Konfiguration

### Anpassung der News-Quellen

In `NewsRepository.kt` können Sie weitere Parameter konfigurieren:

```kotlin
suspend fun fetchTopHeadlines(
    country: String = "de",  // Ändern für andere Länder
    category: String? = null // "business", "sports", etc.
): List<NewsArticle>
```

### Anpassung der KI-Modelle

In `AiSummaryService.kt`:

```kotlin
val request = ChatRequest(
    model = "google/gemini-flash-1.5", // Ändern Sie das Modell hier
    messages = listOf(...)
)
```

Verfügbare Modelle:
- `google/gemini-flash-1.5` - Schnell, kostenlos
- `google/gemini-pro-1.5` - Höhere Qualität
- `anthropic/claude-3-haiku` - Schnell, günstig
- `meta-llama/llama-3.2-3b-instruct` - Open Source

### Anpassung der Heuristik-Checks

In `CredibilityCheckService.kt` können Sie vertrauenswürdige Quellen anpassen:

```kotlin
private fun checkSourceReputation(source: String): Float {
    val reputableSources = listOf(
        // Fügen Sie weitere Quellen hinzu
        "tagesschau", "zdf", "ard", 
        // Ihre bevorzugten Quellen
    )
    // ...
}
```

## Fehlerbehebung

### Build-Fehler

**Problem**: "SDK location not found"
```bash
# Lösung: Erstellen Sie local.properties
echo "sdk.dir=/pfad/zu/android/sdk" > local.properties
```

**Problem**: "Gradle sync failed"
```bash
# Lösung: Löschen Sie Gradle-Cache
rm -rf .gradle
./gradlew clean
```

### Laufzeit-Fehler

**Problem**: "Keine Nachrichten werden geladen"
- Überprüfen Sie den News API Schlüssel in den Einstellungen
- Überprüfen Sie die Internetverbindung
- Prüfen Sie Logcat für Fehler: `adb logcat | grep NewsAgent`

**Problem**: "Zusammenfassungen werden nicht generiert"
- Überprüfen Sie den OpenRouter API Schlüssel
- Stellen Sie sicher, dass Sie Guthaben haben (prüfen Sie OpenRouter Dashboard)
- Aktivieren Sie "Automatische Zusammenfassungen" in den Einstellungen

**Problem**: "App stürzt beim Start ab"
```bash
# Prüfen Sie Logcat für Stack Trace
adb logcat -d > crash.log
# Suchen Sie nach AndroidRuntime oder FATAL EXCEPTION
```

## Entwicklung

### Code-Struktur
```
NewsAgent/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/newsagent/
│   │   │   │   ├── models/      # Datenmodelle
│   │   │   │   ├── api/         # API Interfaces
│   │   │   │   ├── services/    # Business Logic
│   │   │   │   └── ui/          # UI Components
│   │   │   ├── res/             # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # Unit Tests
│   └── build.gradle.kts
├── build.gradle.kts
└── settings.gradle.kts
```

### Logging aktivieren

Fügen Sie in `build.gradle.kts` hinzu:

```kotlin
buildTypes {
    debug {
        isDebuggable = true
        buildConfigField("boolean", "ENABLE_LOGGING", "true")
    }
}
```

### Tests ausführen

```bash
# Unit Tests
./gradlew test

# Instrumentierte Tests
./gradlew connectedAndroidTest
```

## Deployment

### Release Build erstellen

1. **Keystore erstellen**:
```bash
keytool -genkey -v -keystore newsagent.keystore -alias newsagent -keyalg RSA -keysize 2048 -validity 10000
```

2. **keystore.properties erstellen**:
```properties
storePassword=IHR_STORE_PASSWORT
keyPassword=IHR_KEY_PASSWORT
keyAlias=newsagent
storeFile=/pfad/zu/newsagent.keystore
```

3. **build.gradle.kts aktualisieren**:
```kotlin
android {
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = Properties()
                keystoreProperties.load(FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

4. **Release Build**:
```bash
./gradlew assembleRelease
```

Die signierte APK befindet sich in: `app/build/outputs/apk/release/`

### Play Store Deployment

1. Erstellen Sie ein Play Console Konto
2. Erstellen Sie eine neue App
3. Laden Sie die APK/AAB hoch
4. Füllen Sie Store-Listing aus
5. Veröffentlichen Sie die App

## Support

- **Dokumentation**: Siehe README.md und ARCHITECTURE.md
- **Issues**: https://github.com/felix-dieterle/NewsAgent/issues
- **Discussions**: https://github.com/felix-dieterle/NewsAgent/discussions

## Lizenz

Siehe LICENSE Datei für Details.
