package com.khalvat.app.ui

import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.Db
import com.khalvat.app.data.HistoryMode
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import com.khalvat.app.data.TargetMode
import kotlinx.coroutines.launch
import java.util.Calendar

/** Step 2 of adding a rule (or editing one): all settings, tailored to calls or messages. */
@Composable
fun RuleEditorScreen(kind: Int, ruleId: Long, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    var loaded by remember { mutableStateOf<Rule?>(null) }

    LaunchedEffect(ruleId) {
        loaded = if (ruleId == 0L) Rule(name = "", kind = kind)
        else db.rules().getById(ruleId) ?: Rule(name = "", kind = kind)
    }

    loaded?.let { Editor(initial = it, onClose = onClose) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Editor(initial: Rule, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    val contacts by db.contacts().observeAll().collectAsState(initial = emptyList())

    var rule by remember { mutableStateOf(initial) }
    var showError by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var datePicker by remember { mutableStateOf(0) } // 0 none, 1 from, 2 to

    val isCall = rule.kind == RuleKind.CALL
    val selected = rule.contactIdSet()
    val hasDates = rule.dateFrom != 0L || rule.dateTo != 0L

    val errorRes: Int? = when {
        rule.targetMode != TargetMode.ALL && selected.isEmpty() -> R.string.err_no_contacts
        rule.daysMask == 0 -> R.string.err_no_days
        !rule.allDay && rule.startMinute == rule.endMinute -> R.string.err_same_time
        rule.dateFrom != 0L && rule.dateTo != 0L && rule.dateFrom > rule.dateTo -> R.string.err_dates
        else -> null
    }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) scope.launch {
            val ids = importContact(ctx, db, uri)
            if (ids.isEmpty()) {
                Toast.makeText(ctx, R.string.contacts_none_added, Toast.LENGTH_SHORT).show()
            } else {
                rule = rule.copy(contactIds = (rule.contactIdSet() + ids).joinToString(","))
            }
        }
    }

    fun save() {
        if (errorRes != null) {
            showError = true
            return
        }
        scope.launch {
            db.rules().upsert(rule.copy(name = rule.name.trim()))
            onClose()
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
        ScreenBar(
            title = stringResource(
                when {
                    initial.id != 0L -> R.string.editor_edit
                    isCall -> R.string.editor_new_call
                    else -> R.string.editor_new_sms
                }
            ),
            onBack = onClose,
            actions = {
                if (initial.id != 0L) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Kind banner + name
            AppCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(
                        if (isCall) Icons.Default.Call else Icons.Default.Email,
                        Brand.kindBrush(rule.kind)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = rule.name,
                        onValueChange = { rule = rule.copy(name = it) },
                        label = { Text(stringResource(R.string.rule_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // WHO
            Section(stringResource(R.string.section_who)) {
                Option(
                    rule.targetMode == TargetMode.ALL,
                    stringResource(if (isCall) R.string.who_all_calls else R.string.who_all_sms)
                ) { rule = rule.copy(targetMode = TargetMode.ALL) }
                Option(rule.targetMode == TargetMode.ONLY_LISTED, stringResource(R.string.who_only)) {
                    rule = rule.copy(targetMode = TargetMode.ONLY_LISTED)
                }
                Option(
                    rule.targetMode == TargetMode.ALL_EXCEPT_LISTED,
                    stringResource(R.string.who_except),
                    hint = if (isCall) null else stringResource(R.string.who_except_hint_sms)
                ) { rule = rule.copy(targetMode = TargetMode.ALL_EXCEPT_LISTED) }

                AnimatedVisibility(visible = rule.targetMode != TargetMode.ALL) {
                    Column(Modifier.padding(top = 10.dp)) {
                        if (contacts.isEmpty()) {
                            Text(
                                stringResource(R.string.who_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            contacts.forEach { c ->
                                FilterChip(
                                    selected = c.id in selected,
                                    onClick = {
                                        val next = if (c.id in selected) selected - c.id else selected + c.id
                                        rule = rule.copy(contactIds = next.joinToString(","))
                                    },
                                    label = { Text(c.name) }
                                )
                            }
                            AssistChip(
                                onClick = { showAdd = true },
                                label = { Text(stringResource(R.string.who_add)) },
                                leadingIcon = { Icon(Icons.Default.Add, null) }
                            )
                        }
                    }
                }
            }

            // WHEN
            Section(stringResource(R.string.section_when)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SuggestionChip(
                        onClick = { rule = rule.copy(daysMask = 127) },
                        label = { Text(stringResource(R.string.days_every)) }
                    )
                    SuggestionChip(
                        onClick = { rule = rule.copy(daysMask = if (Fmt.isFa) 31 else 124) },
                        label = { Text(stringResource(R.string.preset_workdays)) }
                    )
                    SuggestionChip(
                        onClick = { rule = rule.copy(daysMask = if (Fmt.isFa) 96 else 3) },
                        label = { Text(stringResource(R.string.preset_weekend)) }
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Fmt.dayBitOrder.forEach { bit ->
                        FilterChip(
                            selected = rule.dayOn(bit),
                            onClick = { rule = rule.copy(daysMask = rule.daysMask xor (1 shl bit)) },
                            label = { Text(Fmt.dayName(bit)) }
                        )
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.all_day),
                    checked = rule.allDay,
                    onChecked = { rule = rule.copy(allDay = it) }
                )
                AnimatedVisibility(visible = !rule.allDay) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { pickTime(ctx, rule.startMinute) { rule = rule.copy(startMinute = it) } },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.time_from, Fmt.minutes(rule.startMinute))) }
                        OutlinedButton(
                            onClick = { pickTime(ctx, rule.endMinute) { rule = rule.copy(endMinute = it) } },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.time_to, Fmt.minutes(rule.endMinute))) }
                    }
                }

                SwitchRow(
                    title = stringResource(R.string.limit_dates),
                    subtitle = stringResource(R.string.limit_dates_hint),
                    checked = hasDates,
                    onChecked = { on ->
                        rule = if (on) todayOnly(rule) else rule.copy(dateFrom = 0L, dateTo = 0L)
                    }
                )
                AnimatedVisibility(visible = hasDates) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { datePicker = 1 }, modifier = Modifier.weight(1f)) {
                                Text(
                                    if (rule.dateFrom == 0L) stringResource(R.string.pick_from)
                                    else stringResource(R.string.dates_from, Fmt.date(rule.dateFrom))
                                )
                            }
                            OutlinedButton(onClick = { datePicker = 2 }, modifier = Modifier.weight(1f)) {
                                Text(
                                    if (rule.dateTo == 0L) stringResource(R.string.pick_to)
                                    else stringResource(R.string.dates_until, Fmt.date(rule.dateTo))
                                )
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        SuggestionChip(
                            onClick = { rule = todayOnly(rule) },
                            label = { Text(stringResource(R.string.only_today)) }
                        )
                    }
                }
            }

            // WHAT
            Section(stringResource(R.string.section_what)) {
                if (isCall) {
                    SwitchRow(
                        title = stringResource(R.string.act_reject),
                        subtitle = stringResource(R.string.act_reject_hint),
                        checked = rule.rejectCall,
                        onChecked = { rule = rule.copy(rejectCall = it) }
                    )
                    AnimatedVisibility(visible = !rule.rejectCall) {
                        SwitchRow(
                            title = stringResource(R.string.act_silence),
                            subtitle = stringResource(R.string.act_silence_hint),
                            checked = rule.silence,
                            onChecked = { rule = rule.copy(silence = it) }
                        )
                    }
                    SwitchRow(
                        title = stringResource(R.string.act_hide_call),
                        checked = rule.hideNotification,
                        onChecked = { rule = rule.copy(hideNotification = it) }
                    )
                } else {
                    SwitchRow(
                        title = stringResource(R.string.act_hide_sms),
                        checked = rule.hideNotification,
                        onChecked = { rule = rule.copy(hideNotification = it) }
                    )
                }
            }

            // HISTORY
            Section(stringResource(R.string.section_history)) {
                Option(
                    rule.historyMode == HistoryMode.KEEP,
                    stringResource(if (isCall) R.string.hist_keep_call else R.string.hist_keep_sms)
                ) { rule = rule.copy(historyMode = HistoryMode.KEEP) }
                Option(
                    rule.historyMode == HistoryMode.ARCHIVE,
                    stringResource(if (isCall) R.string.hist_archive_call else R.string.hist_archive_sms)
                ) { rule = rule.copy(historyMode = HistoryMode.ARCHIVE) }
                Option(
                    rule.historyMode == HistoryMode.DELETE,
                    stringResource(R.string.hist_delete),
                    hint = stringResource(R.string.hist_delete_hint)
                ) { rule = rule.copy(historyMode = HistoryMode.DELETE) }
            }

            if (showError && errorRes != null) {
                Text(
                    stringResource(errorRes),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = ::save,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp)
        ) { Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium) }
    }

    if (showAdd) {
        AddPersonDialog(
            onPickFromPhone = { pickLauncher.launch(null) },
            onAdd = { name, number ->
                scope.launch {
                    val id = saveManual(db, name, number)
                    if (id != -1L) rule = rule.copy(contactIds = (rule.contactIdSet() + id).joinToString(","))
                }
            },
            onDismiss = { showAdd = false }
        )
    }

    if (datePicker != 0) {
        CalendarPickerDialog(
            initialMillis = if (datePicker == 1) rule.dateFrom else rule.dateTo,
            onPicked = { millis ->
                rule = if (datePicker == 1) rule.copy(dateFrom = millis)
                else rule.copy(dateTo = millis + 86_399_999L)
                datePicker = 0
            },
            onDismiss = { datePicker = 0 }
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_rule_title),
            text = stringResource(R.string.delete_rule_text),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { scope.launch { db.rules().delete(initial); onClose() } },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth().animateContentSize()) {
        SectionTitle(title)
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun Option(selected: Boolean, label: String, hint: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier.fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
            .border(1.dp, border, shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal)
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun pickTime(ctx: Context, initialMinute: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(ctx, { _, h, m -> onPicked(h * 60 + m) }, initialMinute / 60, initialMinute % 60, true).show()
}

private fun todayOnly(rule: Rule): Rule {
    val c = Calendar.getInstance()
    val start = (c.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return rule.copy(dateFrom = start, dateTo = start + 86_399_999L)
}
