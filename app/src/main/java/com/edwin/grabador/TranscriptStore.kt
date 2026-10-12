package com.edwin.grabador

import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import org.json.JSONObject

object TranscriptStore {
    fun fingerprint(audios: List<File>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        audios.forEach { audio ->
            // Boundaries and names must also match before reusing a block.
            digest.update((audio.name + ":" + audio.length() + "\n").toByteArray(Charsets.UTF_8))
            audio.inputStream().use { input ->
                val buffer = ByteArray(65536)
                while (true) { val count = input.read(buffer); if (count < 0) break; digest.update(buffer, 0, count) }
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }
    private fun file(session: File, hash: String) = File(File(session, "transcripcion-bloques"), "$hash.json")
    fun load(session: File, hash: String): String? = try {
        val result = JSONObject(AtomicFile(file(session, hash)).readFully().toString(Charsets.UTF_8))
        if (result.getString("sha256") == hash && result.getString("model") == GroqTranscriber.MODEL)
            result.getString("text") else null
    } catch (_: Exception) { null }
    fun save(session: File, hash: String, text: String) {
        val target = file(session, hash)
        target.parentFile!!.mkdirs()
        write(target, JSONObject().put("sha256", hash).put("model", GroqTranscriber.MODEL).put("text", text).toString())
    }
    fun write(file: File, text: String) {
        val target = AtomicFile(file)
        val stream = target.startWrite()
        try { stream.write(text.toByteArray(Charsets.UTF_8)); target.finishWrite(stream) }
        catch (e: Exception) { target.failWrite(stream); throw e }
    }
}
