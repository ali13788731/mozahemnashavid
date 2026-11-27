package com.khalvat.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.khalvat.app.data.Db
import com.khalvat.app.data.Prefs
import com.khalvat.app.data.RuleKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Dest(val title: String, val icon: ImageVector) {
    Dashboard("داشبورد", Icons.Default.Home),
    Calls("قوانین تماس", Icons.Default.Call),
    Sms("قوانین پیامک", Icons.Default.Email),
    Contacts("مخاطبین و فرستنده‌ها", Icons.Default.Person),
    Archive("آرشیو", Icons.Default.Lock),
    Settings("تنظیمات", Icons.Default.Settings),
    About("درباره", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    var current by rememberSaveable { mutableStateOf(Dest.Dashboard) }

    // حذف خودکار آرشیوهای قدیمی طبق تنظیمات
    LaunchedEffect(Unit) {
        val days = Prefs(context).retentionDays
        if (days > 0) {
            withContext(Dispatchers.IO) {
                val before = System.currentTimeMillis() - days * 86_400_000L
                val db = Db.get(context)
                db.calls().deleteOlderThan(before)
                db.sms().deleteOlderThan(before)
            }
        }
    }

    BackHandler(enabled = drawerState.isOpen || current != Dest.Dashboard) {
        if (drawerState.isOpen) scope.launch { drawerState.close() } else current = Dest.Dashboard
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader()
                Spacer(Modifier.height(8.dp))
                Dest.entries.forEach { dest ->
                    NavigationDrawerItem(
                        label = { Text(dest.title) },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        selected = current == dest,
                        onClick = {
                            current = dest
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "طراحی شده توسط Mr.PC",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(current.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "منو")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        ) { padding ->
            AnimatedContent(
                targetState = current,
                modifier = Modifier.padding(padding),
                transitionSpec = {
                    (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 10 }) togetherWith
                        fadeOut(tween(140))
                },
                label = "screen"
            ) { dest ->
                when (dest) {
                    Dest.Dashboard -> DashboardScreen(onNavigate = { current = it })
                    Dest.Calls -> RulesScreen(kind = RuleKind.CALL)
                    Dest.Sms -> RulesScreen(kind = RuleKind.SMS)
                    Dest.Contacts -> ContactsScreen()
                    Dest.Archive -> ArchiveScreen()
                    Dest.Settings -> SettingsScreen()
                    Dest.About -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Brand.gradient)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KhalvatLogo(Modifier.size(64.dp))
        Spacer(Modifier.width(14.dp))
        Column {
            Text("خلوت", style = MaterialTheme.typography.titleLarge, color = Color.White)
            Text(
                "محافظ تماس و پیامک",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
