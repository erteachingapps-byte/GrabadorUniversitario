package com.edwin.grabador

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File

/**
 * Decide the processing route without uploading audio or assuming an engine exists.
 * No paid services, keys or remote endpoints are configured.
 */
object TranscriptionRouter {
 enum class Route { LOCAL_READY, LOCAL_MODEL_REQUIRED, ONLINE_UNAVAILABLE, WAIT_FOR_WIFI }
 data class Decision(val route: Route, val explanation: String)

 fun decide(context: Context): Decision {
  val prefs=context.getSharedPreferences("transcription_settings",Context.MODE_PRIVATE)
  val mode=prefs.getString("mode","auto") ?: "auto"
  val cm=context.getSystemService(ConnectivityManager::class.java)
  val caps=cm.getNetworkCapabilities(cm.activeNetwork)
  val wifi=caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  val mobile=caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)==true &&
      caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
  val allowMobile=prefs.getBoolean("mobile_online",false)
  val useOnline=when(mode) {
   "wifi" -> wifi
   "auto" -> wifi || (mobile && allowMobile)
   else -> false
  }
  // A Wi-Fi connection alone is not a transcription service. No upload is performed.
  if(useOnline) return Decision(Route.ONLINE_UNAVAILABLE,
   "Hay conexión, pero todavía no existe un servicio gratuito configurado. No se enviará audio.")
  if(mode=="wifi") return Decision(Route.WAIT_FOR_WIFI,
   "Modo solo Wi-Fi: espera una conexión Wi-Fi con internet.")
  val model=WhisperModelManager.model(context)
  return if(WhisperModelManager.installed(context))
   Decision(Route.LOCAL_MODEL_REQUIRED,
    "Modelo encontrado, pero el motor nativo Whisper aún no está instalado. Procesamiento pendiente.")
  else Decision(Route.LOCAL_MODEL_REQUIRED,
   "Para transcribir sin internet falta instalar Whisper y su modelo gratuito. Audio conservado.")
 }
}
