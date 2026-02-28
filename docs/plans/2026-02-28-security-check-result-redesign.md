# Security Check Result Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current alert-like security result UI with a premium, calm Material 3 status page that maps risk semantics correctly.

**Architecture:** Introduce a small UI state model that maps `RiskLevel` to copy, status tone, and CTA labels. Build a dedicated `SecurityCheckResultScreen` composable around Material 3 surface container roles, then wire `AlertActivity` to use it for URL-based link analysis.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit 4

---

### Task 1: Define the state model with a failing test

**Files:**
- Create: `app/src/test/kotlin/com/smsguard/ui/SecurityCheckResultModelTest.kt`
- Create: `app/src/main/kotlin/com/smsguard/ui/SecurityCheckResultScreen.kt`

**Step 1: Write the failing test**

Write tests that assert:
- `LOW` maps to a calm semantic tone, the "Continuar" CTA, and the low-risk guidance copy.
- `MEDIUM` maps to attention semantics and the "Abrir com cuidado" CTA.
- `HIGH` maps to danger semantics and the "Bloquear" CTA.

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.smsguard.ui.SecurityCheckResultModelTest`
Expected: FAIL because the model does not exist yet.

**Step 3: Write minimal implementation**

Create a small, pure Kotlin model in the screen file for semantic mapping.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.smsguard.ui.SecurityCheckResultModelTest`
Expected: PASS.

### Task 2: Build the Material 3 screen

**Files:**
- Modify: `app/src/main/kotlin/com/smsguard/ui/AlertActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-pt-rPT/strings.xml`
- Create: `app/src/main/kotlin/com/smsguard/ui/SecurityCheckResultScreen.kt`

**Step 1: Implement the Compose layout**

Build:
- `TopAppBar`
- hero card on `surfaceContainer`
- details card on `surfaceContainerLow`
- expandable reasons card
- bottom action area with risk-specific primary CTA and secondary help action

**Step 2: Keep surfaces neutral**

Use `colorScheme.surface` for the screen background and use accents only for icons/chips/buttons.

**Step 3: Add previews**

Create previews for `LOW`, `MEDIUM`, and `HIGH`.

**Step 4: Wire the activity**

Convert the incoming risk string to `RiskLevel` and route URL checks through the new screen.

### Task 3: Verify

**Files:**
- No new files expected

**Step 1: Run focused test**

Run: `./gradlew testDebugUnitTest --tests com.smsguard.ui.SecurityCheckResultModelTest`

**Step 2: Run standard checks**

Run:
- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
- `./gradlew assembleDebug`

**Step 3: Fix regressions immediately**

If any check fails, patch the smallest possible fix and re-run the failing command.
