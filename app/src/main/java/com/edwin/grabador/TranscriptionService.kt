package com.edwin.grabador

import android.app.*
import android.content.Intent
import android.os.*
import java.io.File

class TranscriptionService : Service() {
 companion object { const val CHANNEL="whisper_processing"; const val ID=81; @Volatile var running=false }
 override fun onBind(intent:Intent?)=null
 override fun onCreate() {
  super.onCreate()
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Transcripciones Whisper",NotificationManager.IMPORTANCE_LOW))
 }
 private fun notifyStatus(title:String,body:String,ongoing:Boolean):Notification {
  val intent=PendingIntent.getActivity(this,0,Intent(this,RecordingsActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  return Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle(title).setContentText(body).setContentIntent(intent).setOngoing(ongoing).build()
 }
 private fun update(body:String) { getSystemService(NotificationManager::class.java).notify(ID,notifyStatus("Whisper: transcribiendo",body,true)) }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(running) return START_NOT_STICKY
  val path=intent?.getStringExtra("session") ?: return START_NOT_STICKY
  val session=File(path)
  val root=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases")
  if(!session.canonicalPath.startsWith(root.canonicalPath+File.separator) || !session.isDirectory) return START_NOT_STICKY
  val audios=session.listFiles()?.filter { it.extension.equals("m4a",true) }?.sortedBy { it.name } ?: emptyList()
  if(audios.isEmpty() || !WhisperModelManager.installed(this)) return START_NOT_STICKY
  running=true
  startForeground(ID,notifyStatus("Whisper: transcribiendo","Preparando grabación...",true))
  Thread {
   try {
    val output=StringBuilder()
    audios.forEachIndexed { index,audio ->
     update("Preparando audio ${index+1} de ${audios.size}")
     val samples=AudioPcmDecoder.decode16kMono(audio)
     require(samples.size<=16000*180) { "Por ahora, máximo 3 minutos por segmento." }
     update("Procesando con Whisper ${index+1} de ${audios.size}")
     val result=WhisperNative.transcribe(WhisperModelManager.model(this).absolutePath,samples)
     require(!result.startsWith("ERROR:")) { result }
     output.append("Segmento ${index+1}: ").append(audio.name).append("\n").append(result).append("\n")
    }
    File(session,"transcripcion.txt").writeText(output.toString())
    getSystemService(NotificationManager::class.java).notify(ID+1,notifyStatus("Transcripción terminada","Abre Mis grabaciones para leer el texto.",false))
   } catch(e:Throwable) {
    File(session,"transcripcion-error.txt").writeText(e.message ?: e.javaClass.simpleName)
    getSystemService(NotificationManager::class.java).notify(ID+1,notifyStatus("No se pudo transcribir","Abre Mis grabaciones para revisar el error.",false))
   } finally {
    running=false
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf(startId)
   }
  }.start()
  return START_NOT_STICKY
 }
}
