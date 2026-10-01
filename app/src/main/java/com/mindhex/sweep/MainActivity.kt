package com.mindhex.sweep

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.mindhex.sweep.ads.AdsManager
import com.mindhex.sweep.billing.BillingManager
import com.mindhex.sweep.data.ScanViewModel
import com.mindhex.sweep.ui.AppRoot
import com.mindhex.sweep.ui.theme.SweepTheme

class MainActivity : ComponentActivity() {

    private val vm: ScanViewModel by viewModels()
    private lateinit var billing: BillingManager
    private var pendingDeleteUris: List<Uri> = emptyList()

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            vm.removeItems(pendingDeleteUris)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            vm.scan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        billing = BillingManager(applicationContext).also { it.start() }
        AdsManager.loadInterstitial(this)

        setContent {
            SweepTheme {
                val isPro by billing.isPro.collectAsState()
                AppRoot(
                    vm = vm,
                    isPro = isPro,
                    onScan = { requestMediaPermissions() },
                    onDelete = { uris -> requestDelete(uris) },
                    onUpgrade = { billing.launchPurchase(this) }
                )
            }
        }
    }

    private fun requestMediaPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
        permissionLauncher.launch(perms)
    }

    private fun requestDelete(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingDeleteUris = uris
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ shows a system confirmation dialog for the delete.
            val pending = MediaStore.createDeleteRequest(contentResolver, uris)
            deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        } else {
            // Legacy path: delete directly (WRITE_EXTERNAL_STORAGE granted).
            uris.forEach { runCatching { contentResolver.delete(it, null, null) } }
            vm.removeItems(uris)
        }
    }
}
