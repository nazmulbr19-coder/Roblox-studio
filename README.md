# Mobile Studio — Kotlin/Compose scaffold

A clean-room Kotlin implementation scaffold based on `SPEC.md` (static analysis of
`com.tetrazbloxy.mobilestudio` v2.5).

This first pass intentionally covers **data models, local persistence, core logic,
and unit tests only**. UI rendering, the OpenGL scene renderer, Lua runtime, cloud
clients, and RBXL/RBXM body decoding are left for later passes.

## Implemented

| Layer | Contents |
| --- | --- |
| `model` | `Instance` tree, `DataModel`, `ClassDb`, `InstanceFactory`, `PartData`, `Vector2/3`, `Vector3int16`, `Quat`, `CFrame`, `Color3`, `BrickColors`, `UDim`, `UDim2`, `Rect`, `NumberRange`, `NumberSequence`, `ColorSequence`, `EnumValue`/`Enums`, value wrappers |
| `storage` | `KeyValueStore` abstraction, `InMemoryKeyValueStore`, `SharedPrefsKeyValueStore` |
| `core` | `Settings` (exact keys/defaults from SPEC 7.1), `Theme` + 4 built-ins, `Recents` (40-entry cap), `DockLayoutState` (`dock_v1` schema), `UndoManager`, `Snap`, `PluginManager` (detect/install/state), `OutputLog` |
| `engine` | `PlaytestConstants`, `CharacterPhysics`, `CameraMath`, `RayHit` |
| `format` | `RbxFile` container detection, gzip unwrap, `isPlace`, material migration, `RbxDocument` |
| `net` | `WebPermissions` state machine, `AssetIds` parsing/price conversion, `CloudPlace` |
| `ui` | Compose placeholder screen and dark theme (structure only) |

## Persistence files

The original app has no SQLite/Room/DataStore. These match SPEC 4.1:

- `studio_settings` — `Settings`
- `recents` — `Recents`
- `layout` — `DockLayoutState` (`dock_v1`)
- `plugins_state` — `PluginManager` (`off:<file>`)
- `web_permissions` — `WebPermissions`
- `roblox_auth` — not implemented yet (cloud layer)

## Building

**Do not build on the phone.** The project is configured for GitHub Actions
(`.github/workflows/android.yml`):

- JDK 17
- Gradle 8.13
- AGP 8.10.1 / Kotlin 2.1.21
- compileSdk 36, minSdk 24, targetSdk 36
- Compose (Material 3)

The workflow runs `:app:testDebugUnitTest` and `:app:assembleDebug`, then uploads
the debug APK artifact.

## Tests

JVM unit tests live in `app/src/test/java/...` and use JUnit 4 plus a real
`org.json` implementation (Android only stubs `org.json` in local unit tests).

```bash
gradle :app:testDebugUnitTest
```

## Known scope limits

- `RbxFile.read` validates the container and reports `BINARY`/`XML`; full property
  and instance decoding is not implemented.
- `PluginManager` handles detection/installation/enabled state; Lua script
  execution is not implemented.
- No HTTP transport, Roblox authentication, renderer, scene graph, or editor UI.
