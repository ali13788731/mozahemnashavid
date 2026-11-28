package com.khalvat.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.khalvat.app.data.PrivateContact
import kotlinx.coroutines.launch

/** Manage the saved people/senders list (reached from Settings). A star keeps someone reachable in Focus mode. */
@Composable
fun PeopleScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    val contacts by db.contacts().observeAll().collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PrivateContact?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) scope.launch { importContact(ctx, db, uri) }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenBar(stringResource(R.string.contacts_title), onBack) {
            IconButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, stringResource(R.string.add))
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    stringResource(R.string.contacts_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (contacts.isEmpty()) {
                item {
                    EmptyState(
                        Icons.Default.Person,
                        stringResource(R.string.contacts_empty),
                        stringResource(R.string.who_empty)
                    )
                }
            }
            items(contacts, key = { it.id }) { c ->
                AppCard(Modifier.fillMaxWidth().animateItem()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(42.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                c.name.firstOrNull()?.toString() ?: "?",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(
                                c.number,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { scope.launch { db.contacts().update(c.copy(vip = !c.vip)) } }) {
                            Icon(
                                Icons.Default.Star,
                                stringResource(R.string.vip),
                                tint = if (c.vip) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                        IconButton(onClick = { deleting = c }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPersonDialog(
            onPickFromPhone = { pickLauncher.launch(null) },
            onAdd = { name, number -> scope.launch { saveManual(db, name, number) } },
            onDismiss = { showAdd = false }
        )
    }

    deleting?.let { c ->
        ConfirmDialog(
            title = stringResource(R.string.contacts_delete_title),
            text = stringResource(R.string.contacts_delete_text, c.name),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = { scope.launch { db.contacts().delete(c) } },
            onDismiss = { deleting = null }
        )
    }
}
