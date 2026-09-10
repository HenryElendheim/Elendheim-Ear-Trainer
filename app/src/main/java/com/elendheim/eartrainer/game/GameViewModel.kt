package com.elendheim.eartrainer.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.eartrainer.audio.TonePlayer
import com.elendheim.eartrainer.audio.VoiceListener
import com.elendheim.eartrainer.data.PlayerState
import com.elendheim.eartrainer.data.ProgressRepository
import com.elendheim.eartrainer.model.Challenge
import com.elendheim.eartrainer.model.Difficulty
import com.elendheim.eartrainer.model.Leveling
import com.elendheim.eartrainer.model.Note
import com.elendheim.eartrainer.model.Progression
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

enum class Mode { FREE, DAILY, CHALLENGE }

enum class Phase { IDLE, GUESSING, ANSWERED, DONE }

data class GameUiState(
    val mode: Mode = Mode.FREE,
    val phase: Phase = Phase.IDLE,
    val questionNumber: Int = 0,       // 1-based
    val questionCount: Int = 0,        // 0 means endless
    val correctCount: Int = 0,
    val answeredCount: Int = 0,
    val streak: Int = 0,
    val replaysLeft: Int = 0,
    val targetMidi: Int = -1,
    val guessMidi: Int = -1,
    val wasCorrect: Boolean = false,
    val sessionXp: Int = 0,
    val bonusXp: Int = 0,
    val newDailyStreak: Int = 0,
    val challengeName: String? = null,
    val previousBest: Int = -1,        // best score for this challenge before this run
    val leveledUpTo: Int = 0,          // > 0 while a level-up banner should show
    val difficulty: Difficulty = Difficulty(),
    val flStyleOctaves: Boolean = false,
    // Voice mode
    val voiceMode: Boolean = false,
    val voiceAnyOctave: Boolean = true,
    /** Fractional MIDI of what the mic hears right now; -1 for nothing. */
    val livePitchMidi: Float = -1f,
    val isCapturing: Boolean = false,
    val voiceHint: String? = null,
    val answeredByVoice: Boolean = false,
    /** Roman-numeral figures of the loop backing this run, if any. */
    val progressionFigures: String? = null,
)

private data class RunQuestion(val midi: Int, val difficulty: Difficulty)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgressRepository(application)
    private val tonePlayer = TonePlayer()
    private val voiceListener = VoiceListener()

    val playerState: StateFlow<PlayerState> = repository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerState())

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var freePlayRandom = Random(System.nanoTime())
    private var runQuestions: List<RunQuestion> = emptyList()
    private var xpBeforeSession = 0
    private var currentDay = 0L
    private var activeChallenge: Challenge? = null
    private var activeProgression: Progression? = null
    private var progressionKeyRoot = 48
    private var captureJob: Job? = null

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun startFreePlay() {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            activeChallenge = null
            activeProgression = null
            _uiState.value = GameUiState(
                mode = Mode.FREE,
                phase = Phase.GUESSING,
                questionNumber = 1,
                questionCount = 0,
                replaysLeft = player.difficulty.maxReplays,
                targetMidi = player.difficulty.randomNote(freePlayRandom),
                difficulty = player.difficulty,
                flStyleOctaves = player.flStyleOctaves,
                voiceMode = player.voiceMode,
                voiceAnyOctave = player.voiceAnyOctave,
            )
            playTarget()
        }
    }

    fun startDaily() {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            activeChallenge = null
            activeProgression = null
            currentDay = todayEpochDay()
            runQuestions = buildDailyQuestions(currentDay)
            val first = runQuestions.first()
            _uiState.value = GameUiState(
                mode = Mode.DAILY,
                phase = Phase.GUESSING,
                questionNumber = 1,
                questionCount = runQuestions.size,
                replaysLeft = first.difficulty.maxReplays,
                targetMidi = first.midi,
                difficulty = first.difficulty,
                flStyleOctaves = player.flStyleOctaves,
                voiceMode = player.voiceMode,
                voiceAnyOctave = player.voiceAnyOctave,
            )
            playTarget()
        }
    }

    fun startChallenge(challenge: Challenge) {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            activeChallenge = challenge
            activeProgression = challenge.progression
            progressionKeyRoot = challenge.keyRootMidi
            val random = Random(System.nanoTime())
            val pool = notePool(challenge)
            runQuestions = List(challenge.questionCount) {
                RunQuestion(pool.random(random), challenge.difficulty)
            }
            val first = runQuestions.first()
            _uiState.value = GameUiState(
                mode = Mode.CHALLENGE,
                phase = Phase.GUESSING,
                questionNumber = 1,
                questionCount = runQuestions.size,
                replaysLeft = first.difficulty.maxReplays,
                targetMidi = first.midi,
                difficulty = first.difficulty,
                flStyleOctaves = player.flStyleOctaves,
                challengeName = challenge.name,
                previousBest = player.bestFor(challenge.id) ?: -1,
                voiceMode = player.voiceMode,
                voiceAnyOctave = player.voiceAnyOctave,
                progressionFigures = challenge.progression?.figures,
            )
            playTarget()
        }
    }

    /**
     * Notes a challenge may ask for. With a progression, the melody is kept to
     * that key's scale so every answer actually sits in the harmony.
     */
    private fun notePool(challenge: Challenge): List<Int> {
        val progression = challenge.progression ?: return challenge.difficulty.candidates()
        val pitchClasses = progression.scalePitchClasses(challenge.keyRootMidi)
        val inKey = (challenge.difficulty.lowMidi..challenge.difficulty.highMidi)
            .filter { Math.floorMod(it, 12) in pitchClasses }
        return inKey.ifEmpty { challenge.difficulty.candidates() }
    }

    /**
     * The same date always produces the same ten notes, ramping from one
     * octave of white keys up to the full roll with no anchor.
     */
    private fun buildDailyQuestions(epochDay: Long): List<RunQuestion> {
        val random = Random(epochDay * 31L + 7L)
        val stages = listOf(
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = 3, referenceC = true),
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = 3, referenceC = true),
            Difficulty(60, 71, includeBlackKeys = false, maxReplays = 3, referenceC = true),
            Difficulty(60, 83, includeBlackKeys = false, maxReplays = 3, referenceC = true),
            Difficulty(60, 83, includeBlackKeys = false, maxReplays = 3, referenceC = true),
            Difficulty(60, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
            Difficulty(60, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
            Difficulty(48, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
            Difficulty(48, 83, includeBlackKeys = true, maxReplays = 3, referenceC = false),
            Difficulty(36, 96, includeBlackKeys = true, maxReplays = 3, referenceC = false),
        )
        return stages.map { stage -> RunQuestion(stage.randomNote(random), stage) }
    }

    fun playTarget() {
        val state = _uiState.value
        if (state.targetMidi < 0) return
        val progression = activeProgression
        viewModelScope.launch {
            if (progression != null) {
                // The loop is the anchor: it sets the key before the question.
                tonePlayer.playChords(
                    progression.chords(progressionKeyRoot).map { it.notes() },
                    CHORD_STEP_MILLIS,
                )
                delay(CHORD_STEP_MILLIS)
                tonePlayer.playSequence(listOf(state.targetMidi))
            } else if (state.difficulty.referenceC) {
                tonePlayer.playSequence(listOf(Note.MIDDLE_C, state.targetMidi))
            } else {
                tonePlayer.playSequence(listOf(state.targetMidi))
            }
        }
    }

    fun replay() {
        val state = _uiState.value
        if (state.phase != Phase.GUESSING) return
        val unlimited = state.difficulty.maxReplays >= Difficulty.REPLAYS_UNLIMITED
        if (!unlimited && state.replaysLeft <= 0) return
        if (!unlimited) {
            _uiState.value = state.copy(replaysLeft = state.replaysLeft - 1)
        }
        playTarget()
    }

    /** Plays what the player answered, then what the note actually was. */
    fun compareGuessToTarget() {
        val state = _uiState.value
        if (state.phase != Phase.ANSWERED || state.guessMidi < 0) return
        viewModelScope.launch {
            tonePlayer.playSequence(listOf(state.guessMidi, state.targetMidi))
        }
    }

    // --- Voice mode -------------------------------------------------------

    /** Live tuner: lets the player hunt for the note before committing. */
    fun startListening() {
        if (voiceListener.isRunning) return
        voiceListener.start(viewModelScope) { midi ->
            _uiState.update { it.copy(livePitchMidi = midi) }
        }
    }

    fun stopListening() {
        captureJob?.cancel()
        captureJob = null
        voiceListener.stop()
        _uiState.update { it.copy(livePitchMidi = -1f, isCapturing = false) }
    }

    /**
     * Records for a moment and answers with the steadiest note heard. Taking
     * the median rides out the wobble at the start and end of a sung note.
     */
    fun captureSungNote() {
        val state = _uiState.value
        if (state.phase != Phase.GUESSING || captureJob != null) return
        _uiState.update { it.copy(isCapturing = true, voiceHint = null) }
        captureJob = viewModelScope.launch {
            val heard = mutableListOf<Float>()
            var elapsed = 0L
            while (elapsed < CAPTURE_MILLIS) {
                val pitch = _uiState.value.livePitchMidi
                if (pitch > 0f) heard.add(pitch)
                delay(CAPTURE_POLL_MILLIS)
                elapsed += CAPTURE_POLL_MILLIS
            }
            captureJob = null
            _uiState.update { it.copy(isCapturing = false) }
            if (heard.size < MIN_CAPTURE_SAMPLES) {
                _uiState.update {
                    it.copy(voiceHint = "Didn't catch a steady note. Hold it a little longer.")
                }
                return@launch
            }
            heard.sort()
            submitSungNote(heard[heard.size / 2])
        }
    }

    private fun submitSungNote(midi: Float) {
        val state = _uiState.value
        if (state.phase != Phase.GUESSING) return
        val sung = midi.roundToInt()
        val correct = if (state.voiceAnyOctave) {
            Math.floorMod(sung - state.targetMidi, 12) == 0
        } else {
            sung == state.targetMidi
        }
        applyGuess(sung, correct, byVoice = true)
    }

    // --- Answering --------------------------------------------------------

    fun submitGuess(guessMidi: Int) {
        val state = _uiState.value
        if (state.phase != Phase.GUESSING) return
        applyGuess(guessMidi, guessMidi == state.targetMidi, byVoice = false)
    }

    private fun applyGuess(guessMidi: Int, correct: Boolean, byVoice: Boolean) {
        val state = _uiState.value
        val newStreak = if (correct) state.streak + 1 else 0

        val xpGained = if (correct) {
            val base = when (state.mode) {
                Mode.FREE -> state.difficulty.xpPerCorrect()
                Mode.DAILY -> DAILY_XP_PER_CORRECT
                Mode.CHALLENGE -> state.difficulty.xpPerCorrect()
            }
            // A running streak pays up to double XP.
            val multiplier = min(2.0, 1.0 + newStreak * 0.1)
            (base * multiplier).toInt()
        } else {
            0
        }

        val newSessionXp = state.sessionXp + xpGained
        val levelBefore = Leveling.levelForXp(xpBeforeSession + state.sessionXp)
        val levelAfter = Leveling.levelForXp(xpBeforeSession + newSessionXp)

        _uiState.value = state.copy(
            phase = Phase.ANSWERED,
            guessMidi = guessMidi,
            wasCorrect = correct,
            answeredByVoice = byVoice,
            correctCount = state.correctCount + if (correct) 1 else 0,
            answeredCount = state.answeredCount + 1,
            streak = newStreak,
            sessionXp = newSessionXp,
            leveledUpTo = if (levelAfter > levelBefore) levelAfter else 0,
            voiceHint = null,
        )

        viewModelScope.launch {
            repository.recordAnswer(correct, newStreak, xpGained)
        }
    }

    fun next() {
        val state = _uiState.value
        if (state.phase != Phase.ANSWERED) return
        when (state.mode) {
            Mode.FREE -> {
                _uiState.value = state.copy(
                    phase = Phase.GUESSING,
                    questionNumber = state.questionNumber + 1,
                    replaysLeft = state.difficulty.maxReplays,
                    targetMidi = state.difficulty.randomNote(freePlayRandom),
                    guessMidi = -1,
                    leveledUpTo = 0,
                    voiceHint = null,
                    answeredByVoice = false,
                )
                playTarget()
            }
            Mode.DAILY, Mode.CHALLENGE -> {
                if (state.questionNumber >= runQuestions.size) {
                    finishRun(state)
                } else {
                    val nextQuestion = runQuestions[state.questionNumber]
                    _uiState.value = state.copy(
                        phase = Phase.GUESSING,
                        questionNumber = state.questionNumber + 1,
                        replaysLeft = nextQuestion.difficulty.maxReplays,
                        targetMidi = nextQuestion.midi,
                        difficulty = nextQuestion.difficulty,
                        guessMidi = -1,
                        leveledUpTo = 0,
                        voiceHint = null,
                        answeredByVoice = false,
                    )
                    playTarget()
                }
            }
        }
    }

    private fun finishRun(state: GameUiState) {
        viewModelScope.launch {
            var newDailyStreak = 0
            val bonus: Int
            when (state.mode) {
                Mode.DAILY -> {
                    newDailyStreak = repository.completeDaily(currentDay, state.correctCount)
                    bonus = DAILY_COMPLETION_BONUS + min(newDailyStreak * 5, 50)
                    repository.addXp(bonus)
                }
                Mode.CHALLENGE -> {
                    // Clearing pays the most; partial runs still earn a little.
                    val cleared = state.correctCount == state.questionCount
                    bonus = if (cleared) CHALLENGE_CLEAR_BONUS else state.correctCount * 2
                    activeChallenge?.let {
                        repository.recordChallengeResult(it.id, state.correctCount, bonus)
                    }
                }
                Mode.FREE -> bonus = 0
            }
            val totalXp = xpBeforeSession + state.sessionXp + bonus
            val levelBefore = Leveling.levelForXp(xpBeforeSession + state.sessionXp)
            val levelAfter = Leveling.levelForXp(totalXp)
            _uiState.value = state.copy(
                phase = Phase.DONE,
                sessionXp = state.sessionXp + bonus,
                bonusXp = bonus,
                newDailyStreak = newDailyStreak,
                leveledUpTo = if (levelAfter > levelBefore) levelAfter else 0,
            )
        }
    }

    // --- Settings ---------------------------------------------------------

    fun saveDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { repository.saveDifficulty(difficulty) }
    }

    fun setFlStyleOctaves(enabled: Boolean) {
        viewModelScope.launch { repository.setFlStyleOctaves(enabled) }
    }

    fun setVoiceMode(enabled: Boolean) {
        if (!enabled) stopListening()
        viewModelScope.launch { repository.setVoiceMode(enabled) }
    }

    fun setVoiceAnyOctave(enabled: Boolean) {
        viewModelScope.launch { repository.setVoiceAnyOctave(enabled) }
    }

    override fun onCleared() {
        voiceListener.stop()
        tonePlayer.release()
    }

    companion object {
        const val DAILY_XP_PER_CORRECT = 12
        const val DAILY_COMPLETION_BONUS = 30
        const val CHALLENGE_CLEAR_BONUS = 40
        private const val CHORD_STEP_MILLIS = 900L
        private const val CAPTURE_MILLIS = 1600L
        private const val CAPTURE_POLL_MILLIS = 40L
        private const val MIN_CAPTURE_SAMPLES = 6
    }
}
