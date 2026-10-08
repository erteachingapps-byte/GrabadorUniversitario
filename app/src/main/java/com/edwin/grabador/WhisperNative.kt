package com.edwin.grabador

object WhisperNative {
    init { System.loadLibrary("grabador_whisper") }
    external fun transcribe(modelPath: String, audio: FloatArray): String
}
