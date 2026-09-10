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
- **Challenges**: eighteen set runs you unlock as you level up — one opens
  every five levels. Each is a fixed scenario, from "Warm Up" (one octave of
  white keys) all the way to "Full Roll" (the whole C2-C7 board, heard once).
  Clearing a challenge pays bonus XP and records your best score.
- **Progression challenges**: three of those runs swap the reference C for a
  chord loop — Chill (Imaj7 - vi7 - ii7 - V7), Pop (I - V - vi - IV) and Sad
  (i - VI - III - VII). The loop sets the key, then you name the note sitting
  inside it, which is much closer to how you hear notes in a real track.

## Levels and XP

Every correct answer earns XP, and harder settings pay more. The home screen
shows your level, progress to the next one, and when your next challenge
unlocks. Leveling up is what opens new challenges, so there is always
something to reach for.

## Voice mode

Switch on **Sing the answer** in the settings and you name the note with your
voice instead of your finger. It is off by default; with it off the app
behaves exactly as it does above.

- A **live tuner** shows the note you are humming as you hum it, so you can
  hunt for the pitch before you commit to it.
- When it feels right, hold the note and tap **Lock it in**. The app listens
  for a moment and takes the steadiest pitch as your answer, so a wobbly
  start or finish does not count against you.
- **Any octave counts** by default, because nobody can sing C7. Match the
  note name in whatever octave suits your range and it is correct.
- Pitch is detected on device with the YIN algorithm. Nothing is recorded,
  stored, or sent anywhere, and the microphone is only ever opened while a
  question is on screen with voice mode switched on.

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
- Pitch detection is a hand-rolled YIN implementation over raw AudioRecord
  frames, so voice mode adds no dependencies either
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
