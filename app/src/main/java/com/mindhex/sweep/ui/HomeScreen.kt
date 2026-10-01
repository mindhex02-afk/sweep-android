package com.mindhex.sweep.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mindhex.sweep.ads.AdsManager
import com.mindhex.sweep.data.Category
import com.mindhex.sweep.data.CategoryResult
import com.mindhex.sweep.data.ScanUiState

@Composable
fun HomeScreen(
    state: ScanUiState,
    isPro: Boolean,
    onScan: () -> Unit,
    onOpenCategory: (Category) -> Unit,
    onUpgrade: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                "Sweep",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Find junk photos and free up space.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))

            when {
                state.scanning -> {
                    Text(state.progress, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                state.result == null -> {
                    Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Scan my phone")
                    }
                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }

                else -> {
                    val categories = state.result.categories.filter { it.items.isNotEmpty() }
                    val reclaimable = state.result.categories.sumOf { it.reclaimableBytes }

                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("You can free up", style = MaterialTheme.typography.labelLarge)
                            Text(
                                formatBytes(reclaimable),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    if (categories.isEmpty()) {
                        Text("Nothing to clean — your phone is tidy.")
                    } else {
                        categories.forEach { category ->
                            CategoryRow(category) { onOpenCategory(category.category) }
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                        Text("Rescan")
                    }
                }
            }

            if (!isPro) {
                Spacer(Modifier.height(20.dp))
                Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Go Pro — remove ads")
                }
            }
        }

        if (!isPro) {
            BannerAd()
        }
    }
}

@Composable
private fun CategoryRow(category: CategoryResult, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.category.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${category.itemCount} items • ${formatBytes(category.reclaimableBytes)} reclaimable",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun BannerAd() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdsManager.BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
