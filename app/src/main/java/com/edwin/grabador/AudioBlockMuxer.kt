package com.edwin.grabador

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.nio.ByteBuffer

/** Join encoded AAC packets without decoding, recompressing or changing originals. */
object AudioBlockMuxer {
    fun join(audios: List<File>, destination: File) {
        val muxer = MediaMuxer(destination.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var started = false
        try {
            var outputTrack = -1
            var firstFormat: MediaFormat? = null
            var offsetUs = 0L
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()
            audios.forEach { audio ->
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(audio.absolutePath)
                    val track = (0 until extractor.trackCount).firstOrNull {
                        extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME) == "audio/mp4a-latm"
                    } ?: error("El bloque requiere audio AAC. El archivo original está conservado.")
                    val format = extractor.getTrackFormat(track)
                    if (firstFormat == null) {
                        firstFormat = format
                        outputTrack = muxer.addTrack(format); muxer.start(); started = true
                    } else {
                        val previous = firstFormat!!
                        require(previous.getInteger(MediaFormat.KEY_SAMPLE_RATE) == format.getInteger(MediaFormat.KEY_SAMPLE_RATE) &&
                            previous.getInteger(MediaFormat.KEY_CHANNEL_COUNT) == format.getInteger(MediaFormat.KEY_CHANNEL_COUNT) &&
                            previous.getByteBuffer("csd-0") == format.getByteBuffer("csd-0")) {
                            "Los segmentos tienen formatos diferentes y no pueden agruparse. Los originales están conservados."
                        }
                    }
                    extractor.selectTrack(track)
                    val firstUs = extractor.sampleTime
                    require(firstUs >= 0) { "El segmento está vacío." }
                    var lastUs = 0L
                    var packets = 0
                    val frameUs = 1024L * 1_000_000 / format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    while (true) {
                        buffer.clear()
                        val size = extractor.readSampleData(buffer, 0)
                        if (size < 0) break
                        lastUs = extractor.sampleTime - firstUs
                        info.set(0, size, offsetUs + lastUs, 0)
                        muxer.writeSampleData(outputTrack, buffer, info)
                        packets++
                        if (!extractor.advance()) break
                    }
                    require(packets > 0) { "El segmento no tiene audio." }
                    offsetUs += lastUs + frameUs
                } finally { extractor.release() }
            }
            muxer.stop(); started = false
        } finally {
            if (started) try { muxer.stop() } catch (_: Exception) {}
            muxer.release()
        }
    }
}
