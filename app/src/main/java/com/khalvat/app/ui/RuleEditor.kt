package com.khalvat.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.PrivateContact
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import com.khalvat.app.data.TargetMode
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleEditor(
    initial: Rule,
    contacts: List<PrivateContact>,
    onSave: (Rule) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var rule by remember { mutableStateOf(initial) }
    var showError by remember { mutableStateOf(false) }

    val isCall = rule.kind == RuleKind.CALL
    val selectedIds = rule.contactIdSet()
    val hasDates = rule.dateFrom != 0L || rule.dateTo != 0L

    fun validate(): String? = when {
        rule.targetMode != TargetMode.ALL && selectedIds.isEmpty() -> "حداقل یک مخاطب یا فرستنده انتخاب کنید."
        rule.daysMask == 0 -> "حداقل یک روز هفته را انتخاب کنید."
        !rule.allDay && rule.startMinute == rule.endMinute -> "ساعت شروع و پایان نباید یکسان باشد."
        rule.dateFrom != 0L && rule.dateTo != 0L && rule.dateFrom > rule.dateTo -> "تاریخ شروع باید قبل از تاریخ پایان باشد."
        else -> null
    }
    val error = validate()

    fun save() {
        if (error != null) {
            showError = true
            return
        }
        val fallback = if (isCall) "قانون تماس" else "قانون پیامک"
        onSave(rule.copy(name = rule.name.trim().ifEmpty { fallback }))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "بستن") }
                    Text(
                        if (initial.id == 0L) "قانون جدید" else "ویرایش قانون",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Button(onClick = ::save) { Text("ذخیره") }
                    Spacer(Modifier.width(8.dp))
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // نام
                    OutlinedTextField(
                        value = rule.name,
                        onValueChange = { rule = rule.copy(name = it) },
                        label = { Text("نام قانون") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // هدف
                    SectionCard(if (isCall) "کدام تماس‌ها؟" else "کدام پیامک‌ها؟") {
                        OptionRadio(
                            selected = rule.targetMode == TargetMode.ALL,
                            label = if (isCall) "همه تماس‌ها" else "همه پیامک‌ها"
                        ) { rule = rule.copy(targetMode = TargetMode.ALL) }
                        OptionRadio(
                            selected = rule.targetMode == TargetMode.ONLY_LISTED,
                            label = "فقط موارد انتخاب‌شده"
                        ) { rule = rule.copy(targetMode = TargetMode.ONLY_LISTED) }
                        OptionRadio(
                            selected = rule.targetMode == TargetMode.ALL_EXCEPT_LISTED,
                            label = "همه به‌جز موارد انتخاب‌شده",
                            hint = if (isCall) null else "مثلاً همه پیامک‌ها پنهان شوند به‌جز بانک و دیجی‌کالا"
                        ) { rule = rule.copy(targetMode = TargetMode.ALL_EXCEPT_LISTED) }

                        AnimatedVisibility(visible = rule.targetMode != TargetMode.ALL) {
                            Column(Modifier.padding(top = 8.dp)) {
                                if (contacts.isEmpty()) {
                                    Text(
                                        "ابتدا از منو، بخش «مخاطبین و فرستنده‌ها»، افراد یا فرستنده‌ها (مثل بانک یا دیجی‌کالا) را اضافه کنید.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        contacts.forEach { c ->
                                            FilterChip(
                                                selected = c.id in selectedIds,
                                                onClick = {
                                                    val next = if (c.id in selectedIds) selectedIds - c.id else selectedIds + c.id
                                                    rule = rule.copy(contactIds = next.joinToString(","))
                                                },
                                                label = { Text(c.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // زمان‌بندی
                    SectionCard("چه زمانی؟") {
                        Text("روزهای هفته", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SuggestionChip(onClick = { rule = rule.copy(daysMask = 127) }, label = { Text("هر روز") })
                            SuggestionChip(onClick = { rule = rule.copy(daysMask = 31) }, label = { Text("شنبه تا چهارشنبه") })
                            SuggestionChip(onClick = { rule = rule.copy(daysMask = 96) }, label = { Text("پنجشنبه و جمعه") })
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            RuleText.dayNames.forEachIndexed { index, name ->
                                FilterChip(
                                    selected = rule.dayOn(index),
                                    onClick = { rule = rule.copy(daysMask = rule.daysMask xor (1 shl index)) },
                                    label = { Text(name) }
                                )
                            }
                        }

                        SwitchRow(
                            title = "تمام روز",
                            subtitle = "برای انتخاب ساعت مشخص، خاموش کنید",
                            checked = rule.allDay,
                            onChecked = { rule = rule.copy(allDay = it) }
                        )
                        AnimatedVisibility(visible = !rule.allDay) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        pickTime(ctx, rule.startMinute) { rule = rule.copy(startMinute = it) }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("از ${Fmt.minutes(rule.startMinute)}") }
                                OutlinedButton(
                                    onClick = {
                                        pickTime(ctx, rule.endMinute) { rule = rule.copy(endMinute = it) }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) { Text("تا ${Fmt.minutes(rule.endMinute)}") }
                            }
                        }

                        SwitchRow(
                            title = "محدودیت تاریخ",
                            subtitle = "مثلاً فقط امروز یا یک بازه مشخص",
                            checked = hasDates,
                            onChecked = { on ->
                                rule = if (on) {
                                    val now = Calendar.getInstance()
                                    val y = now.get(Calendar.YEAR)
                                    val m = now.get(Calendar.MONTH)
                                    val d = now.get(Calendar.DAY_OF_MONTH)
                                    rule.copy(dateFrom = startOfDay(y, m, d), dateTo = endOfDay(y, m, d))
                                } else {
                                    rule.copy(dateFrom = 0L, dateTo = 0L)
                                }
                            }
                        )
                        AnimatedVisibility(visible = hasDates) {
                            Column {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            pickDate(ctx, rule.dateFrom) { y, m, d ->
                                                rule = rule.copy(dateFrom = startOfDay(y, m, d))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (rule.dateFrom == 0L) "از تاریخ" else "از ${Fmt.date(rule.dateFrom)}") }
                                    OutlinedButton(
                                        onClick = {
                                            pickDate(ctx, rule.dateTo) { y, m, d ->
                                                rule = rule.copy(dateTo = endOfDay(y, m, d))
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (rule.dateTo == 0L) "تا تاریخ" else "تا ${Fmt.date(rule.dateTo)}") }
                                }
                                Spacer(Modifier.height(6.dp))
                                SuggestionChip(
                                    onClick = {
                                        val now = Calendar.getInstance()
                                        val y = now.get(Calendar.YEAR)
                                        val m = now.get(Calendar.MONTH)
                                        val d = now.get(Calendar.DAY_OF_MONTH)
                                        rule = rule.copy(dateFrom = startOfDay(y, m, d), dateTo = endOfDay(y, m, d))
                                    },
                                    label = { Text("فقط امروز") }
                                )
                            }
                        }
                    }

                    // رفتار
                    SectionCard("چه اتفاقی بیفتد؟") {
                        if (isCall) {
                            SwitchRow(
                                title = "رد کردن تماس",
                                subtitle = "طرف مقابل بوق اشغال می‌شنود. برای حالت عادی خاموش بماند.",
                                checked = rule.rejectCall,
                                onChecked = { rule = rule.copy(rejectCall = it) }
                            )
                            AnimatedVisibility(visible = !rule.rejectCall) {
                                Column {
                                    SwitchRow(
                                        title = "قطع صدا و ویبره",
                                        subtitle = "تماس بی‌صدا می‌شود و طرف مقابل همچنان بوق آزاد می‌شنود.",
                                        checked = rule.silence,
                                        onChecked = { rule = rule.copy(silence = it) }
                                    )
                                }
                            }
                            SwitchRow(
                                title = "پنهان کردن اعلان و صفحه تماس",
                                checked = rule.hideNotification,
                                onChecked = { rule = rule.copy(hideNotification = it) }
                            )
                        } else {
                            SwitchRow(
                                title = "پنهان کردن اعلان پیامک",
                                subtitle = "خود پیامک در برنامه پیامک گوشی می‌ماند.",
                                checked = rule.hideNotification,
                                onChecked = { rule = rule.copy(hideNotification = it) }
                            )
                        }
                    }

                    // تاریخچه
                    SectionCard("تاریخچه") {
                        if (isCall) {
                            OptionRadio(
                                selected = rule.historyMode == HistoryMode.KEEP,
                                label = "در تاریخچه گوشی بماند"
                            ) { rule = rule.copy(historyMode = HistoryMode.KEEP) }
                            OptionRadio(
                                selected = rule.historyMode == HistoryMode.ARCHIVE,
                                label = "از تاریخچه گوشی حذف و در آرشیو برنامه ذخیره شود"
                            ) { rule = rule.copy(historyMode = HistoryMode.ARCHIVE) }
                            OptionRadio(
                                selected = rule.historyMode == HistoryMode.DELETE,
                                label = "کاملاً حذف شود",
                                hint = "نه در گوشی ثبت می‌شود و نه در آرشیو"
                            ) { rule = rule.copy(historyMode = HistoryMode.DELETE) }
                        } else {
                            OptionRadio(
                                selected = rule.historyMode != HistoryMode.ARCHIVE,
                                label = "فقط اعلان پنهان شود (بدون کپی)"
                            ) { rule = rule.copy(historyMode = HistoryMode.KEEP) }
                            OptionRadio(
                                selected = rule.historyMode == HistoryMode.ARCHIVE,
                                label = "کپی پیامک در آرشیو برنامه ذخیره شود",
                                hint = "نیاز به دسترسی دریافت پیامک دارد"
                            ) { rule = rule.copy(historyMode = HistoryMode.ARCHIVE) }
                            Text(
                                "حذف پیامک از صندوق پیامک گوشی فقط برای برنامه پیامک پیش‌فرض ممکن است و در این نسخه پشتیبانی نمی‌شود.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }

                    if (showError && error != null) {
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth().animateContentSize()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun OptionRadio(selected: Boolean, label: String, hint: String? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun pickTime(ctx: Context, initialMinute: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, initialMinute / 60, initialMinute % 60, true).show()
}

private fun pickDate(ctx: Context, initialMillis: Long, onPicked: (Int, Int, Int) -> Unit) {
    val c = Calendar.getInstance().apply { if (initialMillis != 0L) timeInMillis = initialMillis }
    DatePickerDialog(
        ctx,
        { _, y, m, d -> onPicked(y, m, d) },
        c.get(Calendar.YEAR),
        c.get(Calendar.MONTH),
        c.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun startOfDay(y: Int, m: Int, d: Int): Long =
    Calendar.getInstance().apply {
        set(y, m, d, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun endOfDay(y: Int, m: Int, d: Int): Long =
    Calendar.getInstance().apply {
        set(y, m, d, 23, 59, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
