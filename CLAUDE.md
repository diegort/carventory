# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Carventory is a native Android app (Kotlin) for managing a personal inventory of toy cars: list, view details, add/edit/delete, with photos captured via camera or picked from gallery. It has no backend — all data is local (Room database + files stored via FileProvider).

## Build & run

Standard Gradle Android project — single module `app`.

```
./gradlew assembleDebug          # build debug APK
./gradlew installDebug           # build and install on a connected device/emulator
./gradlew test                   # run JVM unit tests (app/src/test)
./gradlew testDebugUnitTest --tests "es.dmontesinos.android.carventory.ExampleUnitTest"  # single test class
./gradlew connectedAndroidTest    # instrumented tests on a device/emulator
./gradlew lint                   # Android lint
```

There is no `gradlew` (unix wrapper) checked in — use `gradlew.bat` on Windows or invoke `gradle` directly if the wrapper script is missing locally.

Release builds require a keystore at `app/keystore/carventory.jks` (referenced in `app/build.gradle.kts` `signingConfigs.release` — placeholder credentials are committed there and must be overridden locally, not with real secrets in the repo).

## Architecture

Single-Activity, Navigation-Component app using classic Fragments + XML view binding (not Jetpack Compose, despite Compose deps being present in `build.gradle.kts` — they're currently unused leftovers except for `coil-compose`/image viewing).

- **`MainActivity`** hosts a single `NavHostFragment` driven by `res/navigation/nav_graph.xml`. It also handles edge-to-edge/status-bar styling.
- **Navigation graph** (`nav_graph.xml`) has 4 destinations: `carListFragment` (start) → `carDetailFragment` → `carFormFragment` (add/edit, `carId` arg with `-1L` default meaning "new car"), and `imageViewerFragment` (full-screen zoomable photo, `imageUri` arg). Safe Args (`androidx.navigation.safeargs.kotlin` plugin) generates the `*Directions`/`*Args` classes used for navigation and argument passing.
- **Data layer** (`data/`): standard Room stack —
  - `Car` — the single `@Entity` (`id`, `name`, `imageUri`).
  - `CarDao` — queries, all suspend/LiveData.
  - `CarDatabase` — singleton Room DB accessor (`getDatabase(context)`), db name `car_database`, version 1, `exportSchema = false` (no migrations exist yet).
  - `CarRepository` — thin wrapper over the DAO.
- **`CarViewModel`** (`viewmodels/`) is an `AndroidViewModel` that builds the DB/repository directly in `init` (no DI framework — no Hilt/Koin). Fragments get it via `by viewModels()`, so each fragment has its own instance backed by the same singleton DB.
- **UI layer** (`ui/`): Fragments use `ViewBinding` (`buildFeatures.viewBinding = true`), not Compose.
  - `CarListFragment` — grid/list toggle (persisted only in-memory), search via `SearchView`/`MenuProvider`, sorts cars case-insensitively by name client-side.
  - `CarFormFragment` — add/edit form; handles camera capture (`CameraX`/`ActivityResultContracts.TakePicture`) with runtime `CAMERA` permission request, EXIF orientation correction (`androidx.exifinterface`), and saving images under the app's external `Pictures` dir via the `FileProvider` declared in `AndroidManifest.xml` (authority `es.dmontesinos.android.carventory.fileprovider`, paths in `res/xml/file_paths.xml`).
  - `CarDetailFragment`, `ImageViewerFragment` — read-only detail/photo views; `ImageViewerFragment` uses `subsampling-scale-image-view` for pinch-zoom.
  - `CarAdapter` — RecyclerView adapter shared between grid and list layouts (`item_car.xml` vs `item_car_list.xml`), switching item layout based on the view mode.
  - `GridSpacingItemDecoration` / `ListSpacingItemDecoration` — item spacing decorators for the two RecyclerView layout modes.
- Image loading uses **Glide** in fragments/adapters and **Coil** only where noted for Compose-style loading; there isn't a single consistent image-loading library across the app.
- Localization: default strings in `res/values/strings.xml`, Spanish overrides in `res/values-es/strings.xml`. Dark theme resources live in `res/values-night/`.

## Key facts to keep in mind when editing

- Namespace/package root: `es.dmontesinos.android.carventory`; `minSdk 30`, `targetSdk`/`compileSdk 35`.
- KSP (not kapt) generates the Room annotation processor code; `kotlin-kapt` plugin is commented out in `app/build.gradle.kts`.
- No dependency injection framework and no repository interfaces/abstractions — everything is concrete classes wired up manually in `CarViewModel.init`. Follow this existing pattern rather than introducing DI unless asked.
- No CI config, no lint suppress baseline, and only one placeholder unit test (`ExampleUnitTest`) exists — there's no established test suite/pattern to follow yet.
