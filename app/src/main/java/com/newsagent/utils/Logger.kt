package com.newsagent.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized logging utility for NewsAgent
 * Logs to both Logcat and a persistent log file for debugging installation and runtime issues
 */
object Logger {
    private const val TAG = "NewsAgent"
    private const val LOG_FILE_NAME = "newsagent_logs.txt"
    private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    
    private var context: Context? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    /**
     * Initialize the logger with application context
     */
    fun init(appContext: Context) {
        context = appContext.applicationContext
        i("Logger", "Logger initialized")
    }
    
    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        Log.d("$TAG/$tag", message)
        writeToFile("DEBUG", tag, message)
    }
    
    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        Log.i("$TAG/$tag", message)
        writeToFile("INFO", tag, message)
    }
    
    /**
     * Log warning message
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w("$TAG/$tag", message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeToFile("WARN", tag, fullMessage)
    }
    
    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG/$tag", message, throwable)
        val fullMessage = if (throwable != null) {
            "$message\n${throwable.stackTraceToString()}"
        } else {
            message
        }
        writeToFile("ERROR", tag, fullMessage)
    }
    
    /**
     * Write log entry to file
     */
    private fun writeToFile(level: String, tag: String, message: String) {
        val ctx = context ?: return
        
        try {
            val logFile = getLogFile(ctx)
            
            // Rotate log file if it's too large
            if (logFile.exists() && logFile.length() > MAX_LOG_SIZE) {
                val backupFile = File(ctx.filesDir, "$LOG_FILE_NAME.old")
                logFile.renameTo(backupFile)
            }
            
            val timestamp = dateFormat.format(Date())
            val logEntry = "$timestamp [$level] $tag: $message\n"
            
            FileOutputStream(logFile, true).use { output ->
                output.write(logEntry.toByteArray())
            }
        } catch (e: Exception) {
            // Silently fail - don't want logging to crash the app
            Log.e(TAG, "Failed to write to log file", e)
        }
    }
    
    /**
     * Get the log file
     */
    fun getLogFile(context: Context): File {
        return File(context.filesDir, LOG_FILE_NAME)
    }
    
    /**
     * Get the old log file (backup)
     */
    fun getOldLogFile(context: Context): File {
        return File(context.filesDir, "$LOG_FILE_NAME.old")
    }
    
    /**
     * Read all logs from the log file
     */
    fun readLogs(context: Context): String {
        val logFile = getLogFile(context)
        val oldLogFile = getOldLogFile(context)
        
        val logs = StringBuilder()
        
        // Read old log file first if it exists
        if (oldLogFile.exists()) {
            try {
                logs.append("=== OLDER LOGS ===\n")
                logs.append(oldLogFile.readText())
                logs.append("\n\n")
            } catch (e: Exception) {
                logs.append("Error reading old log file: ${e.message}\n\n")
            }
        }
        
        // Read current log file
        if (logFile.exists()) {
            try {
                logs.append("=== CURRENT LOGS ===\n")
                logs.append(logFile.readText())
            } catch (e: Exception) {
                logs.append("Error reading log file: ${e.message}")
            }
        } else {
            logs.append("No logs available yet.")
        }
        
        return logs.toString()
    }
    
    /**
     * Clear all logs
     */
    fun clearLogs(context: Context) {
        try {
            getLogFile(context).delete()
            getOldLogFile(context).delete()
            i("Logger", "Logs cleared")
        } catch (e: Exception) {
            e("Logger", "Failed to clear logs", e)
        }
    }
}
