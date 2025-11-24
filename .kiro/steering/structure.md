---
inclusion: always
---

# Project Structure

## Root Level

- `build.gradle.kts` - Root build configuration
- `settings.gradle.kts` - Project settings and module inclusion
- `gradle/` - Gradle wrapper and version catalog
- `app/` - Main application module

## Application Module (`app/`)

### Source Sets

```
app/src/
├── main/
│   ├── java/com/wellycorp/demolottie/
│   │   ├── MainActivity.kt - Entry point, handles overlay permissions
│   │   ├── ZipperOverlayService.kt - Foreground service managing overlay
│   │   ├── ClippingLottieView.kt - Custom Lottie view with reveal progress
│   │   └── ZipperMaskView.kt - Custom view for zipper masking
│   ├── res/
│   │   ├── layout/ - XML layouts
│   │   ├── values/ - Strings, colors, themes
│   │   ├── drawable/ - Vector drawables
│   │   ├── mipmap-*/ - App icons
│   │   └── xml/ - Backup and data extraction rules
│   ├── assets/ - Lottie JSON animation files
│   │   ├── row.json
│   │   ├── wallpaper.json
│   │   └── zipper.json
│   └── AndroidManifest.xml
├── androidTest/ - Instrumented tests
└── test/ - Unit tests
```

## Architecture Patterns

### Service-Based Overlay

The app uses a foreground service (`ZipperOverlayService`) to maintain a persistent system overlay. This service:
- Manages WindowManager for overlay display
- Handles touch events and gesture recognition
- Coordinates three synchronized Lottie animations
- Implements auto-lock and auto-complete behaviors

### Custom Views

- `ClippingLottieView` extends `LottieAnimationView` to add reveal progress functionality
- Custom views handle specific rendering requirements for the zipper effect

### Permission Handling

MainActivity checks and requests SYSTEM_ALERT_WINDOW permission before starting the overlay service.

## Naming Conventions

- Kotlin files: PascalCase (e.g., `MainActivity.kt`)
- Layout files: snake_case (e.g., `activity_main.xml`)
- Resource IDs: camelCase (e.g., `startOverlayButton`)
- Constants: UPPER_SNAKE_CASE (e.g., `REQUEST_OVERLAY_PERMISSION`)
- Package: lowercase (e.g., `com.wellycorp.demolottie`)

## Key Files

- `AndroidManifest.xml` - Declares permissions and components
- `app/build.gradle.kts` - Module-level dependencies and configuration
- `gradle/libs.versions.toml` - Centralized version management
- Lottie assets in `app/src/main/assets/` - Animation definitions
