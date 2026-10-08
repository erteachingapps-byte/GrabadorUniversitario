package com.edwin.grabador

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : Activity() {
    private var subject = "Clase"
    private lateinit var selectedView: TextView
    private lateinit var statusView: TextView
    private lateinit var timerView: TextView
    private lateinit var marksView: TextView
    private var recording = false
    private var paused = false
    private var marks = 0
    private var startMs = 0L
    private var pausedAt = 0L
    private var pausedTotal = 0L
    private val handler = Handler(Looper.getMainLooper())

    private val courses = listOf(
        "Lunes 1 · Fundamentos de la Didáctica",
        "Martes 1 · Uso y Manejo de la Voz",
        "Martes 2 · Currículum Nacional",
        "Miércoles 1 · Atención a la Diversidad",
        "Miércoles 2 · Currículum Nacional",
        "Jueves 1 · Estrategia Psicopedagógica",
        "Viernes 1 · Comunicación y Tecnología Educativa"
    )

    private val ticker = object : Runnable {
        override fun run() {
            if (recording) {
                val now = if (paused) pausedAt else System.currentTimeMillis()
                val elapsed = (now - startMs - pausedTotal).coerceAtLeast(0L) / 1000
                timerView.text = String.format(Locale.US, "%02d:%02d:%02d", elapsed/3600, (elapsed%3600)/60, elapsed%60)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,28,28,28) }
        root.addView(TextView(this).apply { text="Mis Clases · Grabadora"; textSize=26f })
        selectedView=TextView(this).apply { text="Selecciona una clase o reunión"; textSize=18f; setPadding(0,12,0,12) }
        root.addView(selectedView)
        statusView=TextView(this).apply { text="LISTO"; textSize=20f }
        timerView=TextView(this).apply { text="00:00:00"; textSize=36f }
        marksView=TextView(this).apply { text="★ Marcas: 0"; textSize=18f }
        root.addView(statusView); root.addView(timerView); root.addView(marksView)

        root.addView(section("CLASES"))
        courses.forEach { root.addView(subjectButton(it)) }
        root.addView(section("REUNIONES"))
        root.addView(subjectButton("Reuniones TAC"))

        root.addView(Button(this).apply { text="📚 MIS GRABACIONES"; setOnClickListener { startActivity(Intent(this@MainActivity, RecordingsActivity::class.java)) } })
        root.addView(actionButton("● INICIAR GRABACIÓN","START"))
        root.addView(actionButton("Ⅱ PAUSAR","PAUSE"))
        root.addView(actionButton("▶ REANUDAR","RESUME"))
        root.addView(actionButton("★ MARCAR MOMENTO","MARK"))
        root.addView(actionButton("■ FINALIZAR Y GUARDAR","STOP"))
        setContentView(ScrollView(this).apply {\n            clipToPadding = false\n            setPadding(0, 0, 0, (resources.displayMetrics.density * 96).toInt())\n            isFillViewport = true\n            addView(root)\n        })
    }

    private fun section(t:String)=TextView(this).apply { text=t; textSize=18f; setPadding(0,24,0,8) }
    private fun subjectButton(name:String)=Button(this).apply {
        text=name
        setOnClickListener {
            if(recording){ Toast.makeText(context,"Finaliza la grabación antes de cambiar.",Toast.LENGTH_SHORT).show() }
            else { subject=name; selectedView.text=name }
        }
    }

    private fun actionButton(label:String, action:String)=Button(this).apply {
        text=label
        setOnClickListener {
            if(action=="START" && ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this@MainActivity,arrayOf(Manifest.permission.RECORD_AUDIO),10)
                return@setOnClickListener
            }
            when(action){
                "START" -> if(!recording){ sendAction(action); recording=true; paused=false; marks=0; startMs=System.currentTimeMillis(); pausedTotal=0; statusView.text="🔴 GRABANDO"; marksView.text="★ Marcas: 0"; handler.post(ticker) }
                "PAUSE" -> if(recording && !paused){ sendAction(action); paused=true; pausedAt=System.currentTimeMillis(); statusView.text="Ⅱ PAUSADA" }
                "RESUME" -> if(recording && paused){ sendAction(action); pausedTotal += System.currentTimeMillis()-pausedAt; paused=false; statusView.text="🔴 GRABANDO" }
                "MARK" -> if(recording){ sendAction(action); marks++; marksView.text="★ Marcas: $marks"; Toast.makeText(context,"Momento importante marcado",Toast.LENGTH_SHORT).show() }
                "STOP" -> if(recording){ sendAction(action); recording=false; paused=false; handler.removeCallbacks(ticker); statusView.text="✓ GUARDADA"; Toast.makeText(context,"Grabación guardada",Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun sendAction(action:String){
        val i=Intent(this,RecordingService::class.java).putExtra("action",action).putExtra("subject",subject)
        if(action=="START") ContextCompat.startForegroundService(this,i) else startService(i)
    }

    override fun onDestroy(){ handler.removeCallbacks(ticker); super.onDestroy() }
}
