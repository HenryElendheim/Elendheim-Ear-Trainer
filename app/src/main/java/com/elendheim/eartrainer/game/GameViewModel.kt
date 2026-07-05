package com.elendheim.eartrainer.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.eartrainer.audio.TonePlayer
import com.elendheim.eartrainer.data.PlayerState
import com.elendheim.eartrainer.data.ProgressRepository
import com.elendheim.eartrainer.model.Challenge
import com.elendheim.eartrainer.model.Difficulty
import com.elendheim.eartrainer.model.Leveling
import com.elendheim.eartrainer.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.min
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
)

private data class RunQuestion(val midi: Int, val difficulty: Difficulty)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProgressRepository(application)
    private val tonePlayer = TonePlayer()

    val playerState: StateFlow<PlayerState> = repository.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, PlayerState())

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState

    private var freePlayRandom = Random(System.nanoTime())
    private var runQuestions: List<RunQuestion> = emptyList()
    private var xpBeforeSession = 0
    private var currentDay = 0L
    private var activeChallenge: Challenge? = null

    fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

    fun startFreePlay() {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            _uiState.value = GameUiState(
                mode = Mode.FREE,
                phase = Phase.GUESSING,
                questionNumber = 1,
                questionCount = 0,
                replaysLeft = player.difficulty.maxReplays,
                targetMidi = player.difficulty.randomNote(freePlayRandom),
                difficulty = player.difficulty,
                flStyleOctaves = player.flStyleOctaves,
            )
            playTarget()
        }
    }

    fun startDaily() {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            activeChallenge = null
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
            )
            playTarget()
        }
    }

    fun startChallenge(challenge: Challenge) {
        viewModelScope.launch {
            val player = repository.state.first()
            xpBeforeSession = player.xp
            activeChallenge = challenge
            val random = Random(System.nanoTime())
            runQuestions = List(challenge.questionCount) {
                RunQuestion(challenge.difficulty.randomNote(random), challenge.difficulty)
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
            )
            playTarget()
        }
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
        viewModelScope.launch {
            val sequence = if (state.difficulty.referenceC) {
                listOf(Note.MIDDLE_C, state.targetMidi)
            } else {
                listOf(state.targetMidi)
            }
            tonePlayer.playSequence(sequence)
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

    /** Plays what the player guessed, then what the note actually was. */
    fun compareGuessToTarget() {
        val state = _uiState.value
        if (state.phase != Phase.ANSWERED || state.guessMidi < 0) return
        viewModelScope.launch {
            tonePlayer.playSequence(listOf(state.guessMidi, state.targetMidi))
        }
    }

    fun submitGuess(guessMidi: Int) {
        val state = _uiState.value
        if (state.phase != Phase.GUESSING) return
        val correct = guessMidi == state.targetMidi
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
            correctCount = state.correctCount + if (correct) 1 else 0,
            answeredCount = state.answeredCount + 1,
            streak = newStreak,
            sessionXp = newSessionXp,
            leveledUpTo = if (levelAfter > levelBefore) levelAfter else 0,
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

    fun saveDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { repository.saveDifficulty(difficulty) }
    }

    fun setFlStyleOctaves(enabled: Boolean) {
        viewModelScope.launch { repository.setFlStyleOctaves(enabled) }
    }

    override fun onCleared() {
        tonePlayer.release()
    }

    companion object {
        const val DAILY_XP_PER_CORRECT = 12
        const val DAILY_COMPLETION_BONUS = 30
        const val CHALLENGE_CLEAR_BONUS = 40
    }
}
