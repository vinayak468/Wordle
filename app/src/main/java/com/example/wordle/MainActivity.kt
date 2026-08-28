package com.example.wordle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var theme by remember { mutableStateOf("System") }

            val dark = when (theme) {
                "Dark", "AMOLED" -> true
                "Light" -> false
                else -> isSystemInDarkTheme()
            }

            val colors = when (theme) {
                "AMOLED" -> darkColorScheme(
                    background = Color.Black,
                    surface = Color.Black,
                    surfaceVariant = Color(0xFF1A1A1A),
                    outline = Color(0xFF444444)
                )
                else -> if (dark) darkColorScheme() else lightColorScheme()
            }

            MaterialTheme(colorScheme = colors) {
                WordleApp(
                    theme = theme,
                    onThemeChange = { theme = it }
                )
            }
        }
    }
}

@Composable
fun WordleApp(
    theme: String,
    onThemeChange: (String) -> Unit
) {
    var screen by remember { mutableStateOf("game") }

    when (screen) {
        "settings" -> SettingsScreen(
            theme = theme,
            onThemeChange = onThemeChange,
            onBack = { screen = "game" }
        )

        "help" -> HelpScreen(
            onBack = { screen = "game" }
        )

        else -> WordleScreen(
            onSettings = { screen = "settings" },
            onHelp = { screen = "help" }
        )
    }
}

@Composable
fun WordleScreen(
    onSettings: () -> Unit,
    onHelp: () -> Unit
) {
    val context = LocalContext.current

    val answers = remember {
        WordListLoader.load(context, "answers.txt")
    }

    val validGuesses = remember {
        WordListLoader.load(context, "valid_guesses.txt").toSet()
    }

    var answer by remember {
        mutableStateOf(WordleLogic.getTodaysWord(answers))
    }

    var guesses by remember {
        mutableStateOf(listOf<String>())
    }

    var evaluations by remember {
        mutableStateOf(listOf<List<TileState>>())
    }

    var currentGuess by remember {
        mutableStateOf("")
    }

    var message by remember {
        mutableStateOf("")
    }

    var gameOver by remember {
        mutableStateOf(false)
    }

    fun resetGame() {
        answer = answers.random()
        guesses = emptyList()
        evaluations = emptyList()
        currentGuess = ""
        message = ""
        gameOver = false
    }

    fun submitGuess() {
        if (gameOver) return

        if (currentGuess.length != 5) {
            message = "Not enough letters"
            return
        }

        if (currentGuess !in validGuesses) {
            message = "Not in word list"
            return
        }

        val result = WordleLogic.evaluateGuess(
            currentGuess,
            answer
        )

        guesses = guesses + currentGuess
        evaluations = evaluations + listOf(result)

        currentGuess = ""
        message = ""

        if (WordleLogic.isWin(result)) {
            message = "You got it! 🎉"
            gameOver = true
        } else if (guesses.size >= 6) {
            message = "Out of tries. Word was $answer"
            gameOver = true
        }
    }

    fun pressKey(key: String) {
        if (gameOver) return

        when (key) {
            "ENTER" -> submitGuess()

            "DEL" -> {
                if (currentGuess.isNotEmpty()) {
                    currentGuess = currentGuess.dropLast(1)
                }
            }

            else -> {
                if (currentGuess.length < 5) {
                    currentGuess += key
                }
            }
        }
    }

    val letterStates = remember(evaluations, guesses) {
        val map = mutableMapOf<Char, TileState>()

        guesses.forEachIndexed { row, word ->
            word.forEachIndexed { col, char ->
                val state = evaluations[row][col]
                val old = map[char]

                fun rank(s: TileState) = when (s) {
                    TileState.CORRECT -> 3
                    TileState.PRESENT -> 2
                    TileState.ABSENT -> 1
                    TileState.EMPTY -> 0
                }

                if (old == null || rank(state) > rank(old)) {
                    map[char] = state
                }
            }
        }

        map
    }

    val keyboardRows = listOf(
        listOf("Q","W","E","R","T","Y","U","I","O","P"),
        listOf("A","S","D","F","G","H","J","K","L"),
        listOf("ENTER","Z","X","C","V","B","N","M","DEL")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "?", 
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { onHelp() }
                    .padding(8.dp)
            )

            Text(
                "WORDLE",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                "⚙",
                fontSize = 24.sp,
                modifier = Modifier
                    .clickable { onSettings() }
                    .padding(8.dp)
            )
        }

        Text(
            "Guess the word in 6 tries",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "GUESS THE FIVE-LETTER WORD",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        for (row in 0 until 6) {
            Row(
                modifier = Modifier.padding(2.dp)
            ) {
                val word = when {
                    row < guesses.size -> guesses[row]
                    row == guesses.size -> currentGuess
                    else -> ""
                }

                val eval = evaluations.getOrNull(row)

                for (col in 0 until 5) {
                    val letter = word.getOrNull(col)?.toString() ?: ""
                    val state = eval?.getOrNull(col) ?: TileState.EMPTY

                    val bg = when (state) {
                        TileState.CORRECT -> Color(0xFF6AAA64)
                        TileState.PRESENT -> Color(0xFFC9B458)
                        TileState.ABSENT -> Color(0xFF787C7E)
                        TileState.EMPTY ->
                            MaterialTheme.colorScheme.surface
                    }

                    val borderColor =
                        if (state == TileState.EMPTY)
                            MaterialTheme.colorScheme.outline
                        else
                            bg

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .padding(2.dp)
                            .background(
                                bg,
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                2.dp,
                                borderColor,
                                RoundedCornerShape(4.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            letter,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color =
                                if (state == TileState.EMPTY)
                                    MaterialTheme.colorScheme.onSurface
                                else
                                    Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (message.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    message,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(
                        horizontal = 18.dp,
                        vertical = 10.dp
                    )
                )
            }
        }

        if (gameOver) {
            Button(
                onClick = { resetGame() },
                modifier = Modifier.padding(top = 10.dp)
            ) {
                Text("PLAY AGAIN")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            "KEYBOARD",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            keyboardRows.forEach { row ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    row.forEach { key ->
                        val state =
                            letterStates[key.firstOrNull() ?: ' ']

                        val bg = when {
                            key == "ENTER" || key == "DEL" ->
                                MaterialTheme.colorScheme.surfaceVariant

                            state == TileState.CORRECT ->
                                Color(0xFF6AAA64)

                            state == TileState.PRESENT ->
                                Color(0xFFC9B458)

                            state == TileState.ABSENT ->
                                Color(0xFF787C7E)

                            else ->
                                MaterialTheme.colorScheme.surfaceVariant
                        }

                        Box(
                            modifier = Modifier
                                .padding(2.dp)
                                .height(48.dp)
                                .width(
                                    if (key.length > 1) 52.dp
                                    else 34.dp
                                )
                                .background(
                                    bg,
                                    RoundedCornerShape(5.dp)
                                )
                                .clickable {
                                    pressKey(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (key == "DEL") "⌫" else key,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color =
                                    if (
                                        state != null &&
                                        key.length == 1
                                    ) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "You have 6 guesses • Good luck!",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
    }
}

@Composable
fun SettingsScreen(
    theme: String,
    onThemeChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← BACK")
        }

        Text(
            "SETTINGS",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 24.dp
            )
        )

        Text(
            "THEME",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        listOf(
            "System",
            "Light",
            "Dark",
            "AMOLED"
        ).forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onThemeChange(option)
                    }
                    .padding(vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = theme == option,
                    onClick = {
                        onThemeChange(option)
                    }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    option,
                    fontSize = 17.sp
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Text(
            "GAME",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "• Six guesses per game\n" +
            "• Green = correct position\n" +
            "• Yellow = wrong position\n" +
            "• Gray = not in the word\n" +
            "• New games can be started after finishing",
            fontSize = 16.sp,
            lineHeight = 27.sp
        )
    }
}

@Composable
fun HelpScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← BACK")
        }

        Text(
            "HOW TO PLAY",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(
                top = 8.dp,
                bottom = 24.dp
            )
        )

        Text(
            "Guess the hidden five-letter word in six tries.",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "🟩 GREEN",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "The letter is correct and in the correct position.",
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            "🟨 YELLOW",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "The letter is in the word but in the wrong position.",
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            "⬜ GRAY",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "The letter isn't in the answer.",
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "GAME RULE",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "You get six guesses. If all six guesses are wrong, " +
            "the answer is revealed and the game ends.",
            fontSize = 16.sp,
            lineHeight = 27.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

fun Modifier.clickableSimple(
    onClick: () -> Unit
): Modifier = this.then(
    Modifier.clickable(onClick = onClick)
)
