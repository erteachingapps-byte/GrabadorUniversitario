package com.edwin.grabador

import android.app.*
import android.content.Intent
import android.os.*
import java.io.File

class TranscriptionService : Service() {
    companion object { const val CHANNEL = "whisper_processing"; const val ID = 81; @Volatile var running = false }
    private var session: File? = null
    override fun onBind(intent: Intent?) = null
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Transcripción de clases", NotificationManager.IMPORTANCE_LOW))
    }
    private fun notification(title: String, body: String, ongoing: Boolean, percent: Int? = null): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, RecordingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = Notification.Builder(this, CHANNEL).setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(title).setContentText(body).setStyle(Notification.BigTextStyle().bigText(body))
            .setContentIntent(open).setOngoing(ongoing)
        if (ongoing) builder.setProgress(100, percent ?: 0, percent == null)
        return builder.build()
    }
    private fun update(body: String, percent: Int? = null) {
        session?.let { TranscriptStore.write(File(it, "estado-transcripcion.txt"), body) }
        getSystemService(NotificationManager::class.java).notify(ID, notification("Transcribiendo clase", body, true, percent))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (running) return START_NOT_STICKY
        startForeground(ID, notification("Transcribiendo clase", "Preparando grabación...", true))
        running = true
        val path = intent?.getStringExtra("session")
        val lock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:transcription").apply { setReferenceCounted(false) }
        Thread {
            try {
                require(path != null) { "Falta elegir una grabación." }
                val folder = File(path)
                val root = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Clases")
                require(folder.canonicalPath.startsWith(root.canonicalPath + File.separator) && folder.isDirectory) { "Grabación no válida." }
                session = folder
                require(!RecordingService.recording) { "Detén la grabación antes de transcribir." }
                val audios = folder.listFiles()?.filter { it.extension.equals("m4a", true) }?.sortedBy { it.name }.orEmpty()
                require(audios.isNotEmpty()) { "No hay audio en esta grabación." }
                val route = TranscriptionRouter.decide(this)
                require(route.route == TranscriptionRouter.Route.LOCAL_READY || route.route == TranscriptionRouter.Route.ONLINE_READY) { route.explanation }
                File(folder, "transcripcion-error.txt").delete()
                lock.acquire(30 * 60 * 1000L)
                if (route.route == TranscriptionRouter.Route.ONLINE_READY) online(folder, audios) else local(folder, audios)
                File(folder, "transcripcion-error.txt").delete()
                File(folder, "transcripcion-parcial.txt").delete()
                TranscriptStore.write(File(folder, "estado-transcripcion.txt"), "Transcripción terminada. Abre Ver transcripción.")
                getSystemService(NotificationManager::class.java).notify(ID + 1,
                    notification("Transcripción terminada", "Abre Mis grabaciones para leer o exportar el texto.", false))
            } catch (e: Exception) {
                val message = e.message ?: "No se pudo transcribir. Puedes reintentar."
                session?.let {
                    TranscriptStore.write(File(it, "transcripcion-error.txt"), message)
                    TranscriptStore.write(File(it, "estado-transcripcion.txt"), "Procesamiento detenido: $message")
                }
                getSystemService(NotificationManager::class.java).notify(ID + 1, notification("Transcripción detenida", message, false))
            } finally {
                if (lock.isHeld) lock.release()
                session = null; running = false
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf()
            }
        }.start()
        return START_NOT_STICKY
    }
    private fun online(folder: File, audios: List<File>) {
        val batches = GroqUploadPlan.batches(audios.map { it.length() }).map { indexes -> indexes.map { audios[it] } }
        val output = StringBuilder()
        batches.forEachIndexed { index, files ->
            update("Preparando bloque ${index + 1}/${batches.size} · $index bloque(s) completado(s)", index * 100 / batches.size)
            val hash = TranscriptStore.fingerprint(files)
            val saved = TranscriptStore.load(folder, hash)
            val text = if (saved != null) saved else {
                val route = TranscriptionRouter.decide(this)
                if (route.route != TranscriptionRouter.Route.ONLINE_READY) throw TranscriptionPaused(route.explanation)
                val network = TranscriptionRouter.onlineNetwork(this) ?: throw TranscriptionPaused("Conecta una red permitida y pulsa Transcribir para continuar.")
                val key = GroqKeyStore.read(this)
                val temporary = if (files.size > 1) File.createTempFile("groq-block-", ".m4a", cacheDir) else null
                try {
                    if (temporary != null) AudioBlockMuxer.join(files, temporary)
                    update("Enviando bloque ${index + 1}/${batches.size} · $index completado(s)", index * 100 / batches.size)
                    GroqTranscriber.transcribe(temporary ?: files.first(), key, network) {
                        update("Groq procesa el bloque ${index + 1}/${batches.size} · $index completado(s)", index * 100 / batches.size)
                    }.also { TranscriptStore.save(folder, hash, it) }
                } finally { temporary?.delete() }
            }
            output.append("Bloque ${index + 1}: ${files.first().name} — ${files.last().name}\n").append(text).append("\n\n")
            TranscriptStore.write(File(folder, "transcripcion-parcial.txt"), output.toString())
            update("${index + 1}/${batches.size} bloque(s) completado(s)", (index + 1) * 100 / batches.size)
        }
        TranscriptStore.write(File(folder, "transcripcion.txt"), output.toString())
    }
    private fun local(folder: File, audios: List<File>) {
        val output = StringBuilder()
        audios.forEachIndexed { index, audio ->
            update("Preparando audio local ${index + 1}/${audios.size}")
            val samples = AudioPcmDecoder.decode16kMono(audio)
            require(samples.size <= 16000 * 180) { "El modo local admite hasta 3 minutos por segmento. Usa Groq en Preferencias para clases completas." }
            val result = WhisperNative.transcribe(WhisperModelManager.model(this).absolutePath, samples, WhisperProgress { percent ->
                if (percent % 5 == 0 || percent == 100) update("Whisper local · segmento ${index + 1}/${audios.size}", (index * 100 + percent.coerceIn(0,100)) / audios.size)
            })
            require(!result.startsWith("ERROR:")) { result }
            output.append("Segmento ${index + 1}: ${audio.name}\n$result\n")
        }
        TranscriptStore.write(File(folder, "transcripcion.txt"), output.toString())
    }
}
