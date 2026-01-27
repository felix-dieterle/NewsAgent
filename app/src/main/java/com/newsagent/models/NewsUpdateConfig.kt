package com.newsagent.models

/**
 * Configuration for news update intervals
 */
data class NewsUpdateConfig(
    val intervalMinutes: Int = 60,
    val enableNotifications: Boolean = true,
    val enableAutoSummary: Boolean = true,
    val enableCredibilityCheck: Boolean = true,
    val enableAudioSummary: Boolean = false,
    val preferredLanguage: String = "de",
    val maxArticlesPerUpdate: Int = 10
)
