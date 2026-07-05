# Elendheim Ear Trainer

An Android ear trainer that speaks piano-roll. The app plays a note, you tap
the key you think it was, and it tells you how close you were in plain
language: "It was C5. You said A4 — 3 keys too low." No interval names, no
theory jargon.

## How it plays

- **Free play**: endless notes at your own difficulty settings. Build a
  streak for up to double XP per correct answer.
- **Daily challenge**: ten notes, the same set for the whole day, ramping
  from one octave of white keys up to the full C2-C7 roll. Finishing keeps
  your daily streak alive and pays a bonus.
- **Levels**: every correct answer earns XP. Harder settings pay more.

## Difficulty is yours to tune

- Note range, anywhere from one octave up to C2-C7
- White keys only, or the full twelve
- Replays per note: 1, 3, or unlimited
- An optional middle-C reference before each note (turn it off for more XP)
- Octave naming: scientific (middle C = C4) or FL Studio style (C5)

## Tech

- Kotlin + Jetpack Compose, single module, no third-party runtime deps
- Notes are synthesized on device (additive synthesis with decaying
  harmonics), so there are no audio assets
- Progress is stored locally with DataStore; the app works fully offline
- Min SDK 26 (Android 8.0), target SDK 35

## Building

Open the project in Android Studio, or from the command line:

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`. CI builds one on
every push; grab it from the workflow run's artifacts.
