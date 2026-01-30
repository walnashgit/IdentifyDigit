package com.example.identifymydigit

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

// MVI Contract
object DrawingContract {

    // State - Represents the UI state
    data class State(
        val paths: DrawingPath? = null, //List<DrawingPath> = emptyList(),
        val currentPoints: List<Offset> = emptyList(),
        val currentColor: Color = Color.White,
        val strokeWidth: Float = 90f,
        val capturedBitmap: Bitmap? = null,
        val isLoading: Boolean = false,
        val predictedValue: String = ""
    )

    // Events - User actions/intentions
    sealed class Event {
        data class OnDragStart(val offset: Offset) : Event()
        data class OnDrag(val offset: Offset) : Event()
        object OnDragEnd : Event()
        data class OnColorSelected(val color: Color) : Event()
        data class OnStrokeWidthChanged(val width: Float) : Event()
        object OnClearCanvas : Event()
    }

    // Effects - One-time side effects (navigation, showing toasts, etc.)
    sealed class Effect {
        data class ShowToast(val message: String) : Effect()
        data class SaveBitmapToFile(val bitmap: Bitmap) : Effect()
        data class ShareBitmap(val bitmap: Bitmap) : Effect()
    }
}

// Data class for drawing paths
data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)