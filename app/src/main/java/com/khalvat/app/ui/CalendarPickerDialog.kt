package com.khalvat.app.ui

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.khalvat.app.R

/**
 * Month-grid date picker. Uses the Jalali calendar when the phone language is Persian,
 * otherwise the locale's own calendar. [onPicked] receives the local midnight of the chosen day.
 */
@Composable
fun CalendarPickerDialog(initialMillis: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val start = if (initialMillis == 0L) System.currentTimeMillis() else initialMillis
    var monthStart by remember {
        mutableLongStateOf(
            Fmt.calendar(start).apply { set(Calendar.DAY_OF_MONTH, 1) ; dayStart() }.timeInMillis
        )
    }
    val selected = remember { Fmt.calendar(start).apply { dayStart() }.timeInMillis }

    val cal = Fmt.calendar(monthStart)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val first = cal.firstDayOfWeek
    val offset = (cal.get(Calendar.DAY_OF_WEEK) - first + 7) % 7
    val letters = Fmt.narrowWeekdays()

    fun shift(by: Int) {
        monthStart = Fmt.calendar(monthStart).apply { add(Calendar.MONTH, by); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { shift(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null) }
                    Text(
                        Fmt.monthYear(monthStart),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { shift(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) }
                }
                Row(Modifier.fillMaxWidth()) {
                    for (i in 0 until 7) {
                        val dow = ((first - 1 + i) % 7) + 1
                        Text(
                            letters[dow],
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val cells = offset + daysInMonth
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (c in 0 until 7) {
                            val day = r * 7 + c - offset + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (day in 1..daysInMonth) {
                                    val millis = Fmt.calendar(monthStart).apply {
                                        set(Calendar.DAY_OF_MONTH, day)
                                        dayStart()
                                    }.timeInMillis
                                    val isSel = millis == selected
                                    Box(
                                        Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape)
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primary
                                                else androidx.compose.ui.graphics.Color.Transparent
                                            )
                                            .clickable { onPicked(millis) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            day.num(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

private fun Calendar.dayStart() {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}
