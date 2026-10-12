package com.edwin.grabador

import android.app.Activity
import android.os.Bundle
import android.text.InputType
import android.view.WindowManager
import android.widget.*

class TranscriptionSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UiTheme.apply(this)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val prefs = TranscriptionRouter.settings(this)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,28,28,60) }
        root.addView(TextView(this).apply { text = "Preferencias de transcripción · 0.2"; textSize = 24f })
        val group = RadioGroup(this)
        group.addView(RadioButton(this).apply { id = 100; text = "En línea con Groq · Wi-Fi predeterminado" })
        group.addView(RadioButton(this).apply { id = 101; text = "Whisper local · experimental (máximo 3 minutos por segmento)" })
        group.check(if (prefs.getString("mode", "wifi") == "local") 101 else 100)
        root.addView(group)
        val mobile = CheckBox(this).apply {
            text = "Permitir también datos móviles para transcribir"
            isChecked = prefs.getBoolean("mobile_online", false)
        }
        root.addView(mobile)
        root.addView(TextView(this).apply {
            text = "Al pulsar Transcribir en modo en línea, el audio se envía a Groq. La grabación funciona sin internet. Conserva tu cuenta en el plan gratuito; al alcanzar un límite, la app pausa y permite continuar. Los datos móviles pueden consumir tu saldo o plan."
            textSize = 16f
        })
        val state = TextView(this).apply { textSize = 16f }
        fun keyState() { state.text = if (GroqKeyStore.read(this).isNotBlank()) "Clave guardada en este teléfono (pendiente de comprobar al transcribir)." else "Falta guardar tu clave de Groq." }
        keyState(); root.addView(state)
        val keyInput = EditText(this).apply {
            hint = "Pega aquí tu clave API de Groq"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true
            importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        root.addView(keyInput)
        root.addView(Button(this).apply {
            text = "GUARDAR PREFERENCIAS Y CLAVE"
            setOnClickListener {
                try {
                    val entered = keyInput.text.toString().trim()
                    if (entered.isNotEmpty()) {
                        require(entered.startsWith("gsk_") && entered.none { it.isWhitespace() }) { "Revisa la clave: debe empezar por gsk_ y no contener espacios." }
                        GroqKeyStore.save(this@TranscriptionSettingsActivity, entered)
                        keyInput.text.clear()
                    }
                    prefs.edit().putString("mode", if (group.checkedRadioButtonId == 101) "local" else "wifi")
                        .putBoolean("mobile_online", mobile.isChecked).apply()
                    keyState()
                    Toast.makeText(this@TranscriptionSettingsActivity, "Preferencias guardadas", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                    Toast.makeText(this@TranscriptionSettingsActivity, "No se pudo guardar. Comprueba que la clave empiece por gsk_ y no tenga espacios.", Toast.LENGTH_LONG).show()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "BORRAR CLAVE DE ESTE TELÉFONO"
            setOnClickListener { GroqKeyStore.clear(this@TranscriptionSettingsActivity); keyInput.text.clear(); keyState() }
        })
        root.addView(TextView(this).apply {
            text = "Los envíos agrupan hasta 3 segmentos, con un objetivo máximo de 24 MB (unos 45 minutos con la calidad actual). Cada bloque terminado se guarda. Si hay una interrupción, pulsa Transcribir de nuevo. La clave está cifrada y no se incluye en las copias ZIP. El resumen automático sigue pendiente."
            textSize = 16f
        })
        val modelState = TextView(this).apply { textSize = 16f; text = "Whisper local: " + if (WhisperModelManager.installed(this@TranscriptionSettingsActivity)) "modelo descargado" else "modelo pendiente" }
        root.addView(modelState)
        val download = Button(this).apply { text = "DESCARGAR MODELO LOCAL (OPCIONAL)" }
        download.setOnClickListener {
            download.isEnabled = false
            Thread {
                WhisperModelManager.download(applicationContext) { message -> runOnUiThread { modelState.text = message } }
                runOnUiThread { download.isEnabled = true }
            }.start()
        }
        root.addView(download)
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
        scroll.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(0,insets.systemWindowInsetTop,0,insets.systemWindowInsetBottom); insets }
        scroll.requestApplyInsets()
    }
}
