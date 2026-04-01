# Harden Message Rules Regex Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make OTA `messageRules.regexAny` evaluation fail soft when a pattern is malformed, without aborting notification analysis.

**Architecture:** Keep the existing ruleset schema and runtime path. Only wrap regex compilation/matching in safe handling so an invalid OTA pattern is treated as non-matching and other rules continue to be evaluated.

**Tech Stack:** Kotlin, kotlinx.serialization, JUnit4.

---

### Task 1: Harden regex evaluation

**Files:**
- Modify: `app/src/main/kotlin/com/smsguard/core/RiskEngine.kt`

**Step 1: Write the failing test**

Add unit coverage that exercises malformed `regexAny` entries and proves analysis returns normally.

**Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.smsguard.core.RiskEngineTest`
Expected: fail or error before the regex hardening is in place.

**Step 3: Write minimal implementation**

Wrap `Regex(pattern).containsMatchIn(...)` in safe handling so invalid patterns return `false`.

**Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.smsguard.core.RiskEngineTest`
Expected: pass.

### Task 2: Verify full app build

**Files:**
- None

**Step 1: Run the debug build**

Run: `./gradlew assembleDebug`
Expected: success.

### Task 3: Commit

**Files:**
- Modified files from Task 1

**Step 1: Review diff**

Confirm only the regex hardening and test cases changed.

**Step 2: Commit**

Use a message that names regex hardening and OTA safety.

