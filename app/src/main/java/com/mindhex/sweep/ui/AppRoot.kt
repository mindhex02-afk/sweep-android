package com.mindhex.sweep.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mindhex.sweep.data.Category
import com.mindhex.sweep.data.ScanViewModel

/**
 * Tiny state-based navigator. Three screens, so a full nav library would be
 * overkill.
 */
@Composable
fun AppRoot(
    vm: ScanViewModel,
    isPro: Boolean,
    onScan: () -> Unit,
    onDelete: (List<Uri>) -> Unit,
    onUpgrade: () -> Unit
) {
    val state by vm.state.collectAsState()
    var route by remember { mutableStateOf("home") }
    var activeCategory by remember { mutableStateOf<Category?>(null) }

    when (route) {
        "category" -> CategoryScreen(
            result = state.result?.categories?.firstOrNull { it.category == activeCategory },
            isPro = isPro,
            onBack = { route = "home" },
            onDelete = onDelete,
            onUpgrade = { route = "paywall" }
        )

        "paywall" -> PaywallScreen(
            isPro = isPro,
            onClose = { route = "home" },
            onUpgrade = onUpgrade
        )

        else -> HomeScreen(
            state = state,
            isPro = isPro,
            onScan = onScan,
            onOpenCategory = { category ->
                activeCategory = category
                route = "category"
            },
            onUpgrade = { route = "paywall" }
        )
    }
}
