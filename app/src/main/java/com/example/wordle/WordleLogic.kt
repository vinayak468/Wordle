package com.example.wordle

import java.time.LocalDate

enum class TileState { CORRECT, PRESENT, ABSENT, EMPTY }

object WordleLogic {

    fun getTodaysWord(answers: List<String>): String {
        val epochDay = LocalDate.now().toEpochDay()
        val index = (epochDay % answers.size).toInt().let {
            if (it < 0) it + answers.size else it
        }
        return answers[index]
    }

    fun evaluateGuess(guess: String, answer: String): List<TileState> {
        val g = guess.uppercase()
        val a = answer.uppercase()
        val result = MutableList(5) { TileState.ABSENT }
        val answerChars = a.toCharArray()
        val used = BooleanArray(5)

        for (i in 0..4) {
            if (g[i] == answerChars[i]) {
                result[i] = TileState.CORRECT
                used[i] = true
            }
        }
        for (i in 0..4) {
            if (result[i] == TileState.CORRECT) continue
            for (j in 0..4) {
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
