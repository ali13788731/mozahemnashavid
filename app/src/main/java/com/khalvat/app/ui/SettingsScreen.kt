package com.khalvat.app.ui

import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SettingsDialog { Relock, FocusHistory, Retention, Theme, ClearArchive, Wipe }

private val relockOptions = listOf(
    0 to "بلافاصله",
    30 to "بعد از ۳۰ ثانیه",
    60 to "بعد از ۱ دقیقه",
    300 to "بعد از ۵ دقیقه",
    900 to "بعد از ۱۵ دقیقه"
)
private val focusHistoryOptions = listOf(
    HistoryMode.KEEP to "در تاریخچه گوشی بماند",
    HistoryMode.ARCHIVE to "حذف از گوشی و ذخیره در آرشیو",
    HistoryMode.DELETE to "کاملاً حذف شود"
)
private val retentionOptions = listOf(
    0 to "هرگز",
    7 to "بعد از ۷ روز",
    30 to "بعد از ۳۰ روز",
    90 to "بعد از ۹۰ روز"
)
private val themeOptions = listOf(0 to "مطابق سیستم", 1 to "روشن", 2 to "تاریک")

private fun label(options: List<Pair<Int, String>>, value: Int) =
    options.firstOrNull { it.first == value }?.second.orEmpty()

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val prefs = remember { Prefs(ctx) }
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()

    var appLock by remember { mutableStateOf(prefs.appLock) }
    var relock by remember { mutableIntStateOf(prefs.relockSeconds) }
    var blockShots by remember { mutableStateOf(prefs.blockScreenshots) }
    var focusCalls by remember { mutableStateOf(prefs.focusCalls) }
    var focusSms by remember { mutableStateOf(prefs.focusSms) }
    var focusHistory by remember { mutableIntStateOf(prefs.focusHistory) }
    var retention by remember { mutableIntStateOf(prefs.retentionDays) }
    var theme by remember { mutableIntStateOf(prefs.themeMode) }
    var dialog by remember { mutableStateOf<SettingsDialog?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .animateContentSize()
    ) {
        SettingsGroup("امنیت") {
            SwitchRow(
                title = "قفل برنامه",
                subtitle = "ورود با اثر انگشت، چهره یا پین/الگوی گوشی",
                checked = appLock,
                onChecked = { appLock = it; prefs.appLock = it }
            )
            AnimatedVisibility(visible = appLock) {
                ChoiceRow("قفل مجدد خودکار", label(relockOptions, relock)) { dialog = SettingsDialog.Relock }
            }
            SwitchRow(
                title = "جلوگیری از اسکرین‌شات",
                subtitle = "محتوای برنامه در عکس صفحه و لیست برنامه‌های اخیر دیده نمی‌شود",
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

        SettingsGroup("حالت تمرکز") {
            SwitchRow(
                title = "شامل تماس‌ها",
                checked = focusCalls,
                onChecked = { focusCalls = it; prefs.focusCalls = it }
            )
            SwitchRow(
                title = "شامل پیامک‌ها",
                checked = focusSms,
                onChecked = { focusSms = it; prefs.focusSms = it }
            )
            ChoiceRow("تاریخچه در حالت تمرکز", label(focusHistoryOptions, focusHistory)) {
                dialog = SettingsDialog.FocusHistory
            }
        }

        SettingsGroup("آرشیو") {
            ChoiceRow("حذف خودکار آرشیو", label(retentionOptions, retention)) { dialog = SettingsDialog.Retention }
            ActionRow(
                title = "پاک کردن کل آرشیو",
                subtitle = "همه تماس‌ها و پیامک‌های آرشیوشده حذف می‌شوند",
                danger = true
            ) { dialog = SettingsDialog.ClearArchive }
        }

        SettingsGroup("ظاهر") {
            ChoiceRow("پوسته", label(themeOptions, theme)) { dialog = SettingsDialog.Theme }
        }

        SettingsGroup("عملکرد پایدار") {
            ActionRow(
                title = "دسترسی اعلان‌ها",
                subtitle = "برای پنهان کردن اعلان پیامک‌ها باید برای «خلوت» روشن باشد"
            ) { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            ActionRow(
                title = "تنظیمات باتری",
                subtitle = "در گوشی‌های شیائومی، سامسونگ و هواوی بهینه‌سازی باتری را برای خلوت خاموش کنید"
            ) { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }

        SettingsGroup("داده‌ها") {
            ActionRow(
                title = "حذف همه اطلاعات برنامه",
                subtitle = "قوانین، فهرست مخاطبین و آرشیو برای همیشه پاک می‌شوند",
                danger = true
            ) { dialog = SettingsDialog.Wipe }
        }

        SettingsGroup("درباره") {
            ChoiceRow("نسخه", "1.0".faDigits()) {}
            ChoiceRow("طراحی شده توسط", "Mr.PC") {}
        }
    }

    when (dialog) {
        SettingsDialog.Relock -> ChoiceDialog("قفل مجدد خودکار", relockOptions, relock,
            onSelect = { relock = it; prefs.relockSeconds = it }, onDismiss = { dialog = null })

        SettingsDialog.FocusHistory -> ChoiceDialog("تاریخچه در حالت تمرکز", focusHistoryOptions, focusHistory,
            onSelect = { focusHistory = it; prefs.focusHistory = it }, onDismiss = { dialog = null })

        SettingsDialog.Retention -> ChoiceDialog("حذف خودکار آرشیو", retentionOptions, retention,
            onSelect = { retention = it; prefs.retentionDays = it }, onDismiss = { dialog = null })

        SettingsDialog.Theme -> ChoiceDialog("پوسته", themeOptions, theme,
            onSelect = { theme = it; prefs.themeMode = it; ThemeState.mode = it }, onDismiss = { dialog = null })

        SettingsDialog.ClearArchive -> ConfirmDialog(
            title = "پاک کردن کل آرشیو",
            text = "همه تماس‌ها و پیامک‌های آرشیوشده حذف می‌شوند. این کار قابل بازگشت نیست.",
            confirmLabel = "پاک کردن",
            onConfirm = {
                scope.launch {
                    db.calls().deleteAll()
                    db.sms().deleteAll()
                    Toast.makeText(ctx, "آرشیو پاک شد", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { dialog = null }
        )

        SettingsDialog.Wipe -> ConfirmDialog(
            title = "حذف همه اطلاعات",
            text = "قوانین، فهرست مخاطبین و آرشیو برای همیشه پاک می‌شوند. این کار قابل بازگشت نیست.",
            confirmLabel = "حذف همه",
            onConfirm = {
                scope.launch {
                    withContext(Dispatchers.IO) { db.clearAllTables() }
                    Toast.makeText(ctx, "همه اطلاعات پاک شد", Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { dialog = null }
        )

        null -> Unit
    }
}
