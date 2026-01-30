package com.example.identifymydigit

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.exp

/**
 * Repository for running TFLite inference on drawn digits
 * Uses Coroutines with Dispatchers.Default for background processing
 */
class IdentifyDigitRepository (private val context: Context) {

    private var interpreter: Interpreter? = null
    private var inputImageWidth = 28 // MNIST standard size
    private var inputImageHeight = 28 // MNIST standard size
    private val numClasses = 10 // 0-9 digits

    // Initialize the TFLite model
    suspend fun initialize(modelFileName: String = "mnist.tflite") {
        withContext(Dispatchers.IO) {
            try {
                val modelFile = FileUtil.loadMappedFile(context, modelFileName)
                val options = Interpreter.Options().apply {
                    setNumThreads(4) // Use 4 threads for inference
                    // For GPU acceleration (optional):
                    // addDelegate(GpuDelegate())
                }
                interpreter = Interpreter(modelFile, options)
                val inputShape = interpreter?.getInputTensor(0)?.shape()
                inputShape?.let {
                    inputImageWidth = inputShape[1]
                    inputImageHeight = inputShape[2]
                }
                Log.d("IdentifyDigitRepository", "TFLite model loaded successfully")
            } catch (e: Exception) {
                e.printStackTrace()
                throw Exception("Failed to load TFLite model: ${e.message}")
            }
        }
    }

    /**
     * Run inference on the bitmap using Coroutines
     * This is the recommended approach for modern Android
     */
    suspend fun classifyDigit(bitmap: Bitmap): ClassificationResult {
        return withContext(Dispatchers.Default) {
            try {
                // Preprocess the image
                val preprocessedImage = preprocessImage(bitmap)

                // Run inference
                val outputBuffer = runInference(preprocessedImage)

                // Postprocess the results
                postprocessResults(outputBuffer)
            } catch (e: Exception) {
                e.printStackTrace()
                ClassificationResult(
                    predictedDigit = -1,
                    confidence = 0f,
                    allProbabilities = FloatArray(numClasses),
                    error = e.message
                )
            }
        }
    }

    /**
     * Preprocess the bitmap to match model input requirements
     */
    private fun preprocessImage(bitmap: Bitmap): ByteBuffer {
        // Resize bitmap to 28x28 (MNIST size)
        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            inputImageWidth,
            inputImageHeight,
            true
        )

        // Create ByteBuffer for model input
        // Size: 28 * 28 * 1 (grayscale) * 4 bytes (float32)
        val byteBuffer = ByteBuffer.allocateDirect(4 * inputImageWidth * inputImageHeight)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Convert bitmap to grayscale and normalize
        val pixels = IntArray(inputImageWidth * inputImageHeight)
        resizedBitmap.getPixels(
            pixels, 0, inputImageWidth, 0, 0,
            inputImageWidth, inputImageHeight
        )

        for (pixel in pixels) {
            // Extract grayscale value (assuming drawing is on white background)
            val r = (pixel shr 16 and 0xFF)
            val g = (pixel shr 8 and 0xFF)
            val b = (pixel and 0xFF)

            // Convert to grayscale
            val grayscale = (r + g + b) / 3f

            // Normalize to [0, 1] (or [-1, 1] depending on your model)
            val normalizedValue = grayscale / 255f

            byteBuffer.putFloat(normalizedValue)
        }

        return byteBuffer
    }

    /**
     * Run the actual inference
     */
    private fun runInference(inputBuffer: ByteBuffer): FloatArray {
        val outputArray = Array(1) { FloatArray(numClasses) }

        interpreter?.run(inputBuffer, outputArray)
            ?: throw IllegalStateException("Interpreter not initialized")

        return outputArray[0]
    }

    /**
     * Postprocess the model output
     */
    private fun postprocessResults(outputArray: FloatArray): ClassificationResult {
        // Apply softmax to get probabilities
        Log.d("Repo", "output array: ${outputArray.toList()}")
        val probabilities = softmax(outputArray)

        // Find the digit with highest probability
        var maxIndex = 0
        var maxValue = probabilities[0]

        for (i in 1 until probabilities.size) {
            if (probabilities[i] > maxValue) {
                maxValue = probabilities[i]
                maxIndex = i
            }
        }

        return ClassificationResult(
            predictedDigit = maxIndex,
            confidence = outputArray[maxIndex],
            allProbabilities = probabilities,
            error = null
        )
    }

    /**
     * Apply softmax activation
     */
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp((it - maxLogit).toDouble()).toFloat() }
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }

    /**
     * Alternative: Using TensorImage and ImageProcessor
     * This is a more modern approach with TFLite Support Library
     */
    suspend fun classifyDigitWithTensorImage(bitmap: Bitmap): ClassificationResult {
        return withContext(Dispatchers.Default) {
            try {
                // Create TensorImage
                var tensorImage = TensorImage.fromBitmap(bitmap)

                // Create ImageProcessor for preprocessing
                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeWithCropOrPadOp(inputImageHeight, inputImageWidth))
                    .add(ResizeOp(inputImageHeight, inputImageWidth, ResizeOp.ResizeMethod.BILINEAR))
                    .build()

                // Process the image
                tensorImage = imageProcessor.process(tensorImage)

                // Create output buffer
                val outputBuffer = TensorBuffer.createFixedSize(
                    intArrayOf(1, numClasses),
                    org.tensorflow.lite.DataType.FLOAT32
                )

                // Run inference
                interpreter?.run(tensorImage.buffer, outputBuffer.buffer)

                // Get probabilities
                val probabilities = outputBuffer.floatArray
                val softmaxProbs = softmax(probabilities)

                // Find prediction
                val maxIndex = softmaxProbs.indices.maxByOrNull { softmaxProbs[it] } ?: 0

                ClassificationResult(
                    predictedDigit = maxIndex,
                    confidence = softmaxProbs[maxIndex],
                    allProbabilities = softmaxProbs,
                    error = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                ClassificationResult(
                    predictedDigit = -1,
                    confidence = 0f,
                    allProbabilities = FloatArray(numClasses),
                    error = e.message
                )
            }
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}

/**
 * Data class to hold classification results
 */
data class ClassificationResult(
    val predictedDigit: Int,
    val confidence: Float,
    val allProbabilities: FloatArray,
    val error: String? = null
) {
    val isSuccess: Boolean
        get() = error == null && predictedDigit >= 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClassificationResult

        if (predictedDigit != other.predictedDigit) return false
        if (confidence != other.confidence) return false
        if (!allProbabilities.contentEquals(other.allProbabilities)) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = predictedDigit
        result = 31 * result + confidence.hashCode()
        result = 31 * result + allProbabilities.contentHashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}