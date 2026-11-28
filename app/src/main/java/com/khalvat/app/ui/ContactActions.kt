package com.khalvat.app.ui

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.AppDatabase
import com.khalvat.app.data.NumberUtil
import com.khalvat.app.data.PrivateContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dialog offering the two ways of adding a person/sender. */
@Composable
fun AddPersonDialog(onPickFromPhone: () -> Unit, onAdd: (name: String, number: String) -> Unit, onDismiss: () -> Unit) {
    var manual by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!manual) {
                    FilledTonalButton(
                        onClick = { onDismiss(); onPickFromPhone() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.from_phone_contacts)) }
                    FilledTonalButton(
                        onClick = { manual = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(stringResource(R.string.add_manually)) }
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.manual_name)) },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it; error = false },
                        label = { Text(stringResource(R.string.manual_number)) },
                        placeholder = { Text(stringResource(R.string.manual_hint)) },
                        singleLine = true,
                        isError = error
                    )
                    if (error) {
                        Text(stringResource(R.string.manual_error), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            if (manual) {
                TextButton(onClick = {
                    if (NumberUtil.key(number).isEmpty()) error = true
                    else { onAdd(name, number); onDismiss() }
                }) { Text(stringResource(R.string.add)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** Saves a manually typed person/sender and returns its id (existing id if it was already saved). */
suspend fun saveManual(db: AppDatabase, name: String, number: String): Long =
    withContext(Dispatchers.IO) {
        val key = NumberUtil.key(number)
        val id = db.contacts().insert(
            PrivateContact(
                name = name.trim().ifEmpty { number.trim() },
                number = number.trim(),
                numberKey = key
            )
        )
        if (id != -1L) id else db.contacts().findByKey(key)?.id ?: -1L
    }

/** Saves every number of the picked phone contact; returns the ids (new or already saved). */
suspend fun importContact(ctx: Context, db: AppDatabase, uri: Uri): List<Long> =
    withContext(Dispatchers.IO) {
        val resolver = ctx.contentResolver
        var contactId: String? = null
        var name = ""
        resolver.query(
            uri,
            arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
            null, null, null
        )?.use { c ->
            if (c.moveToFirst()) {
                contactId = c.getString(0)
                name = c.getString(1).orEmpty()
            }
        }
        val id = contactId ?: return@withContext emptyList()

        val ids = mutableListOf<Long>()
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(id),
            null
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0).orEmpty()
                val key = NumberUtil.key(number)
                if (key.isEmpty()) continue
                val rowId = db.contacts().insert(
                    PrivateContact(name = name.ifEmpty { number }, number = number, numberKey = key)
                )
                val finalId = if (rowId != -1L) rowId else db.contacts().findByKey(key)?.id
                if (finalId != null) ids += finalId
            }
        }
        ids
    }
