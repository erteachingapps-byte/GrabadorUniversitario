package com.edwin.grabador

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** User-controlled backup through Android's document picker; no storage permission required. */
class BackupActivity : Activity() {
 private lateinit var status:TextView
 private val backupCode=201
 private val restoreCode=202
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  UiTheme.apply(this)
  val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28) }
  root.addView(TextView(this).apply { text="Copias de seguridad";textSize=26f })
  root.addView(TextView(this).apply { text="Exporta tus grabaciones, marcas, transcripciones y modelo Whisper a un ZIP. Guárdalo fuera de la aplicación (por ejemplo, Descargas). La copia puede superar 150 MB.";textSize=17f })
  root.addView(Button(this).apply { text="EXPORTAR COPIA ZIP";setOnClickListener {
   val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="application/zip";putExtra(Intent.EXTRA_TITLE,"GrabadorUniversitario-copia.zip") }
   startActivityForResult(intent,backupCode)
  } })
  root.addView(Button(this).apply { text="RESTAURAR COPIA ZIP";setOnClickListener {
   AlertDialog.Builder(this@BackupActivity).setTitle("Restaurar copia").setMessage("Se incorporarán los archivos de la copia. Si tienen el mismo nombre, se reemplazarán. No restaures mientras estás grabando.").setNegativeButton("Cancelar",null).setPositiveButton("Elegir archivo") { _,_ ->
    val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="application/zip" }
    startActivityForResult(intent,restoreCode)
   }.show()
  } })
  status=TextView(this).apply { text="Listo para exportar o restaurar.";textSize=17f }
  root.addView(status)
  setContentView(root)
  root.setOnApplyWindowInsetsListener { view,insets -> view.setPadding(28,28+insets.systemWindowInsetTop,28,28+insets.systemWindowInsetBottom);insets }
  root.requestApplyInsets()
 }
 private fun recordingsDir()=File(getExternalFilesDir(Environment.DIRECTORY_MUSIC),"Clases")
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
  super.onActivityResult(requestCode,resultCode,data)
  val uri=data?.data ?: return
  if(resultCode!=RESULT_OK) return
  status.text=if(requestCode==backupCode) "Exportando copia... No cierres esta pantalla." else "Restaurando copia... No cierres esta pantalla."
  Thread {
   try {
    if(requestCode==backupCode) {
     contentResolver.openOutputStream(uri,"w")!!.use { out ->
      ZipOutputStream(out.buffered()).use { zip ->
       val recordings=recordingsDir()
       if(recordings.exists()) recordings.walkTopDown().filter { it.isFile }.forEach { f ->
        zip.putNextEntry(ZipEntry("grabaciones/"+f.relativeTo(recordings).invariantSeparatorsPath))
        f.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
       }
       val model=WhisperModelManager.model(this)
       if(model.isFile) {
        zip.putNextEntry(ZipEntry("modelo/ggml-base.bin"))
        model.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
       }
      }
     }
    } else if(requestCode==restoreCode) {
     contentResolver.openInputStream(uri)!!.use { input ->
      ZipInputStream(input.buffered()).use { zip ->
       var entry=zip.nextEntry
       var files=0
       while(entry!=null) {
        if(!entry.isDirectory) {
         val destination=when {
          entry.name.startsWith("grabaciones/") -> File(recordingsDir(),entry.name.removePrefix("grabaciones/"))
          entry.name=="modelo/ggml-base.bin" -> WhisperModelManager.model(this)
          else -> null
         }
         if(destination!=null) {
          val base=if(entry.name.startsWith("grabaciones/")) recordingsDir() else WhisperModelManager.model(this).parentFile!!
          require(destination.canonicalPath.startsWith(base.canonicalPath+File.separator)) { "Ruta inválida en la copia" }
          destination.parentFile?.mkdirs()
          destination.outputStream().use { zip.copyTo(it) }
          files++
         }
        }
        zip.closeEntry()
        entry=zip.nextEntry
       }
       require(files>0) { "La copia no contiene archivos compatibles" }
      }
     }
    }
    runOnUiThread { status.text=if(requestCode==backupCode) "Copia ZIP exportada correctamente. Verifica que esté en la carpeta elegida." else "Copia restaurada. Puedes revisar Mis grabaciones." }
   } catch(e:Exception) { runOnUiThread { status.text="Error: "+(e.message?:"No se pudo completar la operación") } }
  }.start()
 }
}
