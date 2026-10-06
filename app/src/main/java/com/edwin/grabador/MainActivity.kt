package com.edwin.grabador

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : Activity() {
    private var subject = "Clase"

    private val courses = listOf(
        "Lunes 1 · Fundamentos de la Didáctica",
        "Martes 1 · Uso y Manejo de la Voz",
        "Martes 2 · Currículum Nacional",
        "Miércoles 1 · Atención a la Diversidad",
        "Miércoles 2 · Currículum Nacional",
        "Jueves 1 · Estrategia Psicopedagógica",
        "Viernes 1 · Comunicación y Tecnología Educativa"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 28, 28, 28)
        }

        root.addView(TextView(this).apply {
            text = "Mis Clases · Grabadora"
            textSize = 26f
        })

        root.addView(TextView(this).apply {
            text = "CLASES"
            textSize = 18f
            setPadding(0, 24, 0, 8)
        })

        courses.forEach { course ->
            root.addView(subjectButton(course))
        }

        root.addView(TextView(this).apply {
            text = "REUNIONES"
            textSize = 18f
            setPadding(0, 28, 0, 8)
        })
        root.addView(subjectButton("Reuniones TAC"))

        root.addView(actionButton("● INICIAR GRABACIÓN", "START"))
        root.addView(actionButton("Ⅱ PAUSAR", "PAUSE"))
        root.addView(actionButton("▶ REANUDAR", "RESUME"))
        root.addView(actionButton("★ MARCAR MOMENTO", "MARK"))
        root.addView(actionButton("■ FINALIZAR Y GUARDAR", "STOP"))

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun subjectButton(name: String): Button =
        Button(this).apply {
            text = name
            setOnClickListener {
                subject = name
                Toast.makeText(context, "Seleccionada: $name", Toast.LENGTH_SHORT).show()
            }
        }

    private fun actionButton(label: String, action: String): Button =
        Button(this).apply {
            text = label
            setOnClickListener {
                if (
                    action == "START" &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.RECORD_AUDIO),
                        10
                    )
                } else {
                    sendRecorderAction(action)
                }
            }
        }

    private fun sendRecorderAction(action: String) {
        val intent = Intent(this, RecordingService::class.java)
            .putExtra("action", action)
            .putExtra("subject", subject)

        if (action == "START") {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
    }
}
