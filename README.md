# Identify My Digit

## Description

Identify My Digit is a simple Android application that allows users to draw a digit on the screen and have it classified by a pre-trained machine learning model. The app uses a TensorFlow Lite model (MNIST) to recognize the handwritten digit (0-9).

## Features

-   **Drawing Canvas:** A simple and intuitive canvas for users to draw digits.
-   **Real-time Classification:** The app classifies the drawn digit in real-time.
-   **Clear Canvas:** Option to clear the canvas and start over.
-   **Color and Stroke Selection:** Customize the drawing tool's color and stroke width.

## Tech Stack

-   **UI:** Jetpack Compose
-   **Architecture:** MVI 
-   **Dependency Injection:** Hilt
-   **Machine Learning:** TensorFlow Lite

## Getting Started

### Prerequisites

-   Android Studio
-   An Android device or emulator

### Installation

1.  Clone the repository:

    ```bash
    git clone https://github.com/your-username/IdentifyMyDigit.git
    ```
2.  Open the project in Android Studio.
3.  Build and run the application.

## Usage

1.  Launch the app on your device or emulator.
2.  Draw a single digit (0-9) on the canvas.
3.  The app will display the predicted digit and the confidence level of the prediction.
4.  You can clear the canvas to draw a new digit.

## Project Structure

```
/IdentifyMyDigit
|-- app/
|   |-- src/
|   |   |-- main/
|   |   |   |-- java/
|   |   |   |   |-- com/example/identifymydigit/
|   |   |   |       |-- di/                      // Hilt dependency injection modules
|   |   |   |       |-- IdentifyMyDigitApp.kt    // Application class
|   |   |   |       |-- MyDigitViewModel.kt      // ViewModel for the drawing screen
|   |   |   |       |-- IdentifyDigitRepository.kt // Repository for TFLite model
|   |   |   |       |-- DrawingContract.kt       // UI State and Events
|   |   |   |       |-- DrawingPath.kt           // Data class for drawing paths
|   |   |-- assets/
|   |   |   |-- mnist.tflite             // TensorFlow Lite model
|-- build.gradle.kts
|-- ...
```
