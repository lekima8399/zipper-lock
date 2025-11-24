---
inclusion: always
---

# Technology Stack

## Build System

- Gradle with Kotlin DSL (`.gradle.kts`)
- Android Gradle Plugin: 8.11.2
- Gradle wrapper included for consistent builds

## Language & Runtime

- Kotlin 2.0.21
- Java 11 compatibility (source/target)
- JVM target: 11

## Android Configuration

- Compile SDK: 36
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- Namespace: `com.wellycorp.demolottie`

## Key Dependencies

- AndroidX Core KTX: 1.17.0
- AndroidX AppCompat: 1.7.1
- Material Components: 1.13.0
- ConstraintLayout: 2.2.1
- Lottie: 6.7.1 (Airbnb animation library)

## Testing Libraries

- JUnit: 4.13.2
- AndroidX JUnit: 1.3.0
- Espresso Core: 3.7.0

## Common Commands

Build the project:
```
gradlew build
```

Clean build:
```
gradlew clean build
```

Run tests:
```
gradlew test
```

Run instrumented tests:
```
gradlew connectedAndroidTest
```

Install debug APK:
```
gradlew installDebug
```

Generate release APK:
```
gradlew assembleRelease
```

## Dependency Management

Dependencies are managed using version catalogs in `gradle/libs.versions.toml`. Use alias references in build files:
```kotlin
implementation(libs.androidx.core.ktx)
```
