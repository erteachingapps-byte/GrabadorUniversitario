package com.edwin.grabador

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.*
import android.widget.*
import java.io.File

class RecordingsActivity : Activity() {
    private var player: MediaPlayer? = null
    private lateinit var list: LinearLayout
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { load(); handler.postDelayed(this, 5000) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); UiTheme.apply(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,28,28,28) }
        root.addView(TextView(this).apply { text = "Mis grabaciones · 0.2"; textSize = 28f })
        root.addView(Button(this).apply { text = "PREFERENCIAS DE TRANSCRIPCIÓN"; setOnClickListener { startActivity(Intent(this@RecordingsActivity, TranscriptionSettingsActivity::class.java)) } })
        root.addView(Button(this).apply { text = "■ DETENER REPRODUCCIÓN"; setOnClickListener { stopAudio() } })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(list) }, LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(28,28 + insets.systemWindowInsetTop,28,28 + insets.systemWindowInsetBottom); insets }
        root.requestApplyInsets()
    }
    override fun onResume() { super.onResume(); handler.post(refresh) }
    override fun onPause() { handler.removeCallbacks(refresh); super.onPause() }
    private fun load() {
        // Only rebuild when files/status change; this preserves scroll position while idle.
        val base = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Clases")
        val sessions = if (base.exists()) base.walkTopDown().filter { it.isDirectory && it.listFiles()?.any { f -> f.extension.equals("m4a",true) } == true }.toList().sortedByDescending { it.name } else emptyList()
        val signature = sessions.joinToString("|") { s -> s.path + s.listFiles().orEmpty().sortedBy { it.name }.joinToString { "${it.name}:${it.lastModified()}:${it.length()}" } } + TranscriptionService.running
        if (list.tag == signature) return
        list.tag = signature; list.removeAllViews()
        if (sessions.isEmpty()) { list.addView(TextView(this).apply { text = "Todavía no hay grabaciones."; textSize = 18f }); return }
        sessions.forEach { session ->
            val audios = session.listFiles()?.filter { it.extension.equals("m4a",true) }?.sortedBy { it.name }.orEmpty()
            val marks = File(session, "marcas.txt")
            val count = if (marks.isFile) marks.readLines().count { it.contains("IMPORTANTE") } else 0
            list.addView(TextView(this).apply { text = session.relativeTo(base).path.replace(File.separator, " › "); textSize = 19f; setPadding(0,18,0,4) })
            list.addView(TextView(this).apply { text = "${audios.size} segmento(s) · $count marca(s) ★" })
            audios.forEach { audio -> list.addView(Button(this).apply { text = "▶ ${audio.name}"; setOnClickListener { play(audio) } }) }
            val state = File(session, "estado-transcripcion.txt")
            if (state.isFile) list.addView(TextView(this).apply {
                val status = state.readText()
                text = if (!TranscriptionService.running && (status.startsWith("Preparando") || status.startsWith("Enviando") || status.startsWith("Groq procesa") || status.contains("completado(s)")))
                    "El procesamiento se interrumpió. Pulsa Transcribir para continuar desde los bloques guardados."
                else status
                textSize = 16f
            })
            list.addView(Button(this).apply { text = "📝 TRANSCRIBIR / CONTINUAR"; isEnabled = !TranscriptionService.running; setOnClickListener { transcribeSession(session) } })
            val error = File(session, "transcripcion-error.txt")
            if (error.isFile) list.addView(Button(this).apply { text = "⚠ VER AVISO"; setOnClickListener { showText("Aviso de transcripción", error) } })
            listOf("transcripcion.txt", "transcripcion-parcial.txt").forEach { name ->
                val file = File(session, name)
                if (file.isFile) list.addView(Button(this).apply {
                    text = if (name.contains("parcial")) "📄 VER TEXTO PARCIAL" else "📄 VER TRANSCRIPCIÓN COMPLETA"
                    setOnClickListener { startActivity(Intent(this@RecordingsActivity, TranscriptActivity::class.java).putExtra("file", file.absolutePath)) }
                })
            }
            if (marks.isFile) list.addView(Button(this).apply { text = "★ VER MARCAS"; setOnClickListener { showText("Marcas", marks) } })
        }
    }
    private fun showText(title: String, file: File) {
        AlertDialog.Builder(this).setTitle(title).setMessage(file.readText()).setPositiveButton("Cerrar",null).show()
    }
    private fun transcribeSession(session: File) {
        if (RecordingService.recording) { Toast.makeText(this, "Detén la grabación antes de transcribir.", Toast.LENGTH_LONG).show(); return }
        if (TranscriptionService.running) return
        val decision = TranscriptionRouter.decide(this)
        if (decision.route != TranscriptionRouter.Route.ONLINE_READY && decision.route != TranscriptionRouter.Route.LOCAL_READY) {
            AlertDialog.Builder(this).setTitle("Antes de transcribir").setMessage(decision.explanation)
                .setNegativeButton("Cerrar",null).setPositiveButton("Preferencias") { _,_ -> startActivity(Intent(this, TranscriptionSettingsActivity::class.java)) }.show()
            return
        }
        fun start() {
            try {
                androidx.core.content.ContextCompat.startForegroundService(this, Intent(this, TranscriptionService::class.java).putExtra("session", session.absolutePath))
                Toast.makeText(this, "Procesando. Consulta la notificación y el estado de esta grabación.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) { Toast.makeText(this, "No se pudo iniciar. Vuelve a abrir la app y reintenta.", Toast.LENGTH_LONG).show() }
        }
        if (decision.route == TranscriptionRouter.Route.ONLINE_READY) AlertDialog.Builder(this)
            .setTitle("Transcribir con Groq").setMessage("Se enviará el audio de esta clase a Groq usando la conexión permitida. Los originales se conservan. Usa una cuenta en el plan gratuito para mantener costo obligatorio $0.00.")
            .setNegativeButton("Cancelar",null).setPositiveButton("Transcribir") { _,_ -> start() }.show()
        else start()
    }
    private fun play(file: File) {
        stopAudio()
        try { player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() } }
        catch (_: Exception) { Toast.makeText(this, "No se pudo reproducir.", Toast.LENGTH_LONG).show() }
    }
    private fun stopAudio() { try { player?.stop() } catch (_: Exception) {}; player?.release(); player = null }
    override fun onDestroy() { handler.removeCallbacks(refresh); stopAudio(); super.onDestroy() }
}
