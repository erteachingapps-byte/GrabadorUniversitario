package com.edwin.grabador

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Downloads only the public multilingual base model. No recordings leave the device. */
object WhisperModelManager {
 const val MODEL_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base.bin"
 private const val MIN_BYTES = 100_000_000L
 fun model(context:Context) = File(context.filesDir,"models/ggml-base.bin")
 fun installed(context:Context) = model(context).let { it.isFile && it.length() >= MIN_BYTES }

 fun download(context:Context, progress:(String)->Unit) {
  val cm=context.getSystemService(ConnectivityManager::class.java)
  val caps=cm.getNetworkCapabilities(cm.activeNetwork)
  if(caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)!=true ||
      !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
   progress("Conecta el teléfono a Wi-Fi con internet para descargar el modelo."); return
  }
  val target=model(context)
  target.parentFile?.mkdirs()
  val temp=File(target.parentFile,"ggml-base.bin.part")
  try {
   val conn=URL(MODEL_URL).openConnection() as HttpURLConnection
   conn.connectTimeout=20000;conn.readTimeout=30000;conn.instanceFollowRedirects=true
   try {
    if(conn.responseCode !in 200..299) error("HTTP "+conn.responseCode)
    val expected=conn.contentLengthLong
    conn.inputStream.use { input ->
     temp.outputStream().buffered().use { output ->
      val buffer=ByteArray(65536);var total=0L;var last=-1
      while(true) {
       val count=input.read(buffer);if(count<0) break
       output.write(buffer,0,count);total+=count
       if(expected>0) {
        val pct=(total*100/expected).toInt()
        if(pct/10!=last/10) {last=pct;progress("Descargando modelo: $pct%")}
       }
      }
     }
    }
    if(temp.length()<MIN_BYTES || (expected>0 && temp.length()!=expected)) error("Descarga incompleta")
    if(!temp.renameTo(target)) error("No se pudo guardar el modelo")
    progress("Modelo Base descargado. Falta integrar el motor nativo de transcripción.")
   } finally {conn.disconnect()}
  } catch(e:Exception) {
   temp.delete()
   progress("Error al descargar el modelo: "+(e.message?:"desconocido"))
  }
 }
}
