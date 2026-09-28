package com.edwin.grabador

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
 private var subject="Clase"
 private val courses=listOf("Lunes 1 · Fundamentos de la Didáctica","Martes 1 · Uso y Manejo de la Voz","Martes 2 · Currículum Nacional","Miércoles 1 · Atención a la Diversidad","Miércoles 2 · Currículum Nacional","Jueves 1 · Estrategia Psicopedagógica","Viernes 1 · Comunicación y Tecnología Educativa")
 override fun onCreate(b:Bundle?){ super.onCreate(b); val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(28,28,28,28)}; root.addView(TextView(this).apply{text="Mis Clases · Grabadora";textSize=26f}); courses.forEach{c->root.addView(Button(this).apply{text=c;setOnClickListener{subject=c;Toast.makeText(context,"Seleccionada: "+c,Toast.LENGTH_SHORT).show()}})}
 fun btn(label:String,action:String)=Button(this).apply{text=label;setOnClickListener{if(action=="START"&&ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this@MainActivity,arrayOf(Manifest.permission.RECORD_AUDIO),10)}else{val i=Intent(this@MainActivity,RecordingService::class.java).putExtra("action",action).putExtra("subject",subject);if(action=="START")ContextCompat.startForegroundService(this@MainActivity,i)else startService(i)}}}; root.addView(btn("● INICIAR GRABACIÓN","START"));root.addView(btn("Ⅱ PAUSAR","PAUSE"));root.addView(btn("▶ REANUDAR","RESUME"));root.addView(btn("★ MARCAR MOMENTO","MARK"));root.addView(btn("■ FINALIZAR Y GUARDAR","STOP"));setContentView(ScrollView(this).apply{addView(root)}) }
}
