# CLAUDE.md — SMS_Seguro

## Project overview

**SMS_Seguro** (package `com.smsguard`) is an Android application that detects smishing (SMS phishing) attacks in real time. It targets Portuguese-speaking users, with particular focus on Portuguese banking/payment patterns (Multibanco) and government services.

- Version: `0.1.1-alpha` (versionCode 1)
- minSdk 28 (Android 9) / targetSdk 34
- Language: Kotlin, UI in Jetpack Compose + Material3
- Java toolchain: 17

---

## Build commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config — see below)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.smsguard.core.RiskEngineTest"
```

Output APK name pattern: `sms-seguro-<versionName>.apk`

### Release signing

Set these as Gradle properties (`~/.gradle/gradle.properties`) or environment variables. The build fails fast if any are missing:

```
SMS_SEGURO_RELEASE_STORE_FILE=<path to .jks>
SMS_SEGURO_RELEASE_STORE_PASSWORD=
SMS_SEGURO_RELEASE_KEY_ALIAS=
SMS_SEGURO_RELEASE_KEY_PASSWORD=
```

---

## Architecture

All source lives in `app/src/main/kotlin/com/smsguard/` split into these packages:

### `core/` — risk engine and shared logic

| File | Responsibility |
|---|---|
| `RiskEngine.kt` | Scores an SMS against the active ruleset; returns `RiskResult` with score, level, reasons |
| `Models.kt` | All `@Serializable` data classes for rulesets (`RuleSet`, `KeywordGroups`, `UrlSignals`, etc.) and `HistoryEvent` / `RiskLevel` |
| `MultibancoDetector.kt` | Regex-based detection of Multibanco payment data (entidade, referência, valor) |
| `TextNormalizer.kt` | Lowercases and strips diacritics for locale-insensitive matching |
| `UrlExtractor.kt` | Extracts URLs from raw SMS text |
| `UnicodeSpoofingDetector.kt` | Detects Cyrillic or mixed Latin/Cyrillic hostnames in URLs |
| `BrandDetector.kt` | Maps keyword groups to a primary brand (e.g. "ctt", "financas") |
| `PermissionHealth.kt` | Reads live permission/channel state; produces `ProtectionStatusReport` |
| `XiaomiSupport.kt` | Detects Xiaomi/MIUI/HyperOS devices via system properties |
| `RiskAssessment.kt` | Thin data carrier used between processor and notifier |
| `AlertType.kt` | `URL` vs `MULTIBANCO` enum |
| `BuildChannel.kt` | `TEST` (debug) vs `PROD` (release) via `BuildConfig.DEBUG` |
| `AppLogger.kt` | Logcat wrapper; no-ops in release |
| `NotificationPermission.kt` | Android 13+ `POST_NOTIFICATIONS` check |

### `notification/` — SMS interception and alert delivery

| File | Responsibility |
|---|---|
| `SmsBroadcastReceiver.kt` | `RECEIVE_SMS` broadcast (priority 999); calls `SmsEventProcessor` |
| `SmsNotificationListener.kt` | `NotificationListenerService`; secondary interception path for devices where broadcast is suppressed |
| `SmsEventProcessor.kt` | Central pipeline: normalize → detect Multibanco → extract URLs → score → deduplicate → persist → notify |
| `AlertNotifier.kt` | Posts the user-visible high-priority alert notification |
| `AlertNotifierChannels.kt` | Notification channel IDs and setup |
| `AlertPipelineDiagnostics.kt` | Lightweight counters for pipeline health (risk events seen / persisted) |
| `ForegroundServiceNotifier.kt` | Builds/manages the persistent foreground service notification |
| `SmsSourceAllowlist.kt` | Allowlist of sender patterns that are never processed |

### `rules/` — ruleset management

| File | Responsibility |
|---|---|
| `RuleLoader.kt` | Loads `ruleset_current.json` from internal storage (falls back to bundled `assets/ruleset_default.json`); atomic writes via `AtomicFile`; keeps one previous version for rollback |

### `startup/` — app lifecycle

| File | Responsibility |
|---|---|
| `SmsProtectionService.kt` | `START_STICKY` foreground service; schedules ruleset updates; requests `NotificationListenerService` rebind |
| `BootReceiver.kt` | Starts protection service on boot, package replace, and Xiaomi quick-boot |
| `ProtectionServiceStarter.kt` | Helper to start/restart the protection service |

### `storage/` — persistence

| File | Responsibility |
|---|---|
| `HistoryStore.kt` | SharedPreferences-backed list of `HistoryEvent` (JSON-serialized) |
| `TrustedDomainsStore.kt` | User-managed set of domains never flagged |

### `update/` — OTA ruleset updates

| File | Responsibility |
|---|---|
| `RuleUpdateWorker.kt` | `CoroutineWorker`; downloads ruleset JSON + `.sig`; verifies Ed25519 signature; rejects downgrades; persists atomically |
| `RuleUpdateScheduler.kt` | Enqueues `RuleUpdateWorker` via WorkManager (periodic + on-demand) |
| `SignatureVerifier.kt` | Ed25519 verification with hardcoded public key (`MCowBQYDK2VwAyEA...`) |

### `ui/` — Jetpack Compose screens

`MainActivity`, `AlertActivity`, `SetupScreen`, `SetupPermissionsScreen`, `SeniorActivationScreen`, `SecurityCheckResultScreen`, `HistoryScreen`, `AboutScreen`, `BrandHeader`, `ProtectionRepairAction`, `ProtectionSettingsHelpers`, `theme/Theme.kt`.

---

## Risk scoring

Three levels: `LOW` (score < 40) · `MEDIUM` (40–69) · `HIGH` (≥ 70)

**Score contributors** (weights from ruleset JSON):
- Keyword group matches: urgency, threat, payment, dataRequest, publicServices, delivery, banking
- URL signals: has URL, URL shortener, punycode domain, suspicious TLD, Cyrillic/non-Latin hostname, mixed Latin+Cyrillic
- Multibanco signals: entity reference present, amount present, known/intermediary/unknown entity status
- Correlation: brand↔entity mismatch, brand↔domain mismatch

**Forced minimums** — certain signals guarantee at least MEDIUM regardless of total score:
- `dataRequest` keyword matched
- Cyrillic URL detected
- Multibanco reference + entity both detected

**Deduplication**: identical event key (level + normalized text + URL) is suppressed for 15 seconds.

---

## Ruleset OTA update

- JSON URL: `BuildConfig.RULESET_JSON_URL` → GitHub raw `rules/ruleset-latest.json`
- Sig URL: `BuildConfig.RULESET_SIG_URL` → GitHub raw `rules/ruleset-latest.sig`
- Signature algorithm: Ed25519; public key hardcoded in `SignatureVerifier.kt`
- Anti-downgrade: new `version` must be strictly greater than stored version
- Rollback: `rules/ruleset_previous.json` kept for one-step rollback via `RuleLoader.rollback()`
- Size cap: 1 MB max for downloaded ruleset

---

## Testing

Unit tests (JUnit4, no Android instrumentation) live in `app/src/test/kotlin/com/smsguard/`.

Key test files:

| Test | What it covers |
|---|---|
| `RiskEngineTest` | Smishing scenarios, Multibanco, brand/entity mismatch, benign messages, case-insensitivity |
| `SmsEventProcessorTest` | Full pipeline: deduplication, shouldNotify logic, persistAndMaybeNotify |
| `PermissionHealthTest` | `ProtectionStatusReport` flag combinations |
| `UnicodeSpoofingDetectorTest` | Cyrillic / mixed hostname detection |
| `XiaomiSupportTest` | Manufacturer/MIUI property detection |
| `SmsSourceAllowlistTest` | Allowlist matching |
| `TrustedDomainsStoreTest` | Domain allow-listing |
| `SecurityCheckResultModelTest`, `ProtectionRepairActionTest`, `SetupScreenDebugTest` | UI model logic |

Tests use `RuleSet(...)` constructed directly with known weights — no file I/O needed.

---

## Permissions declared

```
INTERNET
RECEIVE_BOOT_COMPLETED
RECEIVE_SMS
POST_NOTIFICATIONS
REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
USE_FULL_SCREEN_INTENT
FOREGROUND_SERVICE
FOREGROUND_SERVICE_DATA_SYNC
```

---

## Platform-specific notes

### Xiaomi / MIUI / HyperOS
- Detection via `Build.MANUFACTURER` and system properties `ro.miui.ui.version.*`
- Dedicated `SeniorActivationScreen` and extra setup guidance
- `BootReceiver` also listens for `QUICKBOOT_POWERON` to survive Xiaomi fast-boot

### Android 13+ (API 33)
- `POST_NOTIFICATIONS` runtime permission required before the alert channel is useful
- `USE_FULL_SCREEN_INTENT` needed for full-screen alert on locked screen

---

## Dependencies (notable)

| Library | Purpose |
|---|---|
| Jetpack Compose BOM 2024.02.00 | UI |
| `androidx.work:work-runtime-ktx:2.9.0` | Periodic ruleset updates |
| `com.squareup.okhttp3:okhttp:4.12.0` | Ruleset download |
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2` | Ruleset JSON parsing |
| `androidx.compose.material:material-icons-extended` | Icons |

No analytics, no crash reporting, no third-party SDKs beyond the above.

---

## Key invariants to preserve

1. **No SMS content is transmitted off-device.** Processing is entirely local.
2. **Ruleset updates must be signature-verified** before being applied. Never skip `SignatureVerifier`.
3. **Anti-downgrade** in `RuleLoader`/`RuleUpdateWorker` must not be bypassed.
4. **`SmsProtectionService` is START_STICKY** — keep it that way to survive process death.
5. **Deduplication** prevents notification spam; the 15 s window is intentional.
6. **`allowBackup="false"`** in the manifest — do not change this.
