package com.khalvat.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.khalvat.app.data.AppDatabase
import com.khalvat.app.data.Db
import com.khalvat.app.data.NumberUtil
import com.khalvat.app.data.PrivateContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ContactsScreen() {
    val ctx = LocalContext.current
    val db = remember { Db.get(ctx) }
    val scope = rememberCoroutineScope()
    val contacts by db.contacts().observeAll().collectAsState(initial = emptyList())

    var showAdd by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<PrivateContact?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            scope.launch {
                val added = importContact(ctx, db, uri)
                Toast.makeText(
                    ctx,
                    if (added > 0) "${added.fa()} شماره اضافه شد" else "شماره‌ای اضافه نشد (شاید قبلاً ثبت شده یا مخاطب شماره ندارد)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) pickLauncher.launch(null)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AppCard(Modifier.fillMaxWidth()) {
                Text(
                    "افراد یا فرستنده‌هایی که در قوانین تماس و پیامک انتخاب می‌شوند را اینجا اضافه کنید. " +
                        "برای پیامک می‌توانید نام فرستنده را هم بنویسید (مثل Digikala). " +
                        "مخاطب ستاره‌دار «ویژه» است و در حالت تمرکز هم مزاحم می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            val ok = ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) ==
                                PackageManager.PERMISSION_GRANTED
                            if (ok) pickLauncher.launch(null)
                            else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("از مخاطبین گوشی") }
                    OutlinedButton(onClick = { showAdd = true }, modifier = Modifier.weight(1f)) {
                        Text("افزودن دستی")
                    }
                }
            }
        }

        if (contacts.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.Person,
                    title = "فهرست خالی است",
                    subtitle = "اولین مخاطب یا فرستنده را اضافه کنید."
                )
            }
        }

        items(contacts, key = { it.id }) { c ->
            AppCard(Modifier.fillMaxWidth().animateItem()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
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
                            contentDescription = "مخاطب ویژه",
                            tint = if (c.vip) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        )
                    }
                    IconButton(onClick = { deleting = c }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddContactDialog(
            onAdd = { name, number ->
                scope.launch {
                    db.contacts().insert(
                        PrivateContact(
                            name = name.trim().ifEmpty { number.trim() },
                            number = number.trim(),
                            numberKey = NumberUtil.key(number)
                        )
                    )
                }
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    deleting?.let { c ->
        ConfirmDialog(
            title = "حذف از فهرست",
            text = "«${c.name}» از فهرست حذف شود؟ (قوانینی که از آن استفاده می‌کنند دیگر آن را شامل نمی‌شوند)",
            confirmLabel = "حذف",
            onConfirm = { scope.launch { db.contacts().delete(c) } },
            onDismiss = { deleting = null }
        )
    }
}

@Composable
private fun AddContactDialog(onAdd: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("افزودن دستی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام (اختیاری)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it; error = false },
                    label = { Text("شماره یا نام فرستنده پیامک") },
                    placeholder = { Text("0912… یا Digikala") },
                    singleLine = true,
                    isError = error
                )
                if (error) {
                    Text("شماره یا نام فرستنده را وارد کنید.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (NumberUtil.key(number).isEmpty()) error = true else onAdd(name, number)
            }) { Text("افزودن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

/** همه شماره‌های مخاطب انتخاب‌شده را به فهرست اضافه می‌کند و تعداد موارد جدید را برمی‌گرداند. */
private suspend fun importContact(ctx: Context, db: AppDatabase, uri: Uri): Int =
    withContext(Dispatchers.IO) {
        val resolver = ctx.contentResolver
        var id: String? = null
        var name = ""
        resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                id = c.getString(0)
                name = c.getString(1).orEmpty()
            }
        }
        val contactId = id ?: return@withContext 0

        var added = 0
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0).orEmpty()
                val key = NumberUtil.key(number)
                if (key.isNotEmpty() &&
                    db.contacts().insert(
                        PrivateContact(name = name.ifEmpty { number }, number = number, numberKey = key)
                    ) != -1L
                ) {
                    added++
                }
            }
        }
        added
    }
