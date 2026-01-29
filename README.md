# Nap Vibration Timer

A simple Android app built with Jetpack Compose that vibrates after a specified time period.

## Features

- Select hours and minutes using NumberPickers
- Start/Stop timer with a single button
- Vibration pattern when timer completes
- Stop vibration with the same control button
- Built with Jetpack Compose for modern UI

## Requirements

- Android 8.0 (API level 26) or higher
- Vibration permission

## Tech Stack

- Kotlin
- Jetpack Compose
- Material Design 3
- AndroidView for NumberPicker integration

## How to Use

1. Select the desired hours using the left NumberPicker
2. Select the desired minutes using the right NumberPicker
3. Press START to begin the countdown
4. Press STOP to cancel the timer or stop the vibration

## Permissions

- `android.permission.VIBRATE` - Required for vibration functionality
- `android.permission.POST_NOTIFICATIONS` - For potential future notification features

## Build

Open the project in Android Studio and build using Gradle:

```bash
./gradlew build
```

## License

MIT License
