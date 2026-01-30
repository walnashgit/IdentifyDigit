package com.example.identifymydigit

import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import android.graphics.Canvas as AndroidCanvas

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingCanvasBitMap(modifier: Modifier = Modifier, viewModel: MyDigitViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(state.paths) {
        if (state.paths != null) {
            captureBitmap(canvasSize, state.paths, viewModel)  // State is ALREADY updated!
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Drawing Canvas") },
                actions = {
                    IconButton(onClick = {
                        viewModel.onEvent(DrawingContract.Event.OnClearCanvas)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Canvas")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Drawing area
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.6f)
                    .background(Color.Black)
                    .onSizeChanged { size ->
                        canvasSize = size
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                viewModel.onEvent(DrawingContract.Event.OnDragStart(offset))
                            },
                            onDrag = { change, _ ->
                                viewModel.onEvent(DrawingContract.Event.OnDrag(change.position))
                            },
                            onDragEnd = {
                                viewModel.onEvent(DrawingContract.Event.OnDragEnd)
                            }
                        )
                    }
            ) {

                state.paths?.let { drawingPath ->
                    if (drawingPath.points.size > 1) {
                        val path = Path().apply {
                            moveTo(drawingPath.points.first().x, drawingPath.points.first().y)
                            for (i in 1 until drawingPath.points.size) {
                                lineTo(drawingPath.points[i].x, drawingPath.points[i].y)
                            }
                        }
                        drawPath(
                            path = path,
                            color = drawingPath.color,
                            style = Stroke(
                                width = drawingPath.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Draw current path being drawn
                if (state.currentPoints.size > 1) {
                    val path = Path().apply {
                        moveTo(state.currentPoints.first().x, state.currentPoints.first().y)
                        for (i in 1 until state.currentPoints.size) {
                            lineTo(state.currentPoints[i].x, state.currentPoints[i].y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = state.currentColor,
                        style = Stroke(
                            width = state.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            Text(
                modifier = Modifier
                    .weight(0.4f)
                    .padding(top = 32.dp)
                    .align(Alignment.CenterHorizontally),
                text = state.predictedValue
            )

            /*// Controls
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Color picker
                    Text("Color:", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ColorButton(Color.Black, currentColor) { currentColor = Color.Black }
                        ColorButton(Color.Red, currentColor) { currentColor = Color.Red }
                        ColorButton(Color.Blue, currentColor) { currentColor = Color.Blue }
                        ColorButton(Color.Green, currentColor) { currentColor = Color.Green }
                        ColorButton(Color.Yellow, currentColor) { currentColor = Color.Yellow }
                        ColorButton(Color.Magenta, currentColor) { currentColor = Color.Magenta }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stroke width slider
                    Text(
                        "Stroke Width: ${strokeWidth.toInt()}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 5f..50f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }*/
        }
    }
}

fun captureBitmap(canvasSize: IntSize, paths: DrawingPath?/*List<DrawingPath>*/, viewModel: MyDigitViewModel) {
    Log.d("CaptureBitmap", "canvasSize: $canvasSize, paths: $paths")
    if (canvasSize.width > 0 && canvasSize.height > 0) {
        val bitmap = createBitmap(canvasSize.width, canvasSize.height)
        val canvas = AndroidCanvas(bitmap)

        // Fill with white background
        canvas.drawColor(android.graphics.Color.BLACK, PorterDuff.Mode.SRC)

        // Draw all paths
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        paths?.let { drawingPath ->
            if (drawingPath.points.size > 1) {
                paint.color = drawingPath.color.toArgb()
                paint.strokeWidth = drawingPath.strokeWidth

                for (i in 0 until drawingPath.points.size - 1) {
                    canvas.drawLine(
                        drawingPath.points[i].x,
                        drawingPath.points[i].y,
                        drawingPath.points[i + 1].x,
                        drawingPath.points[i + 1].y,
                        paint
                    )
                }
            }
        }
        viewModel.onBitmapCaptured(bitmap)
    }
}

@Composable
fun ColorButton(
    color: Color,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .background(
                color = color,
                shape = MaterialTheme.shapes.medium
            )
            .then(
                if (color == selectedColor) {
                    Modifier.padding(4.dp)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {}
        if (color == selectedColor) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White, shape = MaterialTheme.shapes.small)
            )
        }
    }
}

@Preview
@Composable
internal fun PreviewDrawingCanvas() {
    DrawingCanvasBitMap()
}