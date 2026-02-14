package com.newsagent

import android.app.Application
import android.os.Build
import com.newsagent.utils.Logger

/**
 * Custom Application class for NewsAgent
 * Handles application-level initialization and crash logging
 */
class NewsAgentApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logger first
        Logger.init(this)
        Logger.i("Application", "NewsAgent Application starting")
        Logger.i("Application", "Android Version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
        Logger.i("Application", "Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        Logger.i("Application", "App Version: ${getAppVersion()}")
        
        // Setup global exception handler
        setupExceptionHandler()
        
        Logger.i("Application", "Application initialization completed successfully")
    }
    
    /**
     * Setup global uncaught exception handler to log crashes
     */
    private fun setupExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                Logger.e("CrashHandler", "Uncaught exception in thread ${thread.name}", throwable)
                Logger.e("CrashHandler", "App is crashing. This log may help diagnose the issue.")
                
                // Give logger time to write to disk before terminating
                Thread.sleep(500)
            } catch (e: Exception) {
                // If logging fails, we can't do much about it
                android.util.Log.e("NewsAgent", "Failed to log crash", e)
            } finally {
                // Call the default handler to finish the crash handling
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        
        Logger.i("Application", "Global exception handler installed")
    }
    
    /**
     * Get app version information
     */
    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = pInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
            "$versionName ($versionCode)"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
