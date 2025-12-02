package com.example.webpursuer.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel : ViewModel() {
    private val _selectedSelector = MutableStateFlow<String?>(null)
    val selectedSelector: StateFlow<String?> = _selectedSelector.asStateFlow()

    fun onElementSelected(selector: String) {
        _selectedSelector.value = selector
    }

    fun clearSelection() {
        _selectedSelector.value = null
    }
}
