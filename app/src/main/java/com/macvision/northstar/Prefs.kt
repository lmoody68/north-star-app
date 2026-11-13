package com.macvision.northstar

import android.content.Context

/** All user settings. Panic number defaults to the North Star line (parses SOS coords via backend Phase B). */
object Prefs {
    private const val FILE = "northstar_prefs"
    private const val KEY_NUMBER = "panic_number"
    private const val KEY_NAME = "user_name"
    private const val KEY_VOICE = "voice_enabled"
    const val DEFAULT_NUMBER = "+13148664423"   // North Star panic line

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun panicNumber(ctx: Context): String {
        val v = sp(ctx).getString(KEY_NUMBER, DEFAULT_NUMBER)?.trim()
        return if (v.isNullOrEmpty()) DEFAULT_NUMBER else v
    }
    fun setPanicNumber(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_NUMBER, v.trim()).apply()

    fun userName(ctx: Context): String = sp(ctx).getString(KEY_NAME, "")?.trim() ?: ""
    fun setUserName(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_NAME, v.trim()).apply()

    fun voiceEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_VOICE, false)
    fun setVoiceEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_VOICE, v).apply()
}
