package com.edwin.grabador

import android.content.Context
import android.net.Network
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object TranscriptionRouter {
    enum class Route { LOCAL_READY, LOCAL_MODEL_REQUIRED, ONLINE_READY, KEY_REQUIRED, WAIT_FOR_NETWORK }
    data class Decision(val route: Route, val explanation: String)
    fun settings(context: Context) = context.getSharedPreferences("transcription_settings", Context.MODE_PRIVATE).also {
        // Earlier versions forced local mode while online mode was unavailable.
        if (!it.getBoolean("online_v1", false)) it.edit().putBoolean("online_v1", true)
            .putString("mode", "wifi").putBoolean("mobile_online", false).apply()
    }
    fun decide(context: Context): Decision {
        val prefs = settings(context)
        if (prefs.getString("mode", "wifi") == "local") return if (WhisperModelManager.installed(context))
            Decision(Route.LOCAL_READY, "Whisper local") else
            Decision(Route.LOCAL_MODEL_REQUIRED, "Descarga el modelo Whisper desde Preferencias para usar el modo local.")
        if (GroqKeyStore.read(context).isBlank()) return Decision(Route.KEY_REQUIRED,
            "Introduce tu clave de Groq en Preferencias de transcripción.")
        val allowed = onlineNetwork(context) != null
        return if (allowed) Decision(Route.ONLINE_READY, "Groq en línea") else Decision(Route.WAIT_FOR_NETWORK,
            if (prefs.getBoolean("mobile_online", false)) "Conecta Wi-Fi o datos móviles con internet y vuelve a pulsar Transcribir."
            else "Conecta Wi-Fi con internet o permite datos móviles en Preferencias. Luego vuelve a pulsar Transcribir.")
    }
    fun onlineNetwork(context: Context): Network? {
        val prefs = settings(context)
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val network = cm.activeNetwork ?: return null
        val caps = cm.getNetworkCapabilities(network) ?: return null
        val allowed = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            (prefs.getBoolean("mobile_online", false) && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)))
        return if (allowed) network else null
    }

}
