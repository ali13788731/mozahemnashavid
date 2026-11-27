package com.khalvat.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khalvat.app.data.Db
import com.khalvat.app.data.PrivateContact
import com.khalvat.app.data.Rule
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.launch

@Composable
fun RulesScreen(kind: Int) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    val rules by db.rules().observe(kind).collectAsState(initial = emptyList())
    val contacts by db.contacts().observeAll().collectAsState(initial = emptyList())

    var editing by remember { mutableStateOf<Rule?>(null) }
    var deleting by remember { mutableStateOf<Rule?>(null) }
    val isCall = kind == RuleKind.CALL

    Box(Modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            EmptyState(
                icon = if (isCall) Icons.Default.Call else Icons.Default.Email,
                title = if (isCall) "هنوز قانونی برای تماس‌ها ندارید" else "هنوز قانونی برای پیامک‌ها ندارید",
                subtitle = if (isCall) {
                    "قانون بساز تا تماس افراد خاص در ساعت و روز دلخواه، بی‌صدا و بدون اعلان بشه."
                } else {
                    "قانون بساز تا اعلان پیامک‌های انتخابی (یا همه به‌جز بانک و دیجی‌کالا) نمایش داده نشه."
                },
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    RuleCard(
                        rule = rule,
                        contacts = contacts,
                        modifier = Modifier.animateItem(),
                        onToggle = { on -> scope.launch { db.rules().upsert(rule.copy(enabled = on)) } },
                        onEdit = { editing = rule },
                        onDelete = { deleting = rule }
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { editing = Rule(name = "", kind = kind) },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("قانون جدید") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }

    editing?.let { initial ->
        RuleEditor(
            initial = initial,
            contacts = contacts,
            onSave = { saved ->
                scope.launch { db.rules().upsert(saved) }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }

    deleting?.let { rule ->
        ConfirmDialog(
            title = "حذف قانون",
            text = "قانون «${rule.name}» حذف شود؟",
            confirmLabel = "حذف",
            onConfirm = { scope.launch { db.rules().delete(rule) } },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    contacts: List<PrivateContact>,
    modifier: Modifier,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val contentAlpha by animateFloatAsState(if (rule.enabled) 1f else 0.5f, label = "ruleAlpha")

    AppCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                rule.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(1f)
                    .alpha(contentAlpha)
            )
            Switch(checked = rule.enabled, onCheckedChange = onToggle)
        }
        Column(Modifier.alpha(contentAlpha)) {
            Spacer(Modifier.height(6.dp))
            InfoLine("هدف", RuleText.target(rule, contacts))
            InfoLine("زمان", RuleText.schedule(rule))
            InfoLine("رفتار", RuleText.behavior(rule))
            InfoLine("تاریخچه", RuleText.history(rule))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onDelete) {
                Text("حذف", color = MaterialTheme.colorScheme.error)
            }
            TextButton(onClick = onEdit) { Text("ویرایش") }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
