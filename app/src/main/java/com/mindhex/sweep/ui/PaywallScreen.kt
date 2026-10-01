package com.mindhex.sweep.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PaywallScreen(
    isPro: Boolean,
    onClose: () -> Unit,
    onUpgrade: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Sweep Pro",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        Text("• No ads anywhere", style = MaterialTheme.typography.bodyLarge)
        Text("• Smart auto-select of junk photos", style = MaterialTheme.typography.bodyLarge)
        Text("• Unlimited bulk delete", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(32.dp))

        if (isPro) {
            Text("You're Pro. Thank you!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
        } else {
            // TODO: set the same price you configured in Play Console.
            Button(onClick = onUpgrade, modifier = Modifier.fillMaxWidth()) {
                Text("Subscribe — ₹99 / month")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Not now")
            }
        }
    }
}
