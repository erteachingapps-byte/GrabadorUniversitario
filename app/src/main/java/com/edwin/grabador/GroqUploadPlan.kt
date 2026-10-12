package com.edwin.grabador

/** Keep headroom for container overhead; at most three original segments per request. */
object GroqUploadPlan {
    const val BUDGET = 24_000_000L
    fun batches(sizes: List<Long>): List<List<Int>> {
        require(sizes.all { it in 1..BUDGET }) { "Hay un segmento vacío o mayor de 24 MB. Conservamos el audio; debe dividirse para enviarlo." }
        val result = mutableListOf<List<Int>>()
        var current = mutableListOf<Int>()
        var bytes = 0L
        sizes.forEachIndexed { index, size ->
            if (current.size == 3 || bytes + size > BUDGET) {
                result.add(current.toList()); current = mutableListOf(); bytes = 0
            }
            current.add(index); bytes += size
        }
        if (current.isNotEmpty()) result.add(current.toList())
        return result
    }
}
object GroqLimits {
    fun message(retryAfter: String?): String {
        val seconds = retryAfter?.toDoubleOrNull()
        val wait = if (seconds != null && seconds.isFinite() && seconds > 0)
            " Espera aproximadamente ${kotlin.math.ceil(seconds / 60).toLong()} minuto(s)."
        else " Espera a que se renueve el límite de tu cuenta."
        return "Groq alcanzó su límite gratuito.$wait Después pulsa Transcribir para continuar. Los bloques terminados están guardados."
    }
}
