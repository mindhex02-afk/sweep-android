package com.mindhex.sweep.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mindhex.sweep.data.CategoryResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    result: CategoryResult?,
    isPro: Boolean,
    onBack: () -> Unit,
    onDelete: (List<Uri>) -> Unit,
    onUpgrade: () -> Unit
) {
    if (result == null || result.items.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Nothing here", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) { Text("Back") }
        }
        return
    }

    val selection = remember { mutableStateListOf<Uri>() }
    val items = result.items

    fun autoSelect() {
        selection.clear()
        result.groups.forEach { group ->
            if (group.items.size > 1) {
                val keeper = group.items.maxByOrNull { it.sizeBytes }
                group.items.filter { it.uri != keeper?.uri }.forEach { selection.add(it.uri) }
            }
        }
    }

    val selectedBytes = items.filter { it.uri in selection }.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(result.category.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { if (isPro) autoSelect() else onUpgrade() }) {
                        Text("Auto-select")
                    }
                }
            )
        },
        bottomBar = {
            if (selection.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${selection.size} selected • ${formatBytes(selectedBytes)}")
                        Button(onClick = {
                            onDelete(selection.toList())
                            selection.clear()
                        }) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(100.dp),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(items, key = { it.uri.toString() }) { item ->
                val selected = item.uri in selection
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (selected) selection.remove(item.uri) else selection.add(item.uri)
                        }
                ) {
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (selected) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000))
                        )
                    }
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = if (selected) Color.White else Color(0x88FFFFFF),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }
            }
        }
    }
}
