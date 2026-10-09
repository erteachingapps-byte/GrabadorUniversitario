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
  val modes=listOf("Automático (próximamente: servicio en línea)","Siempre local · Whisper (disponible)","En línea solo con Wi-Fi (próximamente)")
  val group=RadioGroup(this)
  val saved="local"
  val keys=listOf("auto","local","wifi")
  modes.forEachIndexed { index,label ->
   group.addView(RadioButton(this).apply { id=100+index; text=label; isChecked=keys[index]==saved })
  }
  group.check(101)
  group.getChildAt(0).isEnabled=false
  group.getChildAt(2).isEnabled=false
  root.addView(group)
  val mobile=CheckBox(this).apply { text="Permitir transcripción en línea con datos móviles"; isChecked=prefs.getBoolean("mobile_online",false) }
  val summary=CheckBox(this).apply { text="Generar resumen automáticamente después de transcribir"; isChecked=prefs.getBoolean("auto_summary",true) }
  mobile.isEnabled=false;summary.isEnabled=false;summary.isChecked=false
  root.addView(mobile);root.addView(summary)
  root.addView(TextView(this).apply { text="Actualmente solo está disponible Whisper local. Las opciones en línea y el resumen automático aún no están implementados.";textSize=15f })
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
   val chosen="local"
   prefs.edit().putString("mode",chosen).putBoolean("mobile_online",mobile.isChecked).putBoolean("auto_summary",summary.isChecked).apply()
   Toast.makeText(this@TranscriptionSettingsActivity,"Preferencias guardadas",Toast.LENGTH_SHORT).show()
  } })
  val modelState=TextView(this).apply { text=if(WhisperModelManager.installed(this@TranscriptionSettingsActivity)) "Modelo Whisper Base descargado" else "Modelo Whisper Base pendiente (aprox. 142 MB)"; textSize=16f }
  root.addView(modelState)
  root.addView(TextView(this).apply { text="Motor Whisper integrado en esta versión. La transcripción se ejecuta localmente y puede tardar varios minutos.";textSize=15f })
  val downloadButton=Button(this).apply { text="DESCARGAR MODELO WHISPER BASE POR WI-FI" }
  downloadButton.setOnClickListener {
   downloadButton.isEnabled=false
   modelState.text="Preparando descarga..."
   Thread { WhisperModelManager.download(applicationContext) { message -> runOnUiThread { modelState.text=message } };runOnUiThread { downloadButton.isEnabled=true } }.start()
  }
  root.addView(downloadButton)
  root.addView(TextView(this).apply { text="Transcripción local con Whisper Base. El procesamiento en línea y los resúmenes automáticos están pendientes de desarrollo. Ningún audio se envía a internet para transcribir.";textSize=15f })
  val scroll=ScrollView(this).apply { setBackgroundColor(UiTheme.background(UiTheme.dark(this@TranscriptionSettingsActivity))); addView(root) }
  setContentView(scroll)
  scroll.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(0,insets.systemWindowInsetTop,0,insets.systemWindowInsetBottom);insets }
  scroll.requestApplyInsets()
 }
}
