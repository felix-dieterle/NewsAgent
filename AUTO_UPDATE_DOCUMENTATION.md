# Auto-Update Feature Documentation

## Overview

Die NewsAgent Android-App verfügt über eine automatische Update-Funktion, die beim Start der App nach neuen Versionen auf GitHub sucht und diese dem Benutzer anbietet.

## Funktionsweise

### 1. Update-Prüfung beim Start

- Die App prüft beim Start automatisch auf neue Versionen
- Die Prüfung erfolgt maximal einmal alle 20 Minuten
- Updates werden von der offiziellen GitHub Releases Seite geladen

### 2. GitHub Integration

Die App nutzt die GitHub API um:
- Die neueste veröffentlichte Version zu ermitteln
- Versionsnummern zu vergleichen
- APK-Dateien herunterzuladen

**GitHub API Endpoint:** `https://api.github.com/repos/felix-dieterle/NewsAgent/releases/latest`

### 3. Benutzer-Dialog

Wenn ein Update verfügbar ist, wird ein Dialog angezeigt mit:
- Aktueller und neuer Versionsnummer
- Release Notes (erste 200 Zeichen)
- Drei Optionen:
  - **Aktualisieren**: Startet den Download
  - **Später**: Verschiebt das Update auf später
  - **Diese Version überspringen**: Ignoriert diese Version dauerhaft

### 4. Download und Installation

- Download erfolgt über Android DownloadManager
- Fortschritt wird in der Benachrichtigungsleiste angezeigt
- Nach dem Download wird die APK-Installation automatisch gestartet
- Benutzer muss die Installation manuell bestätigen (Android Sicherheit)

## Sicherheitsmerkmale

### HTTPS-Only
Alle Downloads müssen über HTTPS erfolgen.

### GitHub-Domain-Validierung
Downloads werden nur von github.com oder githubusercontent.com akzeptiert.

### FileProvider
APK-Dateien werden sicher über FileProvider geteilt (Android 7.0+).

## Einstellungen

### Auto-Update aktivieren/deaktivieren

In den App-Einstellungen kann der Benutzer das Auto-Update Feature komplett deaktivieren:

```
Einstellungen → Automatische Updates aktivieren
```

Standard: **Aktiviert**

### Update-Intervall

Die App prüft maximal einmal alle 20 Minuten auf Updates. Dies verhindert:
- Unnötige API-Aufrufe
- Nervige Update-Dialoge bei jedem App-Start

**Hinweis**: Die Update-Prüfung erfolgt im Hintergrund ohne Benachrichtigung des Benutzers. Nur wenn ein Update verfügbar ist, wird ein Dialog angezeigt. Dies sorgt für eine bessere Benutzererfahrung ohne störende Toast-Nachrichten.

## Technische Details

### Verwendete Komponenten

1. **GitHubApi.kt**: Retrofit API Interface für GitHub Releases
2. **UpdateService.kt**: Hauptlogik für Version-Checking und Downloads
3. **MainActivity.kt**: Integration der Update-Prüfung beim App-Start

### Versionsnummern-Vergleich

Versionen werden als Zahlenfolge verglichen:
- `1.0.0` < `1.0.1`
- `1.0.9` < `1.1.0`
- `1.9.0` < `2.0.0`

Tag-Namen mit "v" Präfix werden automatisch erkannt (`v1.0.0` → `1.0.0`).

### Berechtigungen

Erforderliche Android-Berechtigungen:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

## Für Entwickler

### Release erstellen

Um ein Update bereitzustellen:

1. Versionen in `app/build.gradle.kts` erhöhen:
   ```kotlin
   versionCode = 2
   versionName = "1.1.0"
   ```

2. APK bauen:
   ```bash
   ./gradlew assembleRelease
   ```

3. GitHub Release erstellen:
   - Tag: `v1.1.0` (mit "v" Präfix)
   - APK-Datei als Asset hochladen
   - Release Notes schreiben

4. Release veröffentlichen

Die App wird automatisch das neue Update bei den Benutzern anzeigen.

### Lokale Tests

Für lokale Tests kann die Update-Prüfung simuliert werden:

```kotlin
// In MainActivity
updateService.checkForUpdate()
```

**Hinweis:** Stellen Sie sicher, dass die lokale Version niedriger ist als die veröffentlichte Version.

### Debug-Logging

Alle Update-Aktivitäten werden geloggt:

```
Logger.d("UpdateService", "Checking for updates...")
Logger.i("UpdateService", "Update available: 1.1.0")
Logger.i("MainActivity", "Starting update download")
```

Logs können in den App-Einstellungen angezeigt werden:
**Einstellungen → App-Logs anzeigen**

## Fehlerbehebung

### Update-Prüfung funktioniert nicht

**Mögliche Ursachen:**
1. Keine Internetverbindung
2. GitHub API ist nicht erreichbar
3. Auto-Update ist in den Einstellungen deaktiviert
4. Letzte Prüfung war vor weniger als 20 Minuten

**Lösung:**
- App-Logs in den Einstellungen überprüfen
- Internetverbindung prüfen
- Auto-Update Einstellung überprüfen

### Download schlägt fehl

**Mögliche Ursachen:**
1. Keine Download-Berechtigung
2. Kein Speicherplatz
3. Netzwerkfehler

**Lösung:**
- Speicherplatz überprüfen
- Berechtigungen in Android-Einstellungen prüfen
- Netzwerkverbindung prüfen

### Installation wird nicht gestartet

**Mögliche Ursachen:**
1. "Installation aus unbekannten Quellen" ist deaktiviert
2. APK-Datei ist beschädigt

**Lösung:**
1. Android-Einstellungen öffnen
2. "Installation aus unbekannten Quellen" aktivieren für NewsAgent
3. APK-Download wiederholen

## Best Practices

### Für Endbenutzer

1. **Automatische Updates aktiviert lassen** für maximale Sicherheit
2. **Updates zeitnah installieren** um neue Features und Bugfixes zu erhalten
3. **Release Notes lesen** vor der Installation

### Für Entwickler

1. **Semantic Versioning verwenden** (MAJOR.MINOR.PATCH)
2. **Aussagekräftige Release Notes** schreiben
3. **APK immer signieren** mit dem Release-Key
4. **Version testen** vor der Veröffentlichung
5. **Changelog pflegen** für bessere Nachvollziehbarkeit

## Datenschutz

Die Auto-Update-Funktion:
- Greift nur auf die öffentliche GitHub API zu
- Sendet keine persönlichen Daten
- Erfasst keine Nutzungsstatistiken
- Respektiert die Benutzereinstellungen

Alle API-Aufrufe sind in den App-Logs einsehbar.
