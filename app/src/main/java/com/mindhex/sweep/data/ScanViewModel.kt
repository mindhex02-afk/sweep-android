package com.mindhex.sweep.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScanUiState(
    val scanning: Boolean = false,
    val progress: String = "",
    val result: ScanResult? = null,
    val error: String? = null
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MediaRepository(app)
    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.scanning) return
        _state.value = ScanUiState(scanning = true, progress = "Starting…")
        viewModelScope.launch {
            try {
                val result = repo.scan { p -> _state.value = _state.value.copy(progress = p) }
                _state.value = ScanUiState(scanning = false, result = result)
            } catch (e: Exception) {
                _state.value = ScanUiState(scanning = false, error = e.message ?: "Scan failed")
            }
        }
    }

    /** Drop the given items from the current result after a successful delete. */
    fun removeItems(uris: List<Uri>) {
        val current = _state.value.result ?: return
        val gone = uris.toHashSet()
        val newCategories = current.categories.map { cat ->
            cat.copy(
                groups = cat.groups
                    .mapNotNull { g ->
                        val kept = g.items.filterNot { it.uri in gone }
                        if (kept.isEmpty()) null else g.copy(items = kept)
                    }
            )
        }
        _state.value = _state.value.copy(result = ScanResult(newCategories))
    }
}
