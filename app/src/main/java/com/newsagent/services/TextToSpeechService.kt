package com.newsagent.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.newsagent.models.NewsSummary
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

/**
 * Service for converting text summaries to audio
 */
class TextToSpeechService(private val context: Context) {
    
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    
    /**
     * Initialize Text-to-Speech engine
     */
    suspend fun initialize(): Boolean = suspendCancellableCoroutine { continuation ->
        tts = TextToSpeech(context) { status ->
            isInitialized = status == TextToSpeech.SUCCESS
            if (isInitialized) {
                tts?.language = Locale.GERMAN
            }
            continuation.resume(isInitialized)
        }
    }
    
    /**
     * Speak a summary aloud
     */
    fun speakSummary(summary: NewsSummary, onComplete: () -> Unit = {}) {
        if (!isInitialized) {
            onComplete()
            return
        }
        
        val text = buildString {
            append("Zusammenfassung: ")
            append(summary.summary)
            if (summary.keyPoints.isNotEmpty()) {
                append(". Wichtige Punkte: ")
                summary.keyPoints.forEachIndexed { index, point ->
                    append("${index + 1}. $point. ")
                }
            }
        }
        
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            
            override fun onDone(utteranceId: String?) {
                onComplete()
            }
            
            override fun onError(utteranceId: String?) {
                onComplete()
            }
        })
        
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "summary_${summary.articleId}")
    }
    
    /**
     * Stop speaking
     */
    fun stop() {
        tts?.stop()
    }
    
    /**
     * Shutdown Text-to-Speech engine
     */
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
