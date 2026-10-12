package com.edwin.grabador

import android.net.Network
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import org.json.JSONObject

class TranscriptionPaused(message: String) : IOException(message)

object GroqTranscriber {
    const val MODEL = "whisper-large-v3-turbo"
    fun transcribe(audio: File, apiKey: String, network: Network, waiting: () -> Unit): String {
        require(audio.length() in 1..25_000_000L) {
            "El bloque supera 25 MB o está vacío. Conservamos el audio; necesita dividirse antes de enviarlo."
        }
        val boundary = "Grabador" + UUID.randomUUID().toString()
        fun field(name: String, value: String) = "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n"
        val header = (field("model", MODEL) + field("response_format", "json") + field("temperature", "0") +
            "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"audio.m4a\"\r\nContent-Type: audio/mp4\r\n\r\n").toByteArray(Charsets.UTF_8)
        val footer = "\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8)
        val connection = network.openConnection(URL("https://api.groq.com/openai/v1/audio/transcriptions")) as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 20_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            connection.setFixedLengthStreamingMode(header.size.toLong() + audio.length() + footer.size)
            connection.outputStream.use { out ->
                out.write(header); audio.inputStream().use { it.copyTo(out) }; out.write(footer)
            }
            waiting()
            when (val code = connection.responseCode) {
                200 -> {
                    val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val json = JSONObject(response)
                    require(json.has("text") && json.get("text") is String) { "Groq no devolvió una transcripción válida. Puedes reintentar." }
                    return json.getString("text")
                }
                401, 403 -> throw TranscriptionPaused("Groq no aceptó la clave o los permisos. Revisa tu clave y cuenta en Preferencias; después reintenta.")
                429 -> throw TranscriptionPaused(GroqLimits.message(connection.getHeaderField("retry-after")))
                413 -> throw TranscriptionPaused("Groq rechazó el tamaño del bloque. Conservamos el audio para dividirlo.")
                else -> throw TranscriptionPaused("Groq respondió con código $code. Reintenta más tarde; los bloques terminados están guardados.")
            }
        } catch (e: TranscriptionPaused) { throw e }
        catch (_: IOException) { throw TranscriptionPaused("La conexión se interrumpió o tardó demasiado. Comprueba internet y pulsa Transcribir para continuar desde los bloques guardados.") }
        finally { connection.disconnect() }
    }
}
