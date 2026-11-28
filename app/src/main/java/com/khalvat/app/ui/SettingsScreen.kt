package com.khalvat.app.ui

import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsDialog { FocusHistory, Retention, Theme, ClearArchive, Wipe }

@Composable
fun SettingsScreen(onBack: () -> Unit, onPeople: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()

    var blockShots by remember { mutableStateOf(prefs.blockScreenshots) }
    var focusCalls by remember { mutableStateOf(prefs.focusCalls) }
    var focusSms by remember { mutableStateOf(prefs.focusSms) }
    var focusHistory by remember { mutableIntStateOf(prefs.focusHistory) }
    var retention by remember { mutableIntStateOf(prefs.retentionDays) }
    var theme by remember { mutableIntStateOf(prefs.themeMode) }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    val focusHistoryOptions = listOf(
        HistoryMode.KEEP to stringResource(R.string.hist_keep_call),
        HistoryMode.ARCHIVE to stringResource(R.string.hist_archive_call),
        HistoryMode.DELETE to stringResource(R.string.hist_delete)
    )
    val retentionOptions = listOf(
        0 to stringResource(R.string.ret_never),
        7 to stringResource(R.string.ret_7),
        30 to stringResource(R.string.ret_30),
        90 to stringResource(R.string.ret_90)
    )
    val themeOptions = listOf(
        0 to stringResource(R.string.theme_system),
        1 to stringResource(R.string.theme_light),
        2 to stringResource(R.string.theme_dark)
    )
    fun label(options: List<Pair<Int, String>>, value: Int) =
        options.firstOrNull { it.first == value }?.second.orEmpty()

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenBar(stringResource(R.string.settings), onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp).animateContentSize()
        ) {
            SettingsGroup(stringResource(R.string.set_people)) {
                ActionRow(title = stringResource(R.string.set_people_manage), onClick = onPeople)
            }

            SettingsGroup(stringResource(R.string.set_focus)) {
                SwitchRow(stringResource(R.string.focus_include_calls), null, focusCalls) {
                    focusCalls = it; prefs.focusCalls = it
                }
                SwitchRow(stringResource(R.string.focus_include_sms), null, focusSms) {
                    focusSms = it; prefs.focusSms = it
                }
                ChoiceRow(stringResource(R.string.focus_history), label(focusHistoryOptions, focusHistory)) {
                    dialog = SettingsDialog.FocusHistory
                }
            }

            SettingsGroup(stringResource(R.string.set_privacy)) {
                SwitchRow(
                    title = stringResource(R.string.block_shots),
                    subtitle = stringResource(R.string.block_shots_hint),
                    checked = blockShots,
                    onChecked = { on ->
                        blockShots = on
                        prefs.blockScreenshots = on
                        ctx.findActivity()?.window?.let { w ->
                            if (on) w.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            else w.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
                )
            }

            SettingsGroup(stringResource(R.string.set_archive)) {
                ChoiceRow(stringResource(R.string.retention), label(retentionOptions, retention)) {
                    dialog = SettingsDialog.Retention
                }
                ActionRow(
                    title = stringResource(R.string.clear_archive),
                    subtitle = stringResource(R.string.clear_archive_hint),
                    danger = true
                ) { dialog = SettingsDialog.ClearArchive }
            }

            SettingsGroup(stringResource(R.string.set_appearance)) {
                ChoiceRow(stringResource(R.string.set_theme), label(themeOptions, theme)) {
                    dialog = SettingsDialog.Theme
                }
            }

            SettingsGroup(stringResource(R.string.set_reliability)) {
                ActionRow(
                    title = stringResource(R.string.battery),
                    subtitle = stringResource(R.string.battery_hint)
                ) { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            }

            SettingsGroup(stringResource(R.string.set_data)) {
                ActionRow(
                    title = stringResource(R.string.wipe),
                    subtitle = stringResource(R.string.wipe_hint),
                    danger = true
                ) { dialog = SettingsDialog.Wipe }
            }

            SettingsGroup(stringResource(R.string.set_about)) {
                ChoiceRow(stringResource(R.string.version), "1.1".let { if (Fmt.isFa) it.faDigits() else it })
                ChoiceRow(stringResource(R.string.designed_by), "Mr.PC")
                ChoiceRow(stringResource(R.string.language_note), "")
            }
        }
    }

    when (dialog) {
        SettingsDialog.FocusHistory -> ChoiceDialog(
            stringResource(R.string.focus_history), focusHistoryOptions, focusHistory,
            onSelect = { focusHistory = it; prefs.focusHistory = it }, onDismiss = { dialog = null }
        )
        SettingsDialog.Retention -> ChoiceDialog(
            stringResource(R.string.retention), retentionOptions, retention,
            onSelect = { retention = it; prefs.retentionDays = it }, onDismiss = { dialog = null }
        )
        SettingsDialog.Theme -> ChoiceDialog(
            stringResource(R.string.set_theme), themeOptions, theme,
            onSelect = { theme = it; prefs.themeMode = it; ThemeState.mode = it }, onDismiss = { dialog = null }
        )
        SettingsDialog.ClearArchive -> ConfirmDialog(
            title = stringResource(R.string.clear_archive),
            text = stringResource(R.string.clear_archive_hint),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    db.calls().deleteAll()
                    db.sms().deleteAll()
                    Toast.makeText(ctx, R.string.archive_cleared, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { dialog = null }
        )
        SettingsDialog.Wipe -> ConfirmDialog(
            title = stringResource(R.string.wipe),
            text = stringResource(R.string.wipe_hint),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { db.clearAllTables() }
                    Toast.makeText(ctx, R.string.wipe_done, Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { dialog = null }
        )
        null -> Unit
    }
}
