package com.newsagent.services

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.newsagent.api.GitHubApi
import com.newsagent.api.GitHubRelease
import com.newsagent.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Service for handling app updates from GitHub releases
 * Checks for new versions and downloads APK files
 */
class UpdateService(private val context: Context) {
    
    companion object {
        private const val GITHUB_OWNER = "felix-dieterle"
        private const val GITHUB_REPO = "NewsAgent"
        private const val PREF_KEY_LAST_UPDATE_CHECK = "last_update_check"
        private const val PREF_KEY_SKIP_VERSION = "skip_version"
        private const val UPDATE_CHECK_INTERVAL_MS = 20 * 60 * 1000L // 20 minutes
    }
    
    private val prefs = context.getSharedPreferences("newsagent_prefs", Context.MODE_PRIVATE)
    
    private val githubApi: GitHubApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubApi::class.java)
    }
    
    /**
     * Data class representing update information
     */
    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val currentVersion: String,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String?,
        val releaseUrl: String
    )
    
    /**
     * Check if an update is available
     * @return UpdateInfo containing update availability and details
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            Logger.d("UpdateService", "Checking for updates...")
            
            val response = githubApi.getLatestRelease(GITHUB_OWNER, GITHUB_REPO)
            
            if (!response.isSuccessful) {
                Logger.w("UpdateService", "Failed to fetch latest release: ${response.code()}")
                return@withContext null
            }
            
            val release = response.body() ?: run {
                Logger.w("UpdateService", "Empty response body")
                return@withContext null
            }
            
            // Get current version from package manager
            val currentVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "1.0"
            } catch (e: Exception) {
                Logger.e("UpdateService", "Error getting version info", e)
                "1.0"
            }
            
            val latestVersion = release.tag_name.removePrefix("v")
            
            Logger.i("UpdateService", "Current version: $currentVersion, Latest version: $latestVersion")
            
            // Check if user has skipped this version
            val skippedVersion = prefs.getString(PREF_KEY_SKIP_VERSION, null)
            if (skippedVersion == latestVersion) {
                Logger.d("UpdateService", "User has skipped version $latestVersion")
                return@withContext UpdateInfo(
                    isUpdateAvailable = false,
                    currentVersion = currentVersion,
                    latestVersion = latestVersion,
                    releaseNotes = release.body,
                    downloadUrl = null,
                    releaseUrl = release.html_url
                )
            }
            
            val isNewer = compareVersions(latestVersion, currentVersion) > 0
            
            // Find APK asset
            val apkAsset = release.assets.find { 
                it.name.endsWith(".apk", ignoreCase = true) 
            }
            
            UpdateInfo(
                isUpdateAvailable = isNewer,
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                releaseNotes = release.body,
                downloadUrl = apkAsset?.browser_download_url,
                releaseUrl = release.html_url
            )
        } catch (e: Exception) {
            Logger.e("UpdateService", "Error checking for updates", e)
            null
        }
    }
    
    /**
     * Check if we should perform an update check
     * Based on the last check time and interval
     */
    fun shouldCheckForUpdate(): Boolean {
        val lastCheck = prefs.getLong(PREF_KEY_LAST_UPDATE_CHECK, 0)
        val now = System.currentTimeMillis()
        val timeSinceLastCheck = now - lastCheck
        
        return timeSinceLastCheck >= UPDATE_CHECK_INTERVAL_MS
    }
    
    /**
     * Update the last check timestamp
     */
    fun updateLastCheckTime() {
        prefs.edit()
            .putLong(PREF_KEY_LAST_UPDATE_CHECK, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Mark a version as skipped by the user
     */
    fun skipVersion(version: String) {
        prefs.edit()
            .putString(PREF_KEY_SKIP_VERSION, version)
            .apply()
        Logger.i("UpdateService", "Version $version marked as skipped")
    }
    
    /**
     * Download and install the update
     * @param downloadUrl URL to download the APK from
     * @return Download ID for tracking
     */
    fun downloadAndInstallUpdate(downloadUrl: String): Long {
        Logger.i("UpdateService", "Downloading update from: $downloadUrl")
        
        // Security check: Ensure download is from HTTPS and GitHub
        if (!downloadUrl.startsWith("https://")) {
            throw SecurityException("Update download must use HTTPS")
        }
        
        if (!downloadUrl.contains("github.com") && !downloadUrl.contains("githubusercontent.com")) {
            throw SecurityException("Update download must be from GitHub")
        }
        
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        
        // Use a unique filename with timestamp and random component to prevent replacement attacks
        val uniqueFilename = "NewsAgent-update-${System.currentTimeMillis()}-${(0..9999).random()}.apk"
        
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("NewsAgent Update")
            setDescription("Downloading new version...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                uniqueFilename
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }
        
        return downloadManager.enqueue(request)
    }
    
    /**
     * Install APK file
     * Opens the APK installer for the user
     */
    fun installApk(apkFile: File) {
        Logger.i("UpdateService", "Installing APK: ${apkFile.absolutePath}")
        
        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android 7.0+
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
        }
        
        context.startActivity(intent)
    }
    
    /**
     * Compare two version strings
     * Supports semantic versioning (e.g., 1.2.3)
     * Pre-release versions (with -, +) are considered older than release versions
     * @return negative if v1 < v2, 0 if equal, positive if v1 > v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        // Check for pre-release indicators (-, +)
        val v1HasPreRelease = v1.contains("-") || v1.contains("+")
        val v2HasPreRelease = v2.contains("-") || v2.contains("+")
        
        // Strip any non-numeric suffixes for comparison
        val cleanV1 = v1.split("-", "+")[0]
        val cleanV2 = v2.split("-", "+")[0]
        
        // Parse version parts
        val v1Parts = cleanV1.split(".")
        val v2Parts = cleanV2.split(".")
        
        // Validate that all parts are numeric
        val parts1 = v1Parts.mapNotNull { it.toIntOrNull() }
        val parts2 = v2Parts.mapNotNull { it.toIntOrNull() }
        
        // If any part failed to parse, log warning and use what we have
        if (parts1.size != v1Parts.size || parts2.size != v2Parts.size) {
            Logger.w("UpdateService", "Version contains non-numeric components: v1=$v1, v2=$v2")
        }
        
        // Compare numeric parts
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrNull(i) ?: 0
            val p2 = parts2.getOrNull(i) ?: 0
            
            when {
                p1 < p2 -> return -1
                p1 > p2 -> return 1
            }
        }
        
        // If numeric parts are equal, check pre-release status
        // Pre-release versions are considered older than release versions
        return when {
            v1HasPreRelease && !v2HasPreRelease -> -1  // v1 is pre-release, v2 is release
            !v1HasPreRelease && v2HasPreRelease -> 1   // v1 is release, v2 is pre-release
            else -> 0  // Both are same type or numeric parts differ
        }
    }
}
