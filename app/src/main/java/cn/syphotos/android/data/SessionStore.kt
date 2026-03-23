package cn.syphotos.android.data

import android.content.Context
import cn.syphotos.android.model.AuthSession

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("syphotos_session", Context.MODE_PRIVATE)

    fun read(): AuthSession {
        return AuthSession(
            accessToken = prefs.getString("access_token", "").orEmpty(),
            refreshToken = prefs.getString("refresh_token", "").orEmpty(),
            accessTokenExpiresAt = prefs.getString("access_token_expires_at", "").orEmpty(),
            refreshTokenExpiresAt = prefs.getString("refresh_token_expires_at", "").orEmpty(),
            username = prefs.getString("username", "").orEmpty(),
            email = prefs.getString("email", "").orEmpty(),
        )
    }

    fun write(session: AuthSession) {
        prefs.edit()
            .putString("access_token", session.accessToken)
            .putString("refresh_token", session.refreshToken)
            .putString("access_token_expires_at", session.accessTokenExpiresAt)
            .putString("refresh_token_expires_at", session.refreshTokenExpiresAt)
            .putString("username", session.username)
            .putString("email", session.email)
            .apply()
    }

    fun readCookieHeader(): String = prefs.getString("cookie_header", "").orEmpty()

    fun mergeCookieHeader(header: String) {
        if (header.isBlank()) return
        val cookies = linkedMapOf<String, String>()
        readCookieHeader()
            .split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { item ->
                val (name, value) = item.substringBefore("=") to item.substringAfter("=")
                cookies[name] = value
            }
        header
            .split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .forEach { item ->
                val (name, value) = item.substringBefore("=") to item.substringAfter("=")
                cookies[name] = value
            }
        prefs.edit().putString(
            "cookie_header",
            cookies.entries.joinToString("; ") { (name, value) -> "$name=$value" },
        ).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
