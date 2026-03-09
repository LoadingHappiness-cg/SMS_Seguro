# Play Store Release Signing Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Configure secure local-only Android release signing so `bundleRelease` generates a signed AAB without committing secrets.

**Architecture:** Move release signing to Gradle properties named `RELEASE_*`, keep debug builds untouched, and fail clearly only when a release task is requested without local credentials. Document the local setup in the repository while keeping the actual secrets outside git.

**Tech Stack:** Gradle, Android application plugin, Kotlin/Java 17, README documentation

---

### Task 1: Lock the current failure mode

**Files:**
- Modify: `app/build.gradle`
- Test: release task execution in shell

**Step 1: Verify the red state**

Run: `./gradlew bundleRelease`
Expected: FAIL with the current missing-signing message and old `SMS_SEGURO_*` property names.

**Step 2: Write minimal implementation**

- Switch the signing config to `RELEASE_*`
- Keep debug builds unchanged
- Make release tasks fail clearly if credentials are missing

**Step 3: Verify the green state**

Run: `./gradlew assembleDebug`
Expected: PASS

### Task 2: Document the local machine setup

**Files:**
- Modify: `README.md`

**Step 1: Update release build instructions**

- Add `~/.gradle/gradle.properties` guidance
- Add the exact `bundleRelease` output path

**Step 2: Verify**

Run: `sed -n '1,260p' README.md`
Expected: release instructions are present and copy/pasteable.
