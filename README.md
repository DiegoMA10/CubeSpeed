# CubeSpeed

CubeSpeed is an Android app for timing and tracking Rubik's cube solves.

## Features

- Timer for cube solves
- Support for different cube types (2x2, 3x3, 4x4, etc.)
- Scramble generation
- Statistics tracking
- User authentication with Firebase

## Project Structure

- `app/`: Android application code
- `functions/`: Firebase Functions for statistics calculation (JavaScript)

## Firebase Functions

The app uses Firebase Functions written in JavaScript to calculate statistics for cube solves. When a solve is added or updated, a Firebase Function is automatically triggered to calculate statistics like average time, best time, and rolling averages (Ao5, Ao12, Ao50, Ao100). The function also adds an example_text field to each solve document to demonstrate how Firebase Functions work.

For more information about the Firebase Functions, see the [functions/README.md](functions/README.md) file.

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Connect to Firebase
4. Deploy the Firebase Functions (see [functions/README.md](functions/README.md))
5. Build and run the app
