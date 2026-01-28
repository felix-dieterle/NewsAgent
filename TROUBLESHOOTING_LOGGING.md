# Fehlerbehebung - Installation & Logging

## Problem: "Du kannst die app auf deinem gerät nicht installieren"

Diese Dokumentation hilft bei der Diagnose und Behebung von Installationsproblemen der NewsAgent App.

## Neue Logging-Funktionen

### ✅ Was wurde implementiert

1. **Zentrales Logging-System** (`Logger.kt`)
   - Alle wichtigen App-Ereignisse werden protokolliert
   - Logs werden sowohl in Logcat als auch in Dateien geschrieben
   - Persistente Log-Dateien im App-Verzeichnis (`newsagent_logs.txt`)
   - Automatische Log-Rotation bei 1MB Größe

2. **Custom Application Class** (`NewsAgentApplication.kt`)
   - Globaler Crash-Handler für unbehandelte Exceptions
   - Protokollierung von App-Start und Geräte-Informationen
   - Früherkennung von Initialisierungsproblemen

3. **Erweiterte Fehlerprotokollierung**
   - MainActivity: Detaillierte Logs bei Service-Initialisierung
   - NewsRepository: API-Request-Logging mit Fehlerdetails
   - Alle kritischen Code-Pfade werden überwacht

4. **Log-Viewer in den Einstellungen**
   - **Logs anzeigen**: Alle App-Logs direkt in der App ansehen
   - **Logs löschen**: Alte Logs entfernen
   - **Logs teilen**: Logs per E-Mail/Messenger für Support senden

## Wie verwende ich das Logging?

### Logs anzeigen

1. Öffne die App
2. Gehe zu **Einstellungen** (Menü ≡)
3. Scrolle nach unten zu **"Fehlerbehebung"**
4. Tippe auf **"App-Logs anzeigen"**

Die Logs zeigen:
- Wann die App gestartet wurde
- Welche Services initialisiert wurden
- Alle Fehler mit Stack-Traces
- API-Anfragen und Antworten

### Logs teilen (für Support)

1. In den **Einstellungen**
2. Tippe auf **"Logs teilen (für Support)"**
3. Wähle E-Mail, Messenger oder andere App
4. Sende die Logs an den Support

### Logs löschen

1. In den **Einstellungen**
2. Tippe auf **"Logs löschen"**
3. Alte Logs werden entfernt (spart Speicherplatz)

## Häufige Installationsprobleme & Lösungen

### 1. "App kann nicht installiert werden" - Allgemein

**Mögliche Ursachen:**
- Nicht genug Speicherplatz auf dem Gerät
- Installation aus unbekannten Quellen nicht aktiviert
- Inkompatible Android-Version (benötigt Android 7.0+)
- Beschädigte APK-Datei

**Lösungen:**
1. **Speicherplatz prüfen**: Mind. 50MB frei benötigt
2. **Unbekannte Quellen aktivieren**:
   - Einstellungen → Sicherheit → "Unbekannte Quellen" aktivieren
   - Oder: Einstellungen → Apps → Spezial-Zugriff → "Unbekannte Apps installieren"
3. **Android-Version prüfen**: Einstellungen → Über das Telefon → Android-Version
   - Benötigt: Android 7.0 (API 24) oder höher
4. **APK neu herunterladen**: Evtl. war Download beschädigt

### 2. App stürzt beim Start ab

**Was tun:**
1. App öffnen (falls möglich)
2. Zu **Einstellungen** → **"Logs teilen"**
3. Logs im Dialog anschauen oder teilen
4. Nach Fehlern suchen wie:
   - "OutOfMemoryError" → Gerät hat zu wenig RAM
   - "ClassNotFoundException" → APK ist beschädigt
   - "SecurityException" → Berechtigungsproblem

**Logs mit ADB auslesen (nur für Debug-Builds):**
```bash
# Via Logcat (wenn App abstürzt)
adb logcat -s NewsAgent:* AndroidRuntime:E

# Log-Dateien vom Gerät holen (nur bei Debug-Builds möglich)
# Hinweis: Bei Release-Builds ist "run-as" nicht verfügbar
adb shell run-as com.newsagent cat files/newsagent_logs.txt > logs.txt
adb shell run-as com.newsagent cat files/newsagent_logs.txt.old >> logs.txt
```

### 3. "Parsing Error" beim Installieren

**Ursachen:**
- APK ist nicht vollständig heruntergeladen
- APK wurde für neuere Android-Version kompiliert
- APK ist beschädigt

**Lösungen:**
1. APK komplett neu herunterladen
2. Prüfen: Dateigröße korrekt? (sollte mehrere MB sein)
3. Von einem anderen Gerät/Browser herunterladen

### 4. App installiert, startet aber nicht

**Diagnose:**
1. Nach Installation: Logcat-Ausgabe prüfen (ADB)
2. Crash-Logs werden in `/data/data/com.newsagent/files/newsagent_logs.txt` gespeichert
3. App-Info öffnen → "Speicher löschen" versuchen

**Häufige Fehler in Logs:**
- `"Logger initialized"` → App startet grundsätzlich
- `"Services initialized successfully"` → Hauptkomponenten OK
- Fehlende Meldungen → Crash vor Logger-Init

## Für Entwickler: Log-Stufen verstehen

Die App verwendet folgende Log-Levels:

| Level | Verwendung | Beispiel |
|-------|------------|----------|
| **DEBUG** | Detaillierte Ablauf-Informationen | "Fetching headlines...", "Setting up UI..." |
| **INFO** | Wichtige Ereignisse | "Logger initialized", "App started successfully" |
| **WARN** | Warnungen, nicht-kritische Probleme | "No articles fetched - possibly missing API key" |
| **ERROR** | Fehler mit Stack-Traces | "Exception fetching headlines", "Fatal error in onCreate" |

### Log-Beispiel

```
2026-01-28 10:15:30.123 [INFO] Application: NewsAgent Application starting
2026-01-28 10:15:30.125 [INFO] Application: Android Version: 13 (SDK 33)
2026-01-28 10:15:30.127 [INFO] Application: Device: Samsung SM-G998B
2026-01-28 10:15:30.130 [INFO] Application: App Version: 1.0 (1)
2026-01-28 10:15:30.145 [INFO] Application: Global exception handler installed
2026-01-28 10:15:30.200 [INFO] MainActivity: onCreate started
2026-01-28 10:15:30.210 [DEBUG] MainActivity: Initializing services...
2026-01-28 10:15:30.350 [DEBUG] MainActivity: Services initialized successfully
```

## Implementierte Sicherheitsmaßnahmen

1. **Fail-Safe Logging**: Logging-Fehler crashen die App nicht
2. **Log-Rotation**: Automatische Begrenzung auf 1MB + 1MB Backup
3. **Private Speicherung**: Logs nur im App-Verzeichnis (nicht öffentlich)
4. **Stack-Trace Erfassung**: Vollständige Fehlerinformationen
5. **Startup-Logging**: Geräte-/Version-Info für Kompatibilitätsprüfung

## Support kontaktieren

Wenn die App nicht installiert werden kann oder abstürzt:

1. Logs exportieren (siehe oben)
2. GitHub-Issue erstellen mit:
   - Gerätemodell
   - Android-Version  
   - Fehlerbesschreibung
   - Logs als Anhang oder Text
3. Link: https://github.com/felix-dieterle/NewsAgent/issues

## Technische Details

### Log-Speicherorte

- **App-Logs**: `/data/data/com.newsagent/files/newsagent_logs.txt`
- **Backup**: `/data/data/com.newsagent/files/newsagent_logs.txt.old`
- **Logcat**: Tag `NewsAgent/*` (z.B. `NewsAgent/MainActivity`)

### Log-API für Entwickler

```kotlin
import com.newsagent.utils.Logger

// Debug-Meldung
Logger.d("MyTag", "Debug information")

// Info-Meldung  
Logger.i("MyTag", "Important event occurred")

// Warnung
Logger.w("MyTag", "Something looks wrong", optionalException)

// Fehler
Logger.e("MyTag", "Error happened", exception)

// Logs lesen
val logs = Logger.readLogs(context)

// Logs löschen
Logger.clearLogs(context)
```

## Häufig gestellte Fragen

### Werden Logs automatisch gelöscht?

Nein, aber sie rotieren automatisch bei 1MB. Manuelles Löschen über Einstellungen möglich.

### Sind die Logs sicher?

Ja, sie werden nur lokal im App-Verzeichnis gespeichert (nicht öffentlich zugänglich). API-Schlüssel werden mit minimaler Detailtiefe geloggt (nur HTTP-Methode und URL, keine Request-Bodies oder Header).

### Verlangsamen Logs die App?

Minimaler Impact. Logging läuft asynchron und nutzt effizientes File-I/O.

### Kann ich Logs per ADB auslesen?

Ja (siehe Abschnitt "App stürzt beim Start ab" oben).

---

**Version**: 1.0  
**Letzte Aktualisierung**: Januar 2026
