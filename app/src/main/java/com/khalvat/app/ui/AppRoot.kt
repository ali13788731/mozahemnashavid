package com.khalvat.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.khalvat.app.data.Db
import com.khalvat.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Screens. Home → Choose (call / message) → Editor. No drawer, no bottom menu. */
sealed interface Screen {
    data object Home : Screen
    data object Choose : Screen
    data class Editor(val kind: Int, val ruleId: Long) : Screen
    data object Archive : Screen
    data object Settings : Screen
    data object People : Screen
}

private fun Screen.encode(): String = when (this) {
    Screen.Home -> "home"
    Screen.Choose -> "choose"
    is Screen.Editor -> "editor:$kind:$ruleId"
    Screen.Archive -> "archive"
    Screen.Settings -> "settings"
    Screen.People -> "people"
}

private fun decode(s: String): Screen {
    val p = s.split(":")
    return when (p[0]) {
        "choose" -> Screen.Choose
        "editor" -> Screen.Editor(p[1].toInt(), p[2].toLong())
        "archive" -> Screen.Archive
        "settings" -> Screen.Settings
        "people" -> Screen.People
        else -> Screen.Home
    }
}

private val StackSaver = listSaver<List<Screen>, String>(
    save = { stack -> stack.map { it.encode() } },
    restore = { saved -> saved.map { decode(it) } }
)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    var stack by rememberSaveable(stateSaver = StackSaver) { mutableStateOf<List<Screen>>(listOf(Screen.Home)) }
    val current = stack.last()

    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    // Auto-delete old archive entries according to settings
    LaunchedEffect(Unit) {
        val days = Prefs(context).retentionDays
        if (days > 0) {
            withContext(Dispatchers.IO) {
                val before = System.currentTimeMillis() - days * 86_400_000L
                val db = Db.get(context)
                db.calls().deleteOlderThan(before)
                db.sms().deleteOlderThan(before)
            }
        }
    }

    BackHandler(enabled = stack.size > 1) { pop() }

    AnimatedContent(
        targetState = current,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = targetState != Screen.Home
            if (forward) {
                (fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it / 8 }) togetherWith
                    (fadeOut(tween(120)))
            } else {
                fadeIn(tween(220)) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 8 })
            }
        },
        label = "screen"
    ) { screen ->
        when (screen) {
            Screen.Home -> HomeScreen(
                onAdd = { push(Screen.Choose) },
                onEdit = { push(Screen.Editor(it.kind, it.id)) },
                onArchive = { push(Screen.Archive) },
                onSettings = { push(Screen.Settings) }
            )
            Screen.Choose -> ChooseTypeScreen(
                onBack = ::pop,
                onPick = { kind -> stack = stack.dropLast(1) + Screen.Editor(kind, 0L) }
            )
            is Screen.Editor -> RuleEditorScreen(screen.kind, screen.ruleId, onClose = ::pop)
            Screen.Archive -> ArchiveScreen(onBack = ::pop)
            Screen.Settings -> SettingsScreen(onBack = ::pop, onPeople = { push(Screen.People) })
            Screen.People -> PeopleScreen(onBack = ::pop)
        }
    }
}
