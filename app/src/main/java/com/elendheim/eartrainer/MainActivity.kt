package com.elendheim.eartrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.elendheim.eartrainer.game.GameViewModel
import com.elendheim.eartrainer.ui.DifficultyScreen
import com.elendheim.eartrainer.ui.EarTrainerTheme
import com.elendheim.eartrainer.ui.GameScreen
import com.elendheim.eartrainer.ui.HomeScreen

private const val SCREEN_HOME = "home"
private const val SCREEN_GAME = "game"
private const val SCREEN_DIFFICULTY = "difficulty"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EarTrainerTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: GameViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(SCREEN_HOME) }
    val player by viewModel.playerState.collectAsState()

    BackHandler(enabled = screen != SCREEN_HOME) {
        screen = SCREEN_HOME
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (screen) {
            SCREEN_HOME -> androidx.compose.foundation.layout.Box(contentModifier) {
                HomeScreen(
                    player = player,
                    todayEpochDay = viewModel.todayEpochDay(),
                    onPlayFree = {
                        viewModel.startFreePlay()
                        screen = SCREEN_GAME
                    },
                    onPlayDaily = {
                        viewModel.startDaily()
                        screen = SCREEN_GAME
                    },
                    onOpenDifficulty = { screen = SCREEN_DIFFICULTY },
                )
            }
            SCREEN_GAME -> androidx.compose.foundation.layout.Box(contentModifier) {
                GameScreen(
                    viewModel = viewModel,
                    onExit = { screen = SCREEN_HOME },
                )
            }
            SCREEN_DIFFICULTY -> androidx.compose.foundation.layout.Box(contentModifier) {
                DifficultyScreen(
                    initial = player.difficulty,
                    flStyleOctaves = player.flStyleOctaves,
                    onSave = { viewModel.saveDifficulty(it) },
                    onSetFlStyleOctaves = { viewModel.setFlStyleOctaves(it) },
                    onBack = { screen = SCREEN_HOME },
                )
            }
        }
    }
}
