# Auto-Update Feature - Quick Reference

## Implementation Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     NewsAgent Auto-Update                        │
│                   GitHub Releases Integration                    │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│ 1. APP STARTUP                                                   │
│    ┌──────────────┐                                             │
│    │ MainActivity │                                             │
│    │  onCreate()  │                                             │
│    └──────┬───────┘                                             │
│           │                                                      │
│           ▼                                                      │
│    ┌──────────────────────┐                                     │
│    │ checkForAppUpdate()  │  ◄── Checks preference setting     │
│    └──────┬───────────────┘      Checks 20min interval         │
│           │                                                      │
└───────────┼──────────────────────────────────────────────────────┘
            │
            ▼
┌───────────┴──────────────────────────────────────────────────────┐
│ 2. UPDATE CHECK (UpdateService)                                  │
│    ┌─────────────────────┐                                       │
│    │ GitHub API Request  │                                       │
│    │  GET /repos/.../    │                                       │
│    │  releases/latest    │                                       │
│    └─────────┬───────────┘                                       │
│              │                                                    │
│              ▼                                                    │
│    ┌─────────────────────┐                                       │
│    │ Compare Versions    │  ◄── Semantic versioning             │
│    │  Current vs Latest  │      Pre-release handling            │
│    └─────────┬───────────┘                                       │
└──────────────┼────────────────────────────────────────────────────┘
               │
               ├─── No Update ──► (Silent - no notification)
               │
               ├─── Update Available ──►
               │
               ▼
┌──────────────┴────────────────────────────────────────────────────┐
│ 3. UPDATE DIALOG                                                  │
│    ┌─────────────────────────────────────────────────────────┐   │
│    │  Update Available                                       │   │
│    │                                                         │   │
│    │  Current: 1.0.0                                        │   │
│    │  New: 1.1.0                                            │   │
│    │                                                         │   │
│    │  Release Notes:                                        │   │
│    │  - New features...                                     │   │
│    │                                                         │   │
│    │  [Update Now] [Later] [Skip This Version]             │   │
│    └─────────────────────────────────────────────────────────┘   │
│               │           │              │                        │
└───────────────┼───────────┼──────────────┼────────────────────────┘
                │           │              │
         ┌──────┘           │              └────► (Mark as skipped)
         │                  │
         │                  └───────────────────► (Postpone)
         │
         ▼
┌────────┴──────────────────────────────────────────────────────────┐
│ 4. DOWNLOAD (UpdateService)                                       │
│    ┌─────────────────────┐                                        │
│    │ Security Checks     │  ◄── HTTPS only                       │
│    │  - Validate HTTPS   │      GitHub domain only               │
│    │  - Validate Domain  │                                        │
│    └─────────┬───────────┘                                        │
│              │                                                     │
│              ▼                                                     │
│    ┌─────────────────────┐                                        │
│    │ DownloadManager     │  ◄── Unique filename                  │
│    │  Start Download     │      (timestamp + random)             │
│    └─────────┬───────────┘                                        │
└──────────────┼─────────────────────────────────────────────────────┘
               │
               │ (Shows notification with progress)
               │
               ▼
┌──────────────┴─────────────────────────────────────────────────────┐
│ 5. INSTALLATION                                                    │
│    ┌─────────────────────┐                                         │
│    │ BroadcastReceiver   │  ◄── Receives download completion      │
│    │  onReceive()        │                                         │
│    └─────────┬───────────┘                                         │
│              │                                                      │
│              ▼                                                      │
│    ┌─────────────────────┐                                         │
│    │ Get File URI        │  ◄── Uses DownloadManager API          │
│    │  Validate File      │      Checks file exists                │
│    └─────────┬───────────┘                                         │
│              │                                                      │
│              ▼                                                      │
│    ┌─────────────────────┐                                         │
│    │ Open APK Installer  │  ◄── Uses FileProvider                 │
│    │  (System Intent)    │      Grants read permission            │
│    └─────────┬───────────┘                                         │
└──────────────┼─────────────────────────────────────────────────────┘
               │
               ▼
         (User confirms installation)
               │
               ▼
         (App updated!)

═══════════════════════════════════════════════════════════════════

## Component Diagram

┌─────────────────────────────────────────────────────────────────┐
│                         Components                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────┐         ┌──────────────┐                     │
│  │ MainActivity │◄───────►│UpdateService │                     │
│  └──────┬───────┘         └──────┬───────┘                     │
│         │                        │                              │
│         │                        │                              │
│         │                        ▼                              │
│         │              ┌──────────────────┐                     │
│         │              │   GitHubApi      │                     │
│         │              │  (Retrofit)      │                     │
│         │              └──────────────────┘                     │
│         │                        │                              │
│         │                        ▼                              │
│         │              ┌──────────────────┐                     │
│         │              │  GitHub API      │                     │
│         │              │  api.github.com  │                     │
│         │              └──────────────────┘                     │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────────┐                                          │
│  │DownloadManager   │                                          │
│  │(Android System)  │                                          │
│  └──────────────────┘                                          │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────────┐                                          │
│  │BroadcastReceiver │                                          │
│  │(Download Done)   │                                          │
│  └──────────────────┘                                          │
│         │                                                       │
│         ▼                                                       │
│  ┌──────────────────┐                                          │
│  │Package Installer │                                          │
│  │(Android System)  │                                          │
│  └──────────────────┘                                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

## Security Layers

┌─────────────────────────────────────────────────────────────────┐
│                     Security Measures                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Layer 1: URL Validation                                        │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ✓ HTTPS-only enforcement                               │    │
│  │ ✓ GitHub domain check (github.com, githubusercontent)   │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  Layer 2: Download Security                                     │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ✓ Unique filename (timestamp + random)                 │    │
│  │ ✓ External storage with proper permissions             │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  Layer 3: File Access                                           │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ✓ FileProvider (secure sharing)                        │    │
│  │ ✓ Temporary read permission only                       │    │
│  │ ✓ File existence validation                            │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
│  Layer 4: Android System                                        │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ ✓ User must approve installation                       │    │
│  │ ✓ Package verification by Android                      │    │
│  │ ✓ Signature validation                                 │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

## Settings Flow

┌─────────────────────────────────────────────────────────────────┐
│                    Settings Activity                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ □ Automatische Updates aktivieren                      │    │
│  └────────────────────────────────────────────────────────┘    │
│                    │                                             │
│                    ├─ ON  ──► Updates checked on startup        │
│                    │                                             │
│                    └─ OFF ──► No update checks                  │
│                                                                  │
│  Stored in SharedPreferences:                                   │
│  ┌────────────────────────────────────────────────────────┐    │
│  │ Key: "auto_update_enabled"                             │    │
│  │ Default: true                                           │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

## Version Comparison Logic

┌─────────────────────────────────────────────────────────────────┐
│                  Version Comparison Examples                     │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1.0.0  <  1.0.1    ✓ (patch update)                           │
│  1.0.9  <  1.1.0    ✓ (minor update)                           │
│  1.9.0  <  2.0.0    ✓ (major update)                           │
│  1.0.0-beta < 1.0.0 ✓ (pre-release < release)                  │
│  v1.0.0 = 1.0.0     ✓ (v prefix stripped)                      │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════════

## Files Reference

New Files:
  • GitHubApi.kt              (45 lines)   - API interface
  • UpdateService.kt          (267 lines)  - Core logic
  • AUTO_UPDATE_DOCUMENTATION.md          - User guide
  • IMPLEMENTATION_SUMMARY_AUTO_UPDATE.md - Tech docs

Modified Files:
  • MainActivity.kt           (+150 lines) - Integration
  • SettingsActivity.kt       (+20 lines)  - Preferences
  • AndroidManifest.xml       (+1 line)    - Permission
  • file_paths.xml           (+2 lines)    - FileProvider

═══════════════════════════════════════════════════════════════════
