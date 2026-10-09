package com.edwin.grabador

fun interface WhisperProgress {
    fun onProgress(percent: Int)
}

object WhisperNative {
    init { System.loadLibrary("grabador_whisper") }
    external fun transcribe(modelPath: String, audio: FloatArray, progressCallback: WhisperProgress): String
}
