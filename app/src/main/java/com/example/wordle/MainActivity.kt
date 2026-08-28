package com.example.wordle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val Correct = Color(0xFF538D4E)
private val Present = Color(0xFFB59F3B)
private val Absent = Color(0xFF787C7E)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WordleApp() }
    }
}

@Composable
private fun WordleApp(viewModel: WordleViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("game") }
    val dark = when (state.theme) {
        AppTheme.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.AMOLED -> true
    }
    val colors = if (state.theme == AppTheme.AMOLED) darkColorScheme(background = Color.Black, surface = Color.Black) else if (dark) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (screen) {
                "settings" -> SettingsScreen(state, viewModel::setTheme, viewModel::setMode) { screen = "game" }
                "help" -> HelpScreen { screen = "game" }
                else -> GameScreen(state, viewModel::pressKey, viewModel::startNewGame, { screen = "settings" }, { screen = "help" })
            }
        }
    }
}

@Composable
private fun GameScreen(
    state: GameUiState,
    onKey: (String) -> Unit,
    onNewGame: () -> Unit,
    onSettings: () -> Unit,
    onHelp: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onHelp, modifier = Modifier.semantics { contentDescription = "How to play" }) { Text("?", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("WORDLE", fontSize = 30.sp, fontWeight = FontWeight.Black)
                Text(state.mode.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "Settings" }) { Text("⚙", fontSize = 22.sp) }
        }
        HorizontalDivider()
        Spacer(Modifier.height(8.dp))
        Board(state, Modifier.fillMaxWidth().weight(1f))
        if (state.message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(state.message, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp))
        }
        if (state.gameOver) Button(onClick = onNewGame, modifier = Modifier.padding(top = 8.dp)) { Text(if (state.mode == GameMode.DAILY) "PLAY TODAY AGAIN" else "NEW GAME") }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(Modifier.height(4.dp))
        Keyboard(state, onKey)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Board(state: GameUiState, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val tileSize = minOf(
            54.dp,
            (maxWidth - 20.dp) / WordleLogic.WORD_LENGTH,
            (maxHeight - 24.dp) / WordleLogic.MAX_GUESSES
        ).coerceAtLeast(24.dp)
        val evaluations = state.evaluations
        Column {
            repeat(WordleLogic.MAX_GUESSES) { row ->
                val word = when {
                    row < state.guesses.size -> state.guesses[row]
                    row == state.guesses.size && !state.gameOver -> state.currentGuess
                    else -> ""
                }
                Row {
                    repeat(WordleLogic.WORD_LENGTH) { col ->
                        val tileState = evaluations.getOrNull(row)?.getOrNull(col) ?: TileState.EMPTY
                        Tile(word.getOrNull(col)?.toString().orEmpty(), tileState, tileSize)
                    }
                }
            }
        }
    }
}

@Composable
private fun Tile(letter: String, state: TileState, size: androidx.compose.ui.unit.Dp) {
    val color = tileColor(state)
    Box(
        modifier = Modifier.padding(2.dp).size(size).background(color, RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(letter, fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (state == TileState.EMPTY) MaterialTheme.colorScheme.onSurface else Color.White)
    }
}

@Composable
private fun Keyboard(state: GameUiState, onKey: (String) -> Unit) {
    val statuses = state.guesses.flatMapIndexed { row, word -> word.mapIndexed { col, letter -> letter to state.evaluations[row][col] } }
        .groupBy({ it.first }, { it.second }).mapValues { (_, values) -> values.maxBy { rank(it) } }
    val rows = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("ENTER", "Z", "X", "C", "V", "B", "N", "M", "DEL")
    )
    rows.forEach { row ->
        Row(Modifier.fillMaxWidth().height(52.dp), horizontalArrangement = Arrangement.Center) {
            row.forEach { key ->
                val isSpecial = key == "ENTER" || key == "DEL"
                val color = if (isSpecial) MaterialTheme.colorScheme.surfaceVariant else tileColor(statuses[key.first()] ?: TileState.EMPTY)
                Button(
                    onClick = { onKey(key) },
                    modifier = Modifier.padding(2.dp).height(48.dp).weight(if (isSpecial) 1.5f else 1f).semantics { contentDescription = if (key == "DEL") "Delete letter" else key },
                    shape = RoundedCornerShape(5.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = color, contentColor = if (isSpecial || color == MaterialTheme.colorScheme.surface) MaterialTheme.colorScheme.onSurface else Color.White)
                ) { Text(if (key == "DEL") "⌫" else key, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun tileColor(state: TileState): Color = when (state) {
    TileState.CORRECT -> Correct
    TileState.PRESENT -> Present
    TileState.ABSENT -> Absent
    TileState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
}

private fun rank(state: TileState) = when (state) { TileState.CORRECT -> 3; TileState.PRESENT -> 2; TileState.ABSENT -> 1; TileState.EMPTY -> 0 }

@Composable
private fun SettingsScreen(state: GameUiState, onTheme: (AppTheme) -> Unit, onMode: (GameMode) -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Button(onClick = onBack) { Text("BACK") }
        Spacer(Modifier.height(16.dp))
        Text("SETTINGS", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        SettingGroup("THEME", AppTheme.entries, state.theme, { it.label }, onTheme)
        HorizontalDivider(Modifier.padding(vertical = 16.dp))
        SettingGroup("PLAY MODE", GameMode.entries, state.mode, { it.label }, onMode)
        Text("Changing play mode starts a new game.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun <T> SettingGroup(title: String, items: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    items.forEach { item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == item, onClick = { onSelect(item) })
            Button(onClick = { onSelect(item) }, colors = ButtonDefaults.textButtonColors()) { Text(label(item)) }
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Button(onClick = onBack) { Text("BACK") }
        Spacer(Modifier.height(16.dp))
        Text("HOW TO PLAY", fontSize = 30.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(20.dp))
        Text("Guess the five-letter word in six tries. Green means the right letter in the right place, yellow means the letter is elsewhere, and gray means it is not in the word.", fontSize = 17.sp, lineHeight = 26.sp)
    }
}
