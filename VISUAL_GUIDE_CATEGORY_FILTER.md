# Visual Guide: Category and Keyword Filter Feature

## UI Flow Diagrams

### Settings Screen - New Filter Section

```
┌─────────────────────────────────────────────────┐
│  NewsAgent - Einstellungen                      │
├─────────────────────────────────────────────────┤
│                                                  │
│  [Previous settings sections...]                │
│                                                  │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━   │
│                                                  │
│  📂 Nachrichten-Filter                          │
│                                                  │
│  Kategorien auswählen                           │
│  Lassen Sie leer, um alle anzuzeigen           │
│  ┌─────────────────────────────────────────┐   │
│  │ Technologie, Politik, Sport             │   │
│  └─────────────────────────────────────────┘   │
│  Verfügbar: Allgemein, Technologie, Politik,   │
│  Wirtschaft, Sport, Wissenschaft, Kultur,       │
│  Umwelt                                         │
│                                                  │
│  Stichwörter / Keywords                         │
│  Filter für wichtige Stichwörter               │
│  ┌─────────────────────────────────────────┐   │
│  │ KI, Klima, Europa                       │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  ☑ Nur ungelesene Artikel anzeigen             │
│                                                  │
│  Sortierung                                     │
│  ┌─────────────────────────────────────────┐   │
│  │ Neueste zuerst                    ▼     │   │
│  └─────────────────────────────────────────┘   │
│                                                  │
│  [Einstellungen speichern]                     │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Main Screen - Filtered Articles View

**Before Opening Article (Unread):**
```
┌─────────────────────────────────────────────────┐
│  NewsAgent                           [⋮] [🔄]   │
├─────────────────────────────────────────────────┤
│                                                  │
│  [Technologie] KI revolutioniert die Medizin    │
│  Heise Online • 2024-02-14T10:30:00Z           │
│  ⚠ Glaubwürdigkeit: 78%                        │
│  ────────────────────────────────────────────   │
│                                                  │
│  [Sport] Deutschland gewinnt WM-Qualifikation   │
│  Tagesschau • 2024-02-14T09:15:00Z             │
│  ✓ Glaubwürdigkeit: 92%                        │
│  ────────────────────────────────────────────   │
│                                                  │
│  [Politik] Neue Klimagesetze verabschiedet      │
│  Spiegel Online • 2024-02-14T08:00:00Z         │
│  ✓ Glaubwürdigkeit: 85%                        │
│  ────────────────────────────────────────────   │
│                                                  │
└─────────────────────────────────────────────────┘
```

**After Opening Article (Read - Dimmed):**
```
┌─────────────────────────────────────────────────┐
│  NewsAgent                           [⋮] [🔄]   │
├─────────────────────────────────────────────────┤
│                                                  │
│  ✓ [Technologie] KI revolutioniert...   (dim)  │
│  Heise Online • 2024-02-14T10:30:00Z    (dim)  │
│  ⚠ Glaubwürdigkeit: 78%                 (dim)  │
│  ────────────────────────────────────────────   │
│                                                  │
│  [Sport] Deutschland gewinnt WM-Qualifikation   │
│  Tagesschau • 2024-02-14T09:15:00Z             │
│  ✓ Glaubwürdigkeit: 92%                        │
│  ────────────────────────────────────────────   │
│                                                  │
│  [Politik] Neue Klimagesetze verabschiedet      │
│  Spiegel Online • 2024-02-14T08:00:00Z         │
│  ✓ Glaubwürdigkeit: 85%                        │
│  ────────────────────────────────────────────   │
│                                                  │
└─────────────────────────────────────────────────┘
```

**With "Show Only Unread" Enabled:**
```
┌─────────────────────────────────────────────────┐
│  NewsAgent                           [⋮] [🔄]   │
├─────────────────────────────────────────────────┤
│                                                  │
│  [Sport] Deutschland gewinnt WM-Qualifikation   │
│  Tagesschau • 2024-02-14T09:15:00Z             │
│  ✓ Glaubwürdigkeit: 92%                        │
│  ────────────────────────────────────────────   │
│                                                  │
│  [Politik] Neue Klimagesetze verabschiedet      │
│  Spiegel Online • 2024-02-14T08:00:00Z         │
│  ✓ Glaubwürdigkeit: 85%                        │
│  ────────────────────────────────────────────   │
│                                                  │
│  (Read article no longer visible)               │
│                                                  │
└─────────────────────────────────────────────────┘
```

### Menu Options

```
┌─────────────────────────────────────┐
│  ⋮ Menu                             │
├─────────────────────────────────────┤
│  Einstellungen                      │
│  Kostenlose Suche                   │
│  RSS Nachrichten                    │
│  Alle als ungelesen markieren  ← NEW│
│  ℹ️ Info & Hilfe                   │
└─────────────────────────────────────┘
```

## Filter Behavior Examples

### Example 1: Technology News Only

**Settings:**
- Categories: `Technologie`
- Keywords: _(empty)_
- Unread: Disabled
- Sort: Newest first

**Result:**
```
[Technologie] KI revolutioniert die Medizin
[Technologie] Neue Smartphone-Generation vorgestellt
[Technologie] Cybersecurity-Warnung ausgegeben
```

### Example 2: Politics + Climate Keywords

**Settings:**
- Categories: `Politik, Umwelt`
- Keywords: `Klima, CO2, Europa`
- Unread: Disabled
- Sort: By credibility

**Result (sorted by credibility score):**
```
✓ 95% [Politik] EU-Klimagesetz verabschiedet
✓ 88% [Umwelt] CO2-Emissionen sinken erstmals
✓ 82% [Politik] Klimagipfel in Europa geplant
```

### Example 3: Unread Sports News

**Settings:**
- Categories: `Sport`
- Keywords: _(empty)_
- Unread: **Enabled**
- Sort: Newest first

**Result (only unread sports articles):**
```
[Sport] Bundesliga: Bayern gewinnt
[Sport] Champions League Auslosung
(Previously opened articles hidden)
```

## Visual Indicators Legend

```
Symbol   Meaning
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
[Category]   Article category tag
✓            Read article marker
(dim)        Reduced opacity (0.6)
✓ 92%        Credibility verified
⚠ 78%        Credibility not verified
```

## Accessibility Features

**Screen Reader Announcements:**

Unread article:
```
"Ungelesen. Kategorie: Technologie. 
KI revolutioniert die Medizin"
```

Read article:
```
"Gelesen. Kategorie: Sport. 
Deutschland gewinnt WM-Qualifikation"
```

## User Journey Flow

```
┌─────────────┐
│   App Start │
└──────┬──────┘
       │
       ▼
┌──────────────────────┐
│ Load Filtered News   │ ← Applies saved filter settings
└──────┬───────────────┘
       │
       ▼
┌──────────────────────┐      ┌────────────────┐
│ Show Filtered List   │─────→│ Open Article   │
└──────┬───────────────┘      └────────┬───────┘
       │                               │
       │                               ▼
       │                      ┌────────────────┐
       │                      │ Mark as Read   │
       │                      └────────┬───────┘
       │                               │
       │                               ▼
       │                      ┌────────────────┐
       │                      │ Visual Update  │
       │                      │ (✓ + dimmed)   │
       │                      └────────┬───────┘
       │                               │
       │◄──────────────────────────────┘
       │
       ▼
┌──────────────────────┐      ┌────────────────┐
│ Refresh Button       │─────→│ Reload Filtered│
└──────────────────────┘      └────────────────┘

       ▼
┌──────────────────────┐      ┌────────────────┐
│ Menu: Mark Unread    │─────→│ All → Unread   │
└──────────────────────┘      └────────────────┘

       ▼
┌──────────────────────┐      ┌────────────────┐
│ Menu: Settings       │─────→│ Change Filters │
└──────────────────────┘      └────────┬───────┘
                                       │
                                       ▼
                              ┌────────────────┐
                              │ Save Settings  │
                              └────────┬───────┘
                                       │
                                       ▼
                              ┌────────────────┐
                              │ Return to Main │
                              └────────┬───────┘
                                       │
                                       ▼
                              ┌────────────────┐
                              │ Refresh List   │
                              └────────────────┘
```

## Toast Messages

**On Load:**
```
┌─────────────────────────────────────────┐
│  Suche wird gestartet...                │
│  Quelle: RSS Feeds | Land: de |         │
│  Sprache: de | Max: 10                  │
└─────────────────────────────────────────┘
```

**On Success:**
```
┌─────────────────────────────────────────┐
│  8 Artikel geladen                      │
└─────────────────────────────────────────┘
```

**On No Results (Filters Too Restrictive):**
```
┌─────────────────────────────────────────┐
│  Keine Nachrichten gefunden.            │
│  Bitte API-Schlüssel konfigurieren      │
│  oder Filter anpassen.                  │
└─────────────────────────────────────────┘
```

**On Mark All Unread:**
```
┌─────────────────────────────────────────┐
│  8 Artikel als ungelesen markiert       │
└─────────────────────────────────────────┘
```

## Category Color Coding (Future Enhancement)

While not implemented in this version, categories could have color codes:

```
🔵 Technologie (Blue)
🔴 Politik (Red)
🟢 Wirtschaft (Green)
🟡 Sport (Yellow)
🟣 Wissenschaft (Purple)
🟠 Kultur (Orange)
🌿 Umwelt (Green/Leaf)
⚫ Allgemein (Black/Gray)
```

## Implementation Notes

**Technical Details:**
- Article list updates in real-time when marked as read
- Categories auto-inferred on first load
- Tags extracted automatically
- All changes are immediate (no reload needed for visual updates)
- Filter changes require refresh to fetch new filtered results

**Performance:**
- No additional network calls
- Client-side filtering (fast)
- Minimal memory overhead
- Cache-friendly implementation
