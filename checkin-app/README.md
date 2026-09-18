# Express Yourself — Check-In App (JavaFX)

A pure-Java JavaFX prototype implementing a unified "Express Yourself"
check-in screen: the user can provide a facial photo, written thoughts, or
both in one screen, then run a single "Analyze My Check-In" action that
adapts to whichever inputs are present.

No HTML, CSS-in-web, JavaScript, React, Python, or FXML is used — the UI is
built directly in Java using JavaFX controls and layouts (`ImageView`,
`FileChooser`, `TextArea`, `Label`, `Button`, `VBox`, `HBox`, `GridPane`,
`BorderPane`, `ScrollPane`, `ProgressBar`, `ProgressIndicator`). A small
`styles.css` file is used only for JavaFX's built-in CSS styling API (this is
standard practice for JavaFX apps, not "web CSS").

## Flow

```
CHECK-IN (Express Yourself)
   ↓
PHOTO + THOUGHTS
   ↓
ANALYZE MY CHECK-IN
   ↓
LOADING
   ↓
RESULTS
   ↓
SUPPORTIVE SUGGESTIONS
   ↓
AI COMPANION (placeholder hand-off)
```

The user may provide a photo only, thoughts only, or both. Both inputs
belong to the same check-in and are analyzed together — there is no
separate "Photo Analysis → Results → Thoughts Analysis" detour.

## Project structure

```
checkin-app/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/checkin/
    │   ├── App.java                          Entry point, screen wiring
    │   ├── components/
    │   │   ├── PhotoUploadCard.java          "FACIAL SIGNAL" card (drag & drop / browse / preview)
    │   │   ├── ThoughtsInputCard.java        "YOUR THOUGHTS" card (TextArea + prompt chips + counter)
    │   │   ├── CheckInSummary.java           "YOUR CHECK-IN" status checklist
    │   │   └── AnalyzeButton.java            Single adaptive primary action button
    │   ├── screens/
    │   │   ├── CheckInScreen.java            "Express Yourself" unified screen
    │   │   ├── LoadingScreen.java            Step-by-step pipeline progress
    │   │   ├── ResultsScreen.java            Facial / Textual / Combined results
    │   │   └── SupportiveSuggestionsScreen.java
    │   └── model/
    │       ├── CheckInData.java              Holds photo file + thoughts text
    │       ├── AnalysisResult.java           Facial / text / combined result values
    │       └── AnalysisEngine.java           Prototype (mock) analysis + fusion logic
    └── resources/
        └── styles.css                        JavaFX CSS styling
```

## Requirements

- JDK 17 or newer
- Maven 3.8+
- Internet access the first time you build (Maven downloads the JavaFX
  dependencies and the `javafx-maven-plugin`)

## Build & run

```bash
cd checkin-app
mvn clean javafx:run
```

Or build a runnable jar:

```bash
mvn clean package
```

## Notes on the analysis logic

`AnalysisEngine` is a **prototype stand-in** for real facial- and
text-emotion-recognition models. It uses simple keyword heuristics for text
and randomized-but-plausible confidence scores for the facial signal, so the
full UI flow (loading → results → fusion) can be demonstrated end to end.
Swap in real model calls (e.g. a local model, or a call out to a vision/NLP
service) inside `AnalysisEngine.analyze(...)` when ready — the rest of the
app (screens, components, data flow) does not need to change.

The "Prototype Multimodal Fusion" combined result is intentionally simple
(it looks at whether each signal is broadly negative or positive) and is
labeled as a prototype in the Results screen, along with a note that
differences between the facial and textual signals are normal.
