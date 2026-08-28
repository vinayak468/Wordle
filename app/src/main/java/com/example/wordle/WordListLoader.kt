package com.example.wordle

import android.content.Context

object WordListLoader {
    fun load(context: Context, filename: String): List<String> {
        return context.assets.open(filename).bufferedReader().useLines { lines ->
            lines.map { it.trim().uppercase() }.filter { it.length == 5 }.toList()
        }
    }
}
