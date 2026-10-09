package com.edwin.grabador

import android.app.*
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import java.io.File

class RecordingsActivity : Activity() {
 private var player:MediaPlayer?=null
 override fun onCreate(b:Bundle?){super.onCreate(b);UiTheme.apply(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28)};root.addView(TextView(this).apply{text="Mis grabaciones";textSize=28f});root.addView(Button(this).apply{text="■ DETENER REPRODUCCIÓN";setOnClickListener{stopAudio()}});val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(ScrollView(this).apply{addView(list)});setContentView(root);root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(28,28+insets.systemWindowInsetTop,28,28+insets.systemWindowInsetBottom);insets };root.requestApplyInsets();load(list)}
 private fun load(list:LinearLayout){val base=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases");val sessions=if(base.exists())base.walkTopDown().filter{it.isDirectory&&it.listFiles()?.any{f->f.extension.equals("m4a",true)}==true}.toList().sortedByDescending{it.lastModified()}else emptyList();if(sessions.isEmpty()){list.addView(TextView(this).apply{text="Todavía no hay grabaciones.";textSize=18f});return};sessions.forEach{s->val rel=s.relativeTo(base).path;val audios=s.listFiles()?.filter{it.extension.equals("m4a",true)}?.sortedBy{it.name}?:emptyList();val mf=File(s,"marcas.txt");val mc=if(mf.exists())mf.readLines().count{it.contains("IMPORTANTE")}else 0;list.addView(TextView(this).apply{text=rel.replace(File.separator,"  ›  ");textSize=19f;setPadding(0,18,0,4)});list.addView(TextView(this).apply{text=audios.size.toString()+" segmento(s) · "+mc+" marca(s) ★"});audios.forEach{f->list.addView(Button(this).apply{text="▶ "+f.name;setOnClickListener{play(f)}})};list.addView(Button(this).apply{text="📝 TRANSCRIBIR GRABACIÓN";setOnClickListener{transcribeSession(s,audios)}});val error=File(s,"transcripcion-error.txt");if(error.isFile)list.addView(Button(this).apply{text="⚠ VER ERROR DE TRANSCRIPCIÓN";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Error de transcripción").setMessage(error.readText()).setPositiveButton("Cerrar",null).show()}});val transcript=File(s,"transcripcion.txt");if(transcript.isFile)list.addView(Button(this).apply{text="📄 VER TRANSCRIPCIÓN";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Transcripción").setMessage(transcript.readText().take(25000)).setPositiveButton("Cerrar",null).show()}});if(mf.exists())list.addView(Button(this).apply{text="★ VER MARCAS";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Marcas").setMessage(mf.readText()).setPositiveButton("Cerrar",null).show()}})}}
 private fun transcribeSession(session:File, audios:List<File>) {
  if(!WhisperModelManager.installed(this)) {
   AlertDialog.Builder(this).setTitle("Falta modelo").setMessage("Descarga Whisper Base desde Preferencias.").setPositiveButton("Cerrar",null).show()
   return
  }
  if(TranscriptionService.running) {
   Toast.makeText(this,"Ya hay una transcripción en proceso.",Toast.LENGTH_LONG).show()
   return
  }
  try {
   androidx.core.content.ContextCompat.startForegroundService(this,android.content.Intent(this,TranscriptionService::class.java).putExtra("session",session.absolutePath))
   AlertDialog.Builder(this).setTitle("Transcripción iniciada").setMessage("Whisper seguirá procesando en segundo plano. Consulta la notificación y vuelve a Mis grabaciones al terminar.").setPositiveButton("Entendido",null).show()
  } catch(e:Exception) {
   AlertDialog.Builder(this).setTitle("No se pudo iniciar").setMessage(e.message).setPositiveButton("Cerrar",null).show()
  }
 }
 private fun play(f:File){stopAudio();try{player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start()};Toast.makeText(this,"Reproduciendo "+f.name,Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this,"No se pudo reproducir.",Toast.LENGTH_LONG).show()}}
 private fun stopAudio(){try{player?.stop()}catch(_:Exception){};player?.release();player=null}
 override fun onDestroy(){stopAudio();super.onDestroy()}
}
