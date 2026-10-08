package com.edwin.grabador

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.nio.ByteOrder

/**
 * Decodes Android-supported audio (including AAC .m4a) to mono float PCM.
 * Whisper requires 16 kHz samples; resampling is performed after decoding.
 * Does not send audio to any network service.
 */
object AudioPcmDecoder {
    fun decode16kMono(file: File): FloatArray {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: error("El archivo no contiene audio")
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: error("Formato desconocido")
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()
            val chunks = ArrayList<FloatArray>()
            var total = 0
            var inputDone = false
            var outputDone = false
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            val info = MediaCodec.BufferInfo()
            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val buffer = codec.getInputBuffer(inputIndex)!!
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                when (val index = codec.dequeueOutputBuffer(info, 10000)) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val out = codec.outputFormat
                        sampleRate = out.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = out.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        val encoding = if (out.containsKey(MediaFormat.KEY_PCM_ENCODING)) out.getInteger(MediaFormat.KEY_PCM_ENCODING) else 2
                        require(encoding == 2) { "PCM no compatible: $encoding" }
                    }
                    MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    else -> if (index >= 0) {
                        if (info.size > 0) {
                            val buf = codec.getOutputBuffer(index)!!
                            buf.position(info.offset)
                            buf.limit(info.offset + info.size)
                            val shorts = buf.slice().order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val frames = shorts.remaining() / channels
                            val mono = FloatArray(frames)
                            for (i in 0 until frames) {
                                var sum = 0f
                                for (ch in 0 until channels) sum += shorts.get().toFloat() / 32768f
                                mono[i] = sum / channels
                            }
                            chunks.add(mono)
                            total += frames
                        }
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
            require(sampleRate > 0 && total > 0) { "Audio vacío" }
            val source = FloatArray(total)
            var cursor = 0
            for (chunk in chunks) {
                chunk.copyInto(source, cursor)
                cursor += chunk.size
            }
            if (sampleRate == 16000) return source
            val count = (source.size.toLong() * 16000 / sampleRate).toInt()
            return FloatArray(count) { i ->
                val pos = i.toDouble() * sampleRate / 16000
                val left = pos.toInt().coerceIn(0, source.lastIndex)
                val right = (left + 1).coerceAtMost(source.lastIndex)
                val fraction = (pos - left).toFloat()
                source[left] * (1f - fraction) + source[right] * fraction
            }
        } finally {
            try { codec?.stop() } catch (_: Exception) {}
            codec?.release()
            extractor.release()
        }
    }
}
