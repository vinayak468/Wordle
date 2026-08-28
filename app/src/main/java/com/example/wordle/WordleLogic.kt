package com.example.wordle

import java.time.LocalDate

enum class TileState { CORRECT, PRESENT, ABSENT, EMPTY }

object WordleLogic {
    const val WORD_LENGTH = 5
    const val MAX_GUESSES = 6

    fun getTodaysWord(answers: List<String>): String {
        require(answers.isNotEmpty()) { "At least one answer is required." }
        val epochDay = LocalDate.now().toEpochDay()
        val index = (epochDay % answers.size).toInt().let {
            if (it < 0) it + answers.size else it
        }
        return answers[index]
    }

    fun evaluateGuess(guess: String, answer: String): List<TileState> {
        val g = guess.uppercase()
        val a = answer.uppercase()
        require(g.length == WORD_LENGTH && a.length == WORD_LENGTH) {
            "Guess and answer must both be $WORD_LENGTH letters."
        }
        val result = MutableList(WORD_LENGTH) { TileState.ABSENT }
        val answerChars = a.toCharArray()
        val used = BooleanArray(WORD_LENGTH)

        for (i in 0 until WORD_LENGTH) {
            if (g[i] == answerChars[i]) {
                result[i] = TileState.CORRECT
                used[i] = true
            }
        }
        for (i in 0 until WORD_LENGTH) {
            if (result[i] == TileState.CORRECT) continue
            for (j in 0 until WORD_LENGTH) {
                if (!used[j] && g[i] == answerChars[j]) {
                    result[i] = TileState.PRESENT
                    used[j] = true
                    break
                }
            }
        }
        return result
    }

    fun isWin(states: List<TileState>): Boolean = states.all { it == TileState.CORRECT }
}
