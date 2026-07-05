<p align="center">
  <img src="docs/logo.png" alt="Elendheim Ear Trainer" width="160" />
</p>

<h1 align="center">Elendheim Ear Trainer</h1>

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
- **Challenges**: fifteen set runs you unlock as you level up — one opens
  every five levels. Each is a fixed scenario, from "Warm Up" (one octave of
  white keys) all the way to "Full Roll" (the whole C2-C7 board, heard once).
  Clearing a challenge pays bonus XP and records your best score.

## Levels and XP

Every correct answer earns XP, and harder settings pay more. The home screen
shows your level, progress to the next one, and when your next challenge
unlocks. Leveling up is what opens new challenges, so there is always
something to reach for.

## Difficulty is yours to tune

- Note range, anywhere from one octave up to C2-C7
- White keys only, or the full twelve
- Replays per note: 1, 3, or unlimited
- An optional middle-C reference before each note (turn it off for more XP)
- Octave naming: scientific (middle C = C4) or FL Studio style (C5)
- Four one-tap presets, from "First steps" to "Full roll"

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
every push; grab it from the workflow run's artifacts, or download a tagged
build from the [Releases](../../releases) page.
