package com.khalvat.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.ArchivedCall
import com.khalvat.app.data.ArchivedSms
import com.khalvat.app.data.Db
import kotlinx.coroutines.launch

@Composable
fun ArchiveScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    val calls by db.calls().observeAll().collectAsState(initial = emptyList())
    val sms by db.sms().observeAll().collectAsState(initial = emptyList())

    var tab by remember { mutableIntStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenBar(stringResource(R.string.archive_title), onBack)
        TabRow(selectedTabIndex = tab, containerColor = MaterialTheme.colorScheme.background) {
            Tab(selected = tab == 0, onClick = { tab = 0 },
                text = { Text(stringResource(R.string.tab_calls, calls.size.num())) })
            Tab(selected = tab == 1, onClick = { tab = 1 },
                text = { Text(stringResource(R.string.tab_messages, sms.size.num())) })
        }
        if (tab == 0) {
            CallsList(calls, { scope.launch { db.calls().delete(it) } }, { confirmClear = true })
        } else {
            SmsList(sms, { scope.launch { db.sms().delete(it) } }, { confirmClear = true })
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.clear_all),
            text = stringResource(if (tab == 0) R.string.clear_calls_q else R.string.clear_sms_q),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { scope.launch { if (tab == 0) db.calls().deleteAll() else db.sms().deleteAll() } },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun ClearRow(onClear: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onClear) {
            Text(stringResource(R.string.clear_all), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun CallsList(calls: List<ArchivedCall>, onDelete: (ArchivedCall) -> Unit, onClear: () -> Unit) {
    if (calls.isEmpty()) {
        EmptyState(
            Icons.Default.Call,
            stringResource(R.string.archive_empty_calls),
            stringResource(R.string.archive_empty_calls_d),
            Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ClearRow(onClear) }
        items(calls, key = { it.id }) { call ->
            AppCard(Modifier.fillMaxWidth().animateItem()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Call, Brand.call, size = 40, iconSize = 20)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            call.name.ifEmpty { call.number.ifEmpty { stringResource(R.string.hidden_number) } },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (call.name.isNotEmpty()) {
                            Text(call.number, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            Fmt.dateTime(call.timestamp) + if (call.ruleName.isNotEmpty()) " • ${call.ruleName}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(call) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmsList(sms: List<ArchivedSms>, onDelete: (ArchivedSms) -> Unit, onClear: () -> Unit) {
    if (sms.isEmpty()) {
        EmptyState(
            Icons.Default.Email,
            stringResource(R.string.archive_empty_sms),
            stringResource(R.string.archive_empty_sms_d),
            Modifier.fillMaxSize()
        )
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ClearRow(onClear) }
        items(sms, key = { it.id }) { message ->
            var expanded by remember { mutableStateOf(false) }
            AppCard(
                Modifier.fillMaxWidth().animateItem().animateContentSize().clickable { expanded = !expanded }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Default.Email, Brand.sms, size = 40, iconSize = 20)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            message.name.ifEmpty { message.sender },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            Fmt.dateTime(message.timestamp) + if (message.ruleName.isNotEmpty()) " • ${message.ruleName}" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDelete(message) }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
