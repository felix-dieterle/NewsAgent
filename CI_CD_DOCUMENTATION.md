# CI/CD Pipeline Documentation

This document describes the Continuous Integration and Continuous Deployment (CI/CD) pipeline for the NewsAgent Android application.

## Overview

The project uses GitHub Actions for automated building, testing, and releasing of the Android application.

## Workflows

### 1. Android CI (`android-ci.yml`)

**Trigger:** Pull requests and pushes to the `main` branch

**Purpose:** Ensures code quality by building and testing the application on every pull request and push.

**Steps:**
1. Checkout code
2. Set up JDK 17
3. Grant execute permission for gradlew
4. Run tests
5. Build Debug APK (signed with debug keystore)
6. Extract version information from build.gradle.kts
7. Rename Debug APK with version number
8. Upload Debug APK as artifact

**Artifacts:**
- Debug APK is uploaded and available for download in the workflow run
- APK filename format: `NewsAgent-v{version_name}-debug.apk`
- The APK is **signed with a debug keystore** and can be installed on Android devices

**Note:** Debug APKs are signed automatically by Android with the default debug keystore. These builds are suitable for testing but should not be used for production releases.

### 2. Android Release (`android-release.yml`)

**Trigger:** 
- Push to the `main` branch (after merge)
- Manual workflow dispatch

**Purpose:** Automatically builds and releases the APK when code is merged to main.

**Steps:**
1. Checkout code
2. Set up JDK 17
3. Grant execute permission for gradlew
4. Decode keystore from secrets (if configured)
5. Build Release APK (signed with production or debug keystore)
6. Extract version information from build.gradle.kts
7. Upload Release APK as artifact
8. Rename APK with version number
9. Create GitHub Release with version tag
10. Upload APK to the GitHub Release

**Artifacts:**
- Release APK is uploaded to GitHub Releases
- APK filename format: 
  - `NewsAgent-v{version_name}-release.apk` (if production signed)
  - `NewsAgent-v{version_name}-debug-signed.apk` (if using debug keystore)
- Release tag format: `v{version_name}-build{run_number}`

**Signing:**
- If GitHub Secrets are configured (see below), the APK will be signed with your production keystore
- If secrets are not configured, the APK will be signed with the debug keystore (suitable for testing)
- Debug-signed APKs can be installed but should not be distributed publicly

## Release Process

### Automatic Releases

When code is merged to the `main` branch:
1. The CI workflow runs first to build and test
2. Upon successful merge, the Release workflow automatically:
   - Builds a release APK
   - Creates a new GitHub Release
   - Uploads the APK to the release

### Manual Releases

You can manually trigger a release:
1. Go to the "Actions" tab in GitHub
2. Select "Android Release" workflow
3. Click "Run workflow"
4. Select the branch (usually `main`)
5. Click "Run workflow"

## Version Management

The version is managed in `app/build.gradle.kts`:

```kotlin
defaultConfig {
    applicationId = "com.newsagent"
    versionCode = 1
    versionName = "1.0"
    // ...
}
```

- `versionCode`: Integer value that should be incremented with each release
- `versionName`: User-visible version string (e.g., "1.0", "1.1", "2.0")

**To release a new version:**
1. Update `versionCode` and/or `versionName` in `app/build.gradle.kts`
2. Commit and push the changes
3. Create a pull request
4. After merge, the release workflow will automatically create a new release with the updated version

## Signing Configuration

### Overview

Android APKs must be signed to be installed on devices. This project supports two signing configurations:

1. **Debug Signing** (default): Uses Android's default debug keystore - suitable for development and testing
2. **Production Signing** (optional): Uses your own keystore for production releases

### Debug Signing

Debug signing is automatically configured and requires no setup:
- Used automatically for debug builds (`assembleDebug`)
- Used as fallback for release builds if production keys are not configured
- Keystore location: `~/.android/debug.keystore` (created automatically by Android SDK)
- **Safe for development/testing but NOT for production distribution**

### Production Signing

For production releases, you should create and configure your own keystore.

#### 1. Create a Keystore

```bash
keytool -genkey -v -keystore newsagent.keystore -alias newsagent \
        -keyalg RSA -keysize 2048 -validity 10000
```

Follow the prompts to set:
- Keystore password
- Key password
- Your name and organization details

**Important:** Store this keystore file securely! You need it to update your app in the future.

#### 2. Configure GitHub Secrets

Add the following secrets to your GitHub repository (Settings → Secrets → Actions):

1. **KEYSTORE_BASE64**: Your keystore file encoded in base64
   ```bash
   base64 newsagent.keystore | tr -d '\n' > keystore.txt
   # Copy the content of keystore.txt to this secret
   ```

2. **KEYSTORE_PASSWORD**: The keystore password you set during creation

3. **KEY_ALIAS**: The key alias (e.g., "newsagent")

4. **KEY_PASSWORD**: The key password you set during creation

#### 3. How It Works

- When GitHub Secrets are configured, the release workflow will:
  1. Decode the base64 keystore
  2. Use it to sign the release APK
  3. Upload a production-signed APK

- When GitHub Secrets are NOT configured:
  1. The release workflow falls back to debug signing
  2. APK filename will include "-debug-signed" suffix
  3. A note will appear in the release description

### Local Signing

To build and sign locally:

```bash
# Set environment variables
export KEYSTORE_FILE=/path/to/your/newsagent.keystore
export KEYSTORE_PASSWORD=your_keystore_password
export KEY_ALIAS=newsagent
export KEY_PASSWORD=your_key_password

# Build signed release APK
./gradlew assembleRelease
```

### Verifying Signing

To verify the signature of an APK:

```bash
# Check signing information
apksigner verify --print-certs app-release.apk

# Or use jarsigner
jarsigner -verify -verbose -certs app-release.apk
```

## Requirements

- **JDK Version:** 17 (Temurin distribution)
- **Gradle Version:** 8.5 (via Gradle Wrapper)
- **Android Gradle Plugin:** 8.2.0
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 34

## Troubleshooting

### Build Failures

If the CI/CD build fails:

1. **Check the workflow logs:** Go to Actions tab → Select the failed workflow → View logs
2. **Common issues:**
   - Missing dependencies: Ensure all dependencies in `build.gradle.kts` are accessible
   - Gradle version mismatch: Check `gradle/wrapper/gradle-wrapper.properties`
   - Test failures: Run tests locally with `./gradlew test`
   - Build errors: Build locally with `./gradlew build`

### Release Not Created

If a release is not automatically created:

1. Check that you have write permissions to create releases
2. Ensure `GITHUB_TOKEN` has the necessary permissions
3. Check for version conflicts (tag already exists)

### Local Testing

Test the build locally before pushing:

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Run all checks
./gradlew build
```

## Future Enhancements

Potential improvements to the CI/CD pipeline:

- [ ] Add code quality checks (lint, detekt)
- [ ] Add code coverage reporting
- [ ] Implement signed release builds
- [ ] Add automated instrumentation tests
- [ ] Deploy to Google Play Store (alpha/beta tracks)
- [ ] Add changelog generation
- [ ] Implement semantic versioning automation
- [ ] Add build caching for faster builds
- [ ] Run builds on multiple API levels
