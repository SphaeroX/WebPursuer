package com.example.webpursuer.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BrowserViewModel : ViewModel() {
    private val _selectedSelector = MutableStateFlow<String?>(null)
    val selectedSelector: StateFlow<String?> = _selectedSelector.asStateFlow()
    
    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting.asStateFlow()

    private val _currentSelector = MutableStateFlow<String>("")
    val currentSelector: StateFlow<String> = _currentSelector.asStateFlow()

    private val _recordedInteractions = mutableListOf<InteractionData>()
    
    fun toggleSelectionMode() {
        _isSelecting.value = !_isSelecting.value
    }
    
    fun setSelectionMode(enabled: Boolean) {
        _isSelecting.value = enabled
    }
    
    data class InteractionData(val type: String, val selector: String, val value: String)

    fun onElementSelected(selector: String) {
        _selectedSelector.value = selector
    }

    fun updateCurrentSelector(selector: String) {
        _currentSelector.value = selector
    }

    fun clearSelection() {
        _selectedSelector.value = null
        _currentSelector.value = ""
    }
    
    fun recordInteraction(type: String, selector: String, value: String) {
        if (!_isSelecting.value) {
            _recordedInteractions.add(InteractionData(type, selector, value))
        }
    }
    
    fun getRecordedInteractions(): List<InteractionData> {
        return _recordedInteractions.toList()
    }
    
    fun clearInteractions() {
        _recordedInteractions.clear()
    }
}
