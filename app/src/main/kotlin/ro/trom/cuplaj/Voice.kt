package ro.trom.cuplaj

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Voce sintetică locală (fără rețea): rostește textul pe fluxul de convorbire. */
object Voice {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, String>? = null

    fun speak(ctx: Context, text: String, lang: String) {
        pending = Pair(text, lang)
        if (tts == null) {
            tts = TextToSpeech(ctx.applicationContext) { st ->
                ready = st == TextToSpeech.SUCCESS
                if (ready) flush()
            }
        } else if (ready) {
            flush()
        }
    }

    private fun flush() {
        val p = pending ?: return
        pending = null
        val t = tts ?: return
        try {
            t.language = Locale.forLanguageTag(p.second)
            val b = Bundle()
            b.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
            t.speak(p.first, TextToSpeech.QUEUE_FLUSH, b, "cuplaj")
        } catch (_: Exception) {
        }
    }

    fun stop() {
        pending = null
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }
}
