package com.edwin.grabador

import android.app.*
import android.content.Intent
import android.media.MediaRecorder
import android.os.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class RecordingService:Service(){
 private var recorder:MediaRecorder?=null; private var dir:File?=null; private var part=0; private var sessionStamp=""; private var started=0L; private val handler=Handler(Looper.getMainLooper()); private val rotate=object:Runnable{override fun run(){if(recorder!=null){stopPart();startPart()};handler.postDelayed(this,15*60*1000L)}}
 override fun onBind(i:Intent?)=null
 override fun onCreate(){super.onCreate();if(Build.VERSION.SDK_INT>=26)getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("recording","Grabación de clases",NotificationManager.IMPORTANCE_LOW))}
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{when(i?.getStringExtra("action")){"START"->if(recorder==null)begin(i.getStringExtra("subject")?:"Clase");"PAUSE"->{if(Build.VERSION.SDK_INT>=24)recorder?.pause();mark("PAUSA")};"RESUME"->{if(Build.VERSION.SDK_INT>=24)recorder?.resume();mark("REANUDAR")};"MARK"->mark("IMPORTANTE");"STOP"->{handler.removeCallbacks(rotate);stopPart();stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}};return START_NOT_STICKY}
 private fun begin(subject:String){val day=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());val time=SimpleDateFormat("HHmmss",Locale.US).format(Date());sessionStamp=day+"_"+SimpleDateFormat("HH-mm-ss",Locale.US).format(Date());val safe=subject.replace(Regex("[^A-Za-z0-9ÁÉÍÓÚáéíóúÑñ ._-]"),"_");dir=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases/"+safe+"/"+day+"/"+time).apply{mkdirs()};startForeground(7,Notification.Builder(this,"recording").setContentTitle("Grabando clase").setContentText("Grabación protegida en curso").setSmallIcon(android.R.drawable.ic_btn_speak_now).build());startPart();handler.postDelayed(rotate,15*60*1000L)}
 private fun startPart(){part++;val file=File(dir,String.format(Locale.US,"%s_%03d.m4a",sessionStamp,part));recorder=if(Build.VERSION.SDK_INT>=31)MediaRecorder(this)else @Suppress("DEPRECATION") MediaRecorder();recorder!!.apply{setAudioSource(MediaRecorder.AudioSource.MIC);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setAudioEncodingBitRate(64000);setAudioSamplingRate(44100);setOutputFile(file.absolutePath);prepare();start()};started=System.currentTimeMillis()}
 private fun stopPart(){try{recorder?.stop()}catch(_:Exception){};recorder?.release();recorder=null}
 private fun mark(kind:String){dir?.let{File(it,"marcas.txt").appendText(kind+" | segmento "+part+" | "+((System.currentTimeMillis()-started)/1000)+"s\n")}}
 override fun onDestroy(){handler.removeCallbacks(rotate);stopPart();super.onDestroy()}
}
