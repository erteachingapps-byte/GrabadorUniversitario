package com.edwin.grabador

import android.app.*
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import java.io.File

class RecordingsActivity : Activity() {
 private var player:MediaPlayer?=null
 override fun onCreate(b:Bundle?){super.onCreate(b);UiTheme.apply(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28)};root.addView(TextView(this).apply{text="Mis grabaciones";textSize=28f});root.addView(Button(this).apply{text="■ DETENER REPRODUCCIÓN";setOnClickListener{stopAudio()}});val list=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(ScrollView(this).apply{addView(list)});setContentView(root);root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(28,28+insets.systemWindowInsetTop,28,28+insets.systemWindowInsetBottom);insets };root.requestApplyInsets();load(list)}
 private fun load(list:LinearLayout){val base=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases");val sessions=if(base.exists())base.walkTopDown().filter{it.isDirectory&&it.listFiles()?.any{f->f.extension.equals("m4a",true)}==true}.toList().sortedByDescending{it.lastModified()}else emptyList();if(sessions.isEmpty()){list.addView(TextView(this).apply{text="Todavía no hay grabaciones.";textSize=18f});return};sessions.forEach{s->val rel=s.relativeTo(base).path;val audios=s.listFiles()?.filter{it.extension.equals("m4a",true)}?.sortedBy{it.name}?:emptyList();val mf=File(s,"marcas.txt");val mc=if(mf.exists())mf.readLines().count{it.contains("IMPORTANTE")}else 0;list.addView(TextView(this).apply{text=rel.replace(File.separator,"  ›  ");textSize=19f;setPadding(0,18,0,4)});list.addView(TextView(this).apply{text=audios.size.toString()+" segmento(s) · "+mc+" marca(s) ★"});audios.forEach{f->list.addView(Button(this).apply{text="▶ "+f.name;setOnClickListener{play(f)}})};list.addView(Button(this).apply{text="📝 TRANSCRIBIR GRABACIÓN";setOnClickListener{transcribeSession(s,audios)}});val transcript=File(s,"transcripcion.txt");if(transcript.isFile)list.addView(Button(this).apply{text="📄 VER TRANSCRIPCIÓN";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Transcripción").setMessage(transcript.readText().take(25000)).setPositiveButton("Cerrar",null).show()}});if(mf.exists())list.addView(Button(this).apply{text="★ VER MARCAS";setOnClickListener{AlertDialog.Builder(this@RecordingsActivity).setTitle("Marcas").setMessage(mf.readText()).setPositiveButton("Cerrar",null).show()}})}}
 private fun transcribeSession(session:File, audios:List<File>) {
  if (!WhisperModelManager.installed(this)) {
   AlertDialog.Builder(this).setTitle("Falta modelo").setMessage("Descarga Whisper Base desde Preferencias de transcripción.").setPositiveButton("Entendido",null).show()
   return
  }
  if (audios.isEmpty()) return
  val dialog=AlertDialog.Builder(this).setTitle("Transcribiendo sin internet").setMessage("Procesando audio en el teléfono. No cierres esta pantalla.").setCancelable(false).create()
  dialog.show()
  Thread {
   try {
    val output=StringBuilder()
    audios.forEachIndexed { index, audio ->
     runOnUiThread { dialog.setMessage("Transcribiendo segmento ${index+1} de ${audios.size}...") }
     runOnUiThread { dialog.setMessage("Preparando audio ${index+1} de ${audios.size}...") }
     val samples=AudioPcmDecoder.decode16kMono(audio)
     require(samples.size <= 16000*180) { "Para esta primera prueba, usa segmentos de hasta 3 minutos." }
     runOnUiThread { dialog.setMessage("Whisper está procesando ${index+1} de ${audios.size}. Puede tardar varios minutos...") }
     val result=WhisperNative.transcribe(WhisperModelManager.model(this).absolutePath,samples)
     require(!result.startsWith("ERROR:")) { result }
     output.append("Segmento ${index+1}: ").append(audio.name).append("\\n").append(result).append("\\n")
    }
    File(session,"transcripcion.txt").writeText(output.toString())
    runOnUiThread {
     dialog.dismiss()
     AlertDialog.Builder(this).setTitle("Transcripción guardada").setMessage(output.toString().take(2500)+"\\n\\nArchivo: transcripcion.txt").setPositiveButton("Cerrar",null).show()
    }
   } catch(e:Exception) {
    runOnUiThread { dialog.dismiss();AlertDialog.Builder(this).setTitle("No se pudo transcribir").setMessage(e.message ?: "Error desconocido").setPositiveButton("Cerrar",null).show() }
   } catch(e:UnsatisfiedLinkError) {
    runOnUiThread { dialog.dismiss();AlertDialog.Builder(this).setTitle("Motor nativo no disponible").setMessage(e.message).setPositiveButton("Cerrar",null).show() }
   }
  }.start()
 }
 private fun play(f:File){stopAudio();try{player=MediaPlayer().apply{setDataSource(f.absolutePath);prepare();start()};Toast.makeText(this,"Reproduciendo "+f.name,Toast.LENGTH_SHORT).show()}catch(e:Exception){Toast.makeText(this,"No se pudo reproducir.",Toast.LENGTH_LONG).show()}}
 private fun stopAudio(){try{player?.stop()}catch(_:Exception){};player?.release();player=null}
 override fun onDestroy(){stopAudio();super.onDestroy()}
}
