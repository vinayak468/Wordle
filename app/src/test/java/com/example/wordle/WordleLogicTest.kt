package com.example.wordle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordleLogicTest {
    @Test fun `duplicate letters are scored only as often as the answer contains them`() {
        assertEquals(
            listOf(TileState.ABSENT, TileState.ABSENT, TileState.PRESENT, TileState.PRESENT, TileState.CORRECT),
            WordleLogic.evaluateGuess("EERIE", "RINSE")
        )
    }

    @Test fun `exact letters take precedence over present letters`() {
        assertEquals(
            listOf(TileState.CORRECT, TileState.CORRECT, TileState.ABSENT, TileState.ABSENT, TileState.ABSENT),
            WordleLogic.evaluateGuess("ALLOT", "ALARM")
        )
    }

    @Test fun `today's word is deterministic`() {
        val words = listOf("ALPHA", "BRAVO", "CHARM")
        assertEquals(WordleLogic.getTodaysWord(words), WordleLogic.getTodaysWord(words))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `short guesses are rejected`() {
        WordleLogic.evaluateGuess("TOO", "WORDS")
    }

    @Test fun `all correct tiles win`() {
        assertTrue(WordleLogic.isWin(List(WordleLogic.WORD_LENGTH) { TileState.CORRECT }))
    }
}
