package com.khalvat.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.Db
import com.khalvat.app.data.Prefs
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onEdit: (Rule) -> Unit,
    onArchive: () -> Unit,
    onSettings: () -> Unit
) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val prefs = remember { Prefs(ctx) }
    val scope = rememberCoroutineScope()

    val rules by db.rules().observeAll().collectAsState(initial = emptyList())
    val contacts by db.contacts().observeAll().collectAsState(initial = emptyList())
    val archivedCalls by db.calls().observeCount().collectAsState(initial = 0)
    val archivedSms by db.sms().observeCount().collectAsState(initial = 0)

    var callsOn by remember { mutableStateOf(prefs.callsEnabled) }
    var smsOn by remember { mutableStateOf(prefs.smsEnabled) }
    var focusUntil by remember { mutableLongStateOf(prefs.focusUntil) }
    val focusActive = focusUntil > System.currentTimeMillis()

    val activeRules = rules.count { r ->
        r.enabled && if (r.kind == RuleKind.CALL) callsOn else smsOn
    }
    val hidden = archivedCalls + archivedSms
    val paused = !callsOn && !smsOn

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text(stringResource(R.string.add), fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KhalvatLogo(Modifier.size(38.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.tagline),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onArchive) {
                        BadgedBox(badge = { if (hidden > 0) Badge { Text(hidden.num()) } }) {
                            Icon(Icons.Default.Lock, stringResource(R.string.archive))
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, stringResource(R.string.settings))
                    }
                }
            }

            // Status hero
            item {
                GradientCard(Modifier.fillMaxWidth(), brush = if (paused) Brand.heroPaused else Brand.hero) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(10.dp).clip(CircleShape)
                                .background(if (paused) Color(0xFFCBD5E1) else Color(0xFF4ADE80))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(if (paused) R.string.home_status_paused else R.string.home_status_on),
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.home_stats, activeRules.num(), hidden.num()),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MasterToggle(
                            Icons.Default.Call, stringResource(R.string.toggle_calls), callsOn,
                            Modifier.weight(1f)
                        ) { callsOn = it; prefs.callsEnabled = it }
                        MasterToggle(
                            Icons.Default.Email, stringResource(R.string.toggle_messages), smsOn,
                            Modifier.weight(1f)
                        ) { smsOn = it; prefs.smsEnabled = it }
                    }
                }
            }

            // Focus mode
            item {
                AppCard(Modifier.fillMaxWidth().animateContentSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Default.Notifications, Brand.focus)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.focus_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (focusActive) stringResource(R.string.focus_active_until, Fmt.dateTime(focusUntil))
                                else stringResource(R.string.focus_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (focusActive) {
                        FilledTonalButton(onClick = { prefs.focusUntil = 0L; focusUntil = 0L }) {
                            Text(stringResource(R.string.focus_stop))
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FocusChip(stringResource(R.string.focus_1h)) { focusUntil = startFocus(prefs, hours = 1) }
                            FocusChip(stringResource(R.string.focus_3h)) { focusUntil = startFocus(prefs, hours = 3) }
                            FocusChip(stringResource(R.string.focus_8h)) { focusUntil = startFocus(prefs, hours = 8) }
                            FocusChip(stringResource(R.string.focus_morning)) { focusUntil = startFocus(prefs, untilMorning = true) }
                        }
                    }
                }
            }

            // Rules
            item {
                Text(
                    stringResource(R.string.rules_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                )
            }

            if (rules.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.Lock,
                        stringResource(R.string.rules_empty_title),
                        stringResource(R.string.rules_empty_desc)
                    )
                }
            }

            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    target = RuleText.target(ctx, rule, contacts),
                    schedule = RuleText.schedule(ctx, rule),
                    behavior = RuleText.behavior(ctx, rule),
                    onClick = { onEdit(rule) },
                    onToggle = { on -> scope.launch { db.rules().upsert(rule.copy(enabled = on)) } }
                )
            }
        }
    }
}

@Composable
private fun MasterToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    modifier: Modifier,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier.clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable { onChecked(!checked) }
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            label, color = Color.White, style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun FocusChip(label: String, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick) { Text(label) }
}

@Composable
private fun RuleCard(
    rule: Rule,
    target: String,
    schedule: String,
    behavior: String,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val title = rule.name.ifBlank {
        ctx.getString(if (rule.kind == RuleKind.CALL) R.string.default_call_rule else R.string.default_sms_rule)
    }
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.Top) {
            IconBadge(
                if (rule.kind == RuleKind.CALL) Icons.Default.Call else Icons.Default.Email,
                Brand.kindBrush(rule.kind)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(target, style = MaterialTheme.typography.bodyMedium)
                Text(
                    schedule,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    behavior,
                    style = MaterialTheme.typography.labelMedium,
                    color = Brand.kindColor(rule.kind),
                    fontWeight = FontWeight.Medium
                )
            }
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun startFocus(prefs: Prefs, hours: Int = 0, untilMorning: Boolean = false): Long {
    val until = if (untilMorning) {
        Calendar.getInstance().apply {
            if (get(Calendar.HOUR_OF_DAY) >= 7) add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 7)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } else {
        System.currentTimeMillis() + hours * 3_600_000L
    }
    prefs.focusUntil = until
    return until
}
