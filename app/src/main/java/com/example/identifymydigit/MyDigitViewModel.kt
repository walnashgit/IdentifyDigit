package com.example.identifymydigit

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyDigitViewModel @Inject constructor(private val digitRepo: IdentifyDigitRepository) : ViewModel() {

    // State
    private val _state = MutableStateFlow(DrawingContract.State())
    val state: StateFlow<DrawingContract.State> = _state.asStateFlow()

    // Effects (one-time events)
    private val _effect = Channel<DrawingContract.Effect>()
    val effect = _effect.receiveAsFlow()

    init {
        // Initialize TFLite model on ViewModel creation
        initializeModel()
    }

    private fun initializeModel() {
        viewModelScope.launch {
            try {
                // Load the TFLite model
                digitRepo.initialize("mnist.tflite")
                Log.d("ViewModel", "TFLite model initialized successfully")
            } catch (e: Exception) {
                Log.e("ViewModel", "Failed to initialize TFLite model: ${e.message}")
            }
        }
    }

    // Handle events from UI
    fun onEvent(event: DrawingContract.Event) {
        when (event) {
            is DrawingContract.Event.OnDragStart -> handleDragStart(event.offset)
            is DrawingContract.Event.OnDrag -> handleDrag(event.offset)
            is DrawingContract.Event.OnDragEnd -> handleDragEnd()
            is DrawingContract.Event.OnColorSelected -> handleColorSelected(event.color)
            is DrawingContract.Event.OnStrokeWidthChanged -> handleStrokeWidthChanged(event.width)
            is DrawingContract.Event.OnClearCanvas -> handleClearCanvas()
        }
    }

    private fun handleDragStart(offset: androidx.compose.ui.geometry.Offset) {
        _state.update { it.copy(currentPoints = listOf(offset)) }
    }

    private fun handleDrag(offset: androidx.compose.ui.geometry.Offset) {
        _state.update {
            it.copy(currentPoints = it.currentPoints + offset)
        }
    }

    private fun handleDragEnd() {
        val currentState = _state.value

        if (currentState.currentPoints.isNotEmpty()) {
            val newPath = DrawingPath(
                points = currentState.currentPoints,
                color = currentState.currentColor,
                strokeWidth = currentState.strokeWidth
            )

            _state.update {
                it.copy(
                    paths = newPath, //it.paths + newPath,
                    currentPoints = emptyList()
                )
            }
        } else {
            _state.update { it.copy(currentPoints = emptyList()) }
        }
    }

    private fun handleColorSelected(color: androidx.compose.ui.graphics.Color) {
        _state.update { it.copy(currentColor = color) }
    }

    private fun handleStrokeWidthChanged(width: Float) {
        _state.update { it.copy(strokeWidth = width) }
    }

    private fun handleClearCanvas() {
        _state.update {
            it.copy(
                paths = null, //emptyList(),
                currentPoints = emptyList(),
                capturedBitmap = null,
                predictedValue = ""
            )
        }
    }

    // Called when bitmap is captured
    fun onBitmapCaptured(bitmap: Bitmap) {
        Log.d("ViewModel", "Bitmap captured: ${bitmap.width}x${bitmap.height}")
        _state.update {
            it.copy(
                capturedBitmap = bitmap
            )
        }

        // You can process the bitmap here
        processBitmap(bitmap)
    }

    private fun processBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val result = digitRepo.classifyDigit(bitmap)
            Log.d("ViewModel", "Classification result: $result")
            _state.update {
                it.copy(
                    predictedValue = "${result.predictedDigit} with ${result.confidence} confidence."
                )
            }
        }
    }

    // Optional: Get the current bitmap
    fun getCurrentBitmap(): Bitmap? = _state.value.capturedBitmap

    // Optional: Save bitmap with effect
    fun saveBitmapToFile() {
        viewModelScope.launch {
            _state.value.capturedBitmap?.let { bitmap ->
                _effect.send(DrawingContract.Effect.SaveBitmapToFile(bitmap))
            }
        }
    }

    // Optional: Share bitmap with effect
    fun shareBitmap() {
        viewModelScope.launch {
            _state.value.capturedBitmap?.let { bitmap ->
                _effect.send(DrawingContract.Effect.ShareBitmap(bitmap))
            }
        }
    }
}