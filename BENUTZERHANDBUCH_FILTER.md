# Benutzerhandbuch: Nachrichten-Filter

## Überblick

Mit den neuen Filter-Funktionen können Sie Ihre Nachrichten personalisieren und nur die Artikel anzeigen, die Sie wirklich interessieren.

## Funktionen

### 1. Kategorie-Filter

Filtern Sie Nachrichten nach Themengebieten:

**Verfügbare Kategorien:**
- 🖥️ **Technologie** - Tech-News, KI, Digitales, Software
- 🏛️ **Politik** - Regierung, Wahlen, Gesetze
- 💼 **Wirtschaft** - Börse, Unternehmen, Märkte
- ⚽ **Sport** - Fußball, Bundesliga, Olympia
- 🔬 **Wissenschaft** - Forschung, Gesundheit, Studien
- 🎨 **Kultur** - Kunst, Musik, Film, Theater
- 🌱 **Umwelt** - Klima, Energie, Natur
- 📰 **Allgemein** - Sonstige Nachrichten

**Wie es funktioniert:**
1. Öffnen Sie die **Einstellungen** (Menü → Einstellungen)
2. Scrollen Sie zu **"Nachrichten-Filter"**
3. Geben Sie die gewünschten Kategorien ein (kommagetrennt)
   - Beispiel: `Technologie, Politik, Sport`
4. Speichern Sie die Einstellungen
5. Aktualisieren Sie die Hauptseite

**Hinweis:** Wenn keine Kategorien angegeben sind, werden alle Artikel angezeigt.

### 2. Stichwort-Filter

Filtern Sie Artikel nach bestimmten Begriffen:

**Verwendung:**
1. Geben Sie Ihre wichtigen Stichwörter ein (kommagetrennt)
   - Beispiel: `KI, Klima, Europa, Deutschland`
2. Die App sucht nach diesen Begriffen in:
   - Artikelüberschrift
   - Artikelbeschreibung
   - Automatisch extrahierten Tags
3. Nur passende Artikel werden angezeigt

**Tipps:**
- Verwenden Sie allgemeine Begriffe für mehr Ergebnisse
- Kombinieren Sie mit Kategorie-Filtern für präzise Resultate
- Groß-/Kleinschreibung wird ignoriert

### 3. Ungelesen-Filter

Zeigen Sie nur Artikel an, die Sie noch nicht geöffnet haben:

1. Aktivieren Sie **"Nur ungelesene Artikel anzeigen"**
2. Artikel, die Sie öffnen, werden automatisch als gelesen markiert
3. Diese verschwinden dann aus der Liste (wenn Filter aktiv)

**Alle als ungelesen markieren:**
- Menü → "Alle als ungelesen markieren"
- Nützlich, um Ihre Liste zurückzusetzen

### 4. Sortierung

Wählen Sie, wie Ihre Artikel sortiert werden:

**Sortier-Optionen:**

**🕐 Neueste zuerst** (Standard)
- Zeigt die aktuellsten Artikel zuerst
- Ideal für Breaking News

**⭐ Nach Glaubwürdigkeit**
- Sortiert nach Credibility Score
- Vertrauenswürdige Artikel zuerst
- Bei gleichem Score: neueste zuerst

**🎯 Nach Relevanz**
- Basiert auf Keyword-Übereinstimmung
- Funktioniert am besten mit Stichwort-Filter

## Visuelle Indikatoren

### In der Artikelliste

**Kategorie-Anzeige:**
```
[Technologie] KI revolutioniert die Medizin
```

**Gelesen-Markierung:**
```
✓ [Sport] Deutschland gewinnt WM
```
- Gelesene Artikel sind leicht abgedunkelt
- Checkmark (✓) zeigt an, dass der Artikel gelesen wurde

**Glaubwürdigkeits-Indikator:**
```
✓ Glaubwürdigkeit: 87%   (verifiziert)
⚠ Glaubwürdigkeit: 45%   (nicht verifiziert)
```

## Automatische Kategorisierung

Die App erkennt Kategorien automatisch basierend auf Schlüsselwörtern:

**Beispiele:**

| Artikel-Inhalt | Zugewiesene Kategorie |
|----------------|----------------------|
| "Neue KI-Technologie entwickelt" | Technologie |
| "Bundestag beschließt Gesetz" | Politik |
| "DAX steigt auf Rekordhoch" | Wirtschaft |
| "FC Bayern gewinnt Meisterschaft" | Sport |
| "Neue Studie zu Klimawandel" | Wissenschaft/Umwelt |

## Beispiel-Szenarien

### Szenario 1: Tech-Enthusiast
```
Kategorien: Technologie, Wissenschaft
Stichwörter: KI, AI, ChatGPT, Robotik, Innovation
Sortierung: Neueste zuerst
```
➜ Zeigt neueste Tech- und Wissenschafts-News mit KI-Bezug

### Szenario 2: Politisch Interessiert
```
Kategorien: Politik, Wirtschaft
Stichwörter: Bundestag, Europa, Wahlen
Ungelesen: Aktiviert
Sortierung: Nach Glaubwürdigkeit
```
➜ Zeigt ungelesene Politik/Wirtschaft-News, sortiert nach Vertrauenswürdigkeit

### Szenario 3: Sport-Fan
```
Kategorien: Sport
Stichwörter: Bundesliga, Champions League, Bayern
Sortierung: Neueste zuerst
```
➜ Zeigt aktuelle Fußball-News zu deutschen Vereinen

### Szenario 4: Alles sehen (Standard)
```
Kategorien: (leer)
Stichwörter: (leer)
Ungelesen: Deaktiviert
Sortierung: Neueste zuerst
```
➜ Zeigt alle Artikel chronologisch

## Fehlerbehebung

### Problem: Keine Artikel werden angezeigt

**Mögliche Ursachen:**
1. Filter zu restriktiv
   - **Lösung:** Entfernen Sie einige Kategorien/Stichwörter
2. Alle Artikel gelesen + Ungelesen-Filter aktiv
   - **Lösung:** "Alle als ungelesen markieren" oder Filter deaktivieren
3. Keine Artikel in gewählten Kategorien
   - **Lösung:** Warten Sie auf nächstes Update oder erweitern Sie Filter

### Problem: Falsche Kategorisierung

Die automatische Kategorisierung basiert auf Heuristiken und kann gelegentlich falsch sein:
- Dies ist normal und kein Fehler
- Verwenden Sie Stichwort-Filter für präzisere Ergebnisse
- Zukünftige Versionen könnten ML-basierte Kategorisierung verwenden

### Problem: Filter funktionieren nicht

1. Stellen Sie sicher, dass Sie die Einstellungen gespeichert haben
2. Aktualisieren Sie die Artikelliste (Refresh-Button)
3. Überprüfen Sie, ob API-Key konfiguriert ist
4. Prüfen Sie App-Logs (Einstellungen → App-Logs anzeigen)

## Best Practices

1. **Starten Sie breit:** Beginnen Sie mit wenigen Kategorien, verfeinern Sie später
2. **Kombinieren Sie Filter:** Kategorie + Stichwort für beste Ergebnisse
3. **Nutzen Sie Ungelesen:** Behalten Sie den Überblick über neue Artikel
4. **Experimentieren Sie:** Testen Sie verschiedene Sortierungen
5. **Aktualisieren Sie regelmäßig:** Filter wirken nur auf neue API-Aufrufe

## Technische Details

- **Caching:** Gefilterte Ergebnisse werden gecacht
- **Performance:** Keine zusätzlichen API-Kosten
- **Client-Side:** Filterung erfolgt nach API-Abruf
- **Backward Compatible:** Alte Einstellungen bleiben erhalten

## Zukünftige Erweiterungen

Geplante Features (nicht implementiert):
- [ ] Multi-Select Kategorie-Picker (statt Text-Eingabe)
- [ ] Filter-Presets speichern
- [ ] Datum-Filter (nur Artikel der letzten X Tage)
- [ ] Quellen-Filter (nur bestimmte News-Quellen)
- [ ] Statistiken (X von Y Artikeln gefiltert)

## Support

Bei Problemen:
1. Prüfen Sie dieses Handbuch
2. Sehen Sie sich die App-Logs an (Einstellungen → App-Logs)
3. Erstellen Sie ein Issue auf GitHub mit Logs
