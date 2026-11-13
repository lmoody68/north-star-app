package com.macvision.northstar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * v1 BETA voice trigger using Android's on-device SpeechRecognizer. Listens for the wake phrase and fires the
 * SOS. NOTE: SpeechRecognizer is NOT a true always-on low-power wake-word engine — it has gaps and costs
 * battery; the production path is Picovoice Porcupine, a drop-in replacement for this class (see README).
 * Off by default.
 */
class VoiceTrigger(private val ctx: Context, private val onWake: () -> Unit) {
    private var recognizer: SpeechRecognizer? = null
    private var running = false
    private val phrases = listOf("north star", "northstar", "help me", "mayday", "emergency")

    fun start() {
        if (running) return
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
            Log.w("NorthStar", "Speech recognition unavailable on this device")
            return
        }
        running = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(ctx).apply { setRecognitionListener(listener) }
        listen()
    }

    private fun listen() {
        if (!running) return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        try { recognizer?.startListening(intent) } catch (_: Exception) { restart() }
    }

    private fun restart() {
        if (!running) return
        Handler(ctx.mainLooper).postDelayed({ listen() }, 800)
    }

    fun stop() {
        running = false
        try { recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onResults(results: Bundle?) {
            val hits = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            if (hits.any { h -> phrases.any { h.lowercase().contains(it) } }) onWake()
            restart()
        }
        override fun onError(error: Int) = restart()
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
