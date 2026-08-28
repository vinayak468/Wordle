package com.example.wordle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GameMode(val label: String) { DAILY("Daily"), UNLIMITED("Unlimited") }
enum class AppTheme(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark"), AMOLED("AMOLED") }

data class GameUiState(
    val answer: String = "",
    val guesses: List<String> = emptyList(),
    val currentGuess: String = "",
    val message: String = "",
    val gameOver: Boolean = false,
    val mode: GameMode = GameMode.DAILY,
    val theme: AppTheme = AppTheme.SYSTEM
) {
    val evaluations: List<List<TileState>> get() = guesses.map { WordleLogic.evaluateGuess(it, answer) }
}

class WordleViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = application.getSharedPreferences("wordle", Application.MODE_PRIVATE)
    private val answers = WordListLoader.load(application, "answers.txt")
    private val validGuesses = WordListLoader.load(application, "valid_guesses.txt").toSet()
    private val _uiState = MutableStateFlow(restore())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        check(answers.isNotEmpty()) { "The answer list is empty." }
        check(answers.all { it in validGuesses }) { "Every answer must be a valid guess." }
    }

    fun pressKey(key: String) {
        val state = _uiState.value
        if (state.gameOver) return
        when (key) {
            "ENTER" -> submitGuess()
            "DEL" -> _uiState.update { it.copy(currentGuess = it.currentGuess.dropLast(1), message = "") }
            else -> if (key.length == 1 && key[0].isLetter() && state.currentGuess.length < WordleLogic.WORD_LENGTH) {
                _uiState.update { it.copy(currentGuess = it.currentGuess + key.uppercase(), message = "") }
            }
        }
        persist()
    }

    fun submitGuess() {
        val state = _uiState.value
        if (state.gameOver) return
        when {
            state.currentGuess.length != WordleLogic.WORD_LENGTH -> updateMessage("Not enough letters")
            state.currentGuess !in validGuesses -> updateMessage("Not in word list")
            else -> {
                val result = WordleLogic.evaluateGuess(state.currentGuess, state.answer)
                val guesses = state.guesses + state.currentGuess
                val won = WordleLogic.isWin(result)
                val exhausted = guesses.size >= WordleLogic.MAX_GUESSES
                _uiState.value = state.copy(
                    guesses = guesses,
                    currentGuess = "",
                    gameOver = won || exhausted,
                    message = when {
                        won -> "You got it!"
                        exhausted -> "Out of tries. Word was ${state.answer}"
                        else -> ""
                    }
                )
                persist()
            }
        }
    }

    fun startNewGame() {
        val mode = _uiState.value.mode
        _uiState.value = newGame(mode).copy(theme = _uiState.value.theme)
        persist()
    }

    fun setMode(mode: GameMode) {
        _uiState.value = newGame(mode).copy(theme = _uiState.value.theme)
        persist()
    }

    fun setTheme(theme: AppTheme) {
        _uiState.update { it.copy(theme = theme) }
        preferences.edit().putString(KEY_THEME, theme.name).apply()
        persist()
    }

    private fun updateMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    private fun restore(): GameUiState {
        val savedMode = enumValue(preferences.getString(KEY_MODE, null), GameMode.DAILY)
        val savedTheme = enumValue(preferences.getString(KEY_THEME, null), AppTheme.SYSTEM)
        val savedDate = preferences.getString(KEY_DATE, null)
        val answer = preferences.getString(KEY_ANSWER, null)
        val guesses = preferences.getString(KEY_GUESSES, "")!!.split(',').filter { it.length == WordleLogic.WORD_LENGTH }
        val canRestore = answer in answers && (savedMode == GameMode.UNLIMITED || savedDate == LocalDate.now().toString())
        return if (canRestore) {
            GameUiState(answer = answer!!, guesses = guesses, gameOver = preferences.getBoolean(KEY_OVER, false), mode = savedMode, theme = savedTheme)
        } else newGame(savedMode).copy(theme = savedTheme)
    }

    private fun newGame(mode: GameMode): GameUiState = GameUiState(
        answer = if (mode == GameMode.DAILY) WordleLogic.getTodaysWord(answers) else answers.random(),
        mode = mode
    )


    private inline fun <reified T : Enum<T>> enumValue(value: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: fallback

    private fun persist() {
        val state = _uiState.value
        preferences.edit()
            .putString(KEY_MODE, state.mode.name)
            .putString(KEY_THEME, state.theme.name)
            .putString(KEY_DATE, LocalDate.now().toString())
            .putString(KEY_ANSWER, state.answer)
            .putString(KEY_GUESSES, state.guesses.joinToString(","))
            .putBoolean(KEY_OVER, state.gameOver)
            .apply()
    }

    private companion object {
        const val KEY_MODE = "mode"
        const val KEY_THEME = "theme"
        const val KEY_DATE = "date"
        const val KEY_ANSWER = "answer"
        const val KEY_GUESSES = "guesses"
        const val KEY_OVER = "game_over"
    }
}
