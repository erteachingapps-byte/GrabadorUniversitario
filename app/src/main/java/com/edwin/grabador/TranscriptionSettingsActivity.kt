package com.edwin.grabador

import android.app.Activity
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.*

class TranscriptionSettingsActivity : Activity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState); UiTheme.apply(this)
  val prefs=getSharedPreferences("transcription_settings",MODE_PRIVATE)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,28,28,100) }
  root.addView(TextView(this).apply { text="Preferencias de transcripción"; textSize=24f })
  val modes=listOf("Automático: Wi-Fi en línea; datos o sin conexión, local","Siempre local","En línea solo con Wi-Fi")
  val group=RadioGroup(this)
  val saved=prefs.getString("mode","auto") ?: "auto"
  val keys=listOf("auto","local","wifi")
  modes.forEachIndexed { index,label ->
   group.addView(RadioButton(this).apply { id=100+index; text=label; isChecked=keys[index]==saved })
  }
  root.addView(group)
  val mobile=CheckBox(this).apply { text="Permitir transcripción en línea con datos móviles"; isChecked=prefs.getBoolean("mobile_online",false) }
  val summary=CheckBox(this).apply { text="Generar resumen automáticamente después de transcribir"; isChecked=prefs.getBoolean("auto_summary",true) }
  root.addView(mobile);root.addView(summary)
  val state=TextView(this).apply { textSize=16f; setPadding(0,20,0,20) }
  root.addView(state)
  val connectivity=getSystemService(ConnectivityManager::class.java)
  val network=connectivity.activeNetwork
  val caps=connectivity.getNetworkCapabilities(network)
  val validated=caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
  val wifi=caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true
  val cellular=caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)==true
  state.text=when {
   wifi && validated -> "Conexión actual: Wi-Fi con internet"
   cellular && validated -> "Conexión actual: datos móviles"
   else -> "Conexión actual: sin internet validado"
  }
  root.addView(Button(this).apply { text="GUARDAR PREFERENCIAS";setOnClickListener {
   val chosen=keys.getOrElse(group.checkedRadioButtonId-100){"auto"}
   prefs.edit().putString("mode",chosen).putBoolean("mobile_online",mobile.isChecked).putBoolean("auto_summary",summary.isChecked).apply()
   Toast.makeText(this@TranscriptionSettingsActivity,"Preferencias guardadas",Toast.LENGTH_SHORT).show()
  } })
  val modelState=TextView(this).apply { text=if(WhisperModelManager.installed(this@TranscriptionSettingsActivity)) "Modelo Whisper Base descargado" else "Modelo Whisper Base pendiente (aprox. 142 MB)"; textSize=16f }
  root.addView(modelState)
  val downloadButton=Button(this).apply { text="DESCARGAR MODELO WHISPER BASE POR WI-FI" }
  downloadButton.setOnClickListener {
   downloadButton.isEnabled=false
   modelState.text="Preparando descarga..."
   Thread { WhisperModelManager.download(applicationContext) { message -> runOnUiThread { modelState.text=message } };runOnUiThread { downloadButton.isEnabled=true } }.start()
  }
  root.addView(downloadButton)
  root.addView(TextView(this).apply { text="Configuración preparada. La transcripción local requiere instalar un motor y modelo; el procesamiento en línea requiere configurar un servicio. No se enviarán grabaciones automáticamente hasta completar esas etapas.";textSize=15f })
  setContentView(ScrollView(this).apply { setBackgroundColor(UiTheme.background(UiTheme.dark(this@TranscriptionSettingsActivity))); addView(root) })
 }
}
