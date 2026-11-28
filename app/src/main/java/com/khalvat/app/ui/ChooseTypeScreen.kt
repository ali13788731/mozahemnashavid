package com.khalvat.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.khalvat.app.R
import com.khalvat.app.data.RuleKind

/** Step 1 of adding a rule: calls or messages. */
@Composable
fun ChooseTypeScreen(onBack: () -> Unit, onPick: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        ScreenBar(stringResource(R.string.choose_title), onBack)
        Column(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TypeCard(
                Icons.Default.Call, Brand.call,
                R.string.choose_calls, R.string.choose_calls_desc
            ) { onPick(RuleKind.CALL) }
            TypeCard(
                Icons.Default.Email, Brand.sms,
                R.string.choose_messages, R.string.choose_messages_desc
            ) { onPick(RuleKind.SMS) }
        }
    }
}

@Composable
private fun TypeCard(icon: ImageVector, brush: Brush, title: Int, desc: Int, onClick: () -> Unit) {
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon, brush, size = 64, iconSize = 30)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
