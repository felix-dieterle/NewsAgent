# Contributing to NewsAgent

Vielen Dank für Ihr Interesse, zu NewsAgent beizutragen! 🎉

## Wie Sie beitragen können

### Bugs melden

Wenn Sie einen Bug finden:

1. Überprüfen Sie, ob das Problem bereits in den [Issues](https://github.com/felix-dieterle/NewsAgent/issues) gemeldet wurde
2. Wenn nicht, erstellen Sie ein neues Issue mit:
   - Klarer Beschreibung des Problems
   - Schritten zur Reproduktion
   - Erwartetes vs. tatsächliches Verhalten
   - Android-Version und Gerätetyp
   - Logcat-Ausgabe (falls relevant)

### Feature-Vorschläge

Wir freuen uns über Ideen für neue Features:

1. Erstellen Sie ein Issue mit dem Label "enhancement"
2. Beschreiben Sie:
   - Das Problem, das das Feature lösen würde
   - Ihr vorgeschlagener Lösungsansatz
   - Alternative Lösungen, die Sie in Betracht gezogen haben

### Code beitragen

#### 1. Fork & Clone

```bash
# Fork das Repository auf GitHub
# Dann klonen Sie Ihren Fork
git clone https://github.com/IHR_USERNAME/NewsAgent.git
cd NewsAgent
git remote add upstream https://github.com/felix-dieterle/NewsAgent.git
```

#### 2. Branch erstellen

```bash
git checkout -b feature/ihr-feature-name
# oder
git checkout -b bugfix/ihr-bugfix-name
```

#### 3. Änderungen vornehmen

- Folgen Sie dem bestehenden Code-Stil
- Schreiben Sie aussagekräftige Commit-Messages
- Testen Sie Ihre Änderungen gründlich
- Aktualisieren Sie die Dokumentation falls nötig

#### 4. Tests

```bash
# Unit Tests ausführen
./gradlew test

# Lint prüfen
./gradlew lint
```

#### 5. Commit & Push

```bash
git add .
git commit -m "feat: Beschreibung Ihrer Änderung"
git push origin feature/ihr-feature-name
```

**Commit Message Konventionen:**
- `feat:` Neues Feature
- `fix:` Bugfix
- `docs:` Dokumentationsänderung
- `style:` Code-Formatierung
- `refactor:` Code-Refactoring
- `test:` Tests hinzufügen/ändern
- `chore:` Build-Prozess, Tools

#### 6. Pull Request erstellen

1. Gehen Sie zu Ihrem Fork auf GitHub
2. Klicken Sie auf "Pull Request"
3. Füllen Sie die PR-Vorlage aus:
   - Beschreibung der Änderungen
   - Bezug zu relevanten Issues
   - Screenshots (bei UI-Änderungen)
   - Checkliste abarbeiten

## Code-Stil

### Kotlin Conventions

Wir folgen den [offiziellen Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

```kotlin
// Guter Stil
class NewsRepository(private val context: Context) {
    
    suspend fun fetchNews(): List<NewsArticle> = withContext(Dispatchers.IO) {
        try {
            // Implementation
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

// Vermeide
class news_repository {
    fun FetchNews() {
        // ...
    }
}
```

### Dokumentation

- Dokumentieren Sie öffentliche APIs mit KDoc
- Fügen Sie erklärende Kommentare für komplexe Logik hinzu
- Aktualisieren Sie README.md bei wesentlichen Änderungen

```kotlin
/**
 * Generiert eine KI-Zusammenfassung für einen Nachrichtenartikel
 * 
 * @param article Der zu zusammenfassende Artikel
 * @return NewsSummary oder null bei Fehler
 */
suspend fun generateSummary(article: NewsArticle): NewsSummary?
```

### Architektur

- Folgen Sie der bestehenden Architektur (siehe ARCHITECTURE.md)
- Modelle in `models/`
- API-Definitionen in `api/`
- Business-Logik in `services/`
- UI-Code in `ui/`

## Testing Guidelines

### Unit Tests

```kotlin
@Test
fun `checkSourceReputation returns high score for known sources`() {
    val service = CredibilityCheckService(context)
    val score = service.checkSourceReputation("Tagesschau")
    assertTrue(score > 0.8f)
}
```

### UI Tests

```kotlin
@Test
fun testNewsListDisplayed() {
    val scenario = launchActivity<MainActivity>()
    onView(withId(R.id.news_list)).check(matches(isDisplayed()))
}
```

## Review-Prozess

1. **Automatische Checks**: CI/CD läuft automatisch
2. **Code Review**: Mindestens ein Maintainer reviewt
3. **Diskussion**: Bei Fragen/Anmerkungen
4. **Merge**: Nach Approval und bestandenen Tests

## Entwicklungsumgebung

### Empfohlene Tools

- **Android Studio**: Neueste stabile Version
- **Kotlin Plugin**: Aktuell halten
- **Android SDK**: API Level 24-34

### Nützliche Plugins

- **Kotlin Multiplatform Mobile**: Für zukünftige KMM-Entwicklung
- **Rainbow Brackets**: Code-Lesbarkeit
- **Key Promoter X**: Tastatur-Shortcuts lernen

## Fragen?

- **Discussions**: Für allgemeine Fragen
- **Issues**: Für spezifische Probleme
- **Email**: Für private Anliegen

## Verhaltenskodex

Wir erwarten von allen Beitragenden:

- Respektvoller Umgang
- Konstruktive Kritik
- Offenheit für verschiedene Perspektiven
- Fokus auf das Beste für das Projekt

## Lizenz

Durch Beiträge stimmen Sie zu, dass Ihre Arbeit unter der MIT-Lizenz lizenziert wird.

---

Danke für Ihren Beitrag zu NewsAgent! 🚀
