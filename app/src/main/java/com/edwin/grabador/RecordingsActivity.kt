package com.edwin.grabador

import android.app.*
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import java.io.File

class RecordingsActivity : Activity() {
 private var player:MediaPlayer?=null
 override fun onCreate(b:Bundle?){super.onCreate(b);UiTheme.apply(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28)};root.addView(TextView(this).apply{text="Mis grabaciones";textSize=28f});root.addView(Button(this).apply{text="■ DETENER REPRODUCCIÓN";setOnClickListener{stopAudio()}});val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(ScrollView(this).apply{addView(list)});setContentView(root);root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(28,28+insets.systemWindowInsetTop,28,28+insets.systemWindowInsetBottom);insets };root.requestApplyInsets();load(list)}
 private fun load(list:LinearLayout){val base=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases");val sessions=if(base.exists())base.walkTopDown().filter{it.isDirectory&&it.listFiles()?.any{f->f.extension.equals("m4a",true)}==true}.toList().sortedByDescending{it.lastModified()}else emptyList();if(sessions.isEmpty()){list.addView(TextView(this).apply{text="Todavía no hay grabaciones.";textSize=18f});return};sessions.forEach{s->val rel=s.relativeTo(base).path;val audios=s.listFiles()?.filter{it.extension.equals("m4a",true)}?.sortedBy{it.name}?:emptyList();val mf=File(s,"marcas.txt");val mc=if(mf.exists())mf.readLines().count{it.contains("IMPORTANTE")}else 0;list.addView(TextView(this).apply{text=rel.replace(File.separator,"  ›  ");textSize=19f;setPadding(0,18,0,4)});list.addView(TextView(this).apply{text=audios.size.toString()+" segmento(s) · "+mc+" marca(s) ★"});audios.forEach{f->list.addView(Button(this).apply{text="▶ "+f.name;setOnClickListener{play(f)}})};list.addView(Button(this).apply{text="📝 TRANSCRIPCIÓN Y RESUMEN";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Procesamiento pendiente").setMessage(TranscriptionRouter.decide(this@RecordingsActivity).explanation + "\n\nSesión: " + rel + "\nSegmentos: " + audios.size + "\nResumen automático: " + if(getSharedPreferences("transcription_settings",MODE_PRIVATE).getBoolean("auto_summary",true)) "activado (pendiente de motor)" else "desactivado").setPositiveButton("Entendido",null).show()}});if(mf.exists())list.addView(Button(this).apply{text="★ VER MARCAS";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Marcas").setMessage(mf.readText()).setPositiveButton("Cerrar",null).show()}})}}
 private fun play(f:File){stopAudio();try{player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start()};Toast.makeText(this,"Reproduciendo "+f.name,Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this,"No se pudo reproducir.",Toast.LENGTH_LONG).show()}}
 private fun stopAudio(){try{player?.stop()}catch(_:Exception){};player?.release();player=null}
 override fun onDestroy(){stopAudio();super.onDestroy()}
}
