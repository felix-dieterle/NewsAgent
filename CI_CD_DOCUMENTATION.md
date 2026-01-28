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
4. Build with Gradle
5. Run tests
6. Build Debug APK
7. Upload Debug APK as artifact

**Artifacts:**
- Debug APK is uploaded and available for download in the workflow run

### 2. Android Release (`android-release.yml`)

**Trigger:** 
- Push to the `main` branch (after merge)
- Manual workflow dispatch

**Purpose:** Automatically builds and releases the APK when code is merged to main.

**Steps:**
1. Checkout code
2. Set up JDK 17
3. Grant execute permission for gradlew
4. Build Release APK
5. Extract version information from build.gradle.kts
6. Upload Release APK as artifact
7. Create GitHub Release with version tag
8. Upload APK to the GitHub Release

**Artifacts:**
- Release APK is uploaded to GitHub Releases
- APK filename format: `NewsAgent-v{version_name}-release.apk`
- Release tag format: `v{version_name}-build{run_number}`

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

Currently, the APK is built without signing (unsigned release APK). For production releases, you should:

1. Create a keystore file
2. Add signing configuration to `app/build.gradle.kts`
3. Store keystore credentials as GitHub Secrets
4. Update the workflow to use the signing configuration

### Adding Signing (Optional Enhancement)

1. **Create keystore:**
   ```bash
   keytool -genkey -v -keystore newsagent.keystore -alias newsagent -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Add to `app/build.gradle.kts`:**
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file(System.getenv("KEYSTORE_FILE") ?: "path/to/keystore")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = System.getenv("KEY_ALIAS")
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           getByName("release") {
               signingConfig = signingConfigs.getByName("release")
               // ...
           }
       }
   }
   ```

3. **Add GitHub Secrets:**
   - `KEYSTORE_FILE`: Base64 encoded keystore file
   - `KEYSTORE_PASSWORD`: Keystore password
   - `KEY_ALIAS`: Key alias
   - `KEY_PASSWORD`: Key password

4. **Update workflow** to decode and use the keystore

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
