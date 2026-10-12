package com.edwin.grabador

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.*
import java.io.File

class TranscriptActivity : Activity() {
    private var transcript: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); UiTheme.apply(this)
        val file = intent.getStringExtra("file")?.let { File(it) }
        val rootDir = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Clases")
        if (file == null || !file.isFile || !file.canonicalPath.startsWith(rootDir.canonicalPath + File.separator) ||
            file.name !in listOf("transcripcion.txt", "transcripcion-parcial.txt")) { finish(); return }
        transcript = file
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28,28,28,28) }
        root.addView(TextView(this).apply { text = if (file.name.contains("parcial")) "Transcripción parcial" else "Transcripción completa"; textSize = 25f })
        root.addView(Button(this).apply {
            text = "EXPORTAR TEXTO (.TXT)"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"
                    putExtra(Intent.EXTRA_TITLE, "Transcripcion-${file.parentFile?.name}.txt")
                }, 301)
            }
        })
        val textView = TextView(this).apply { text = file.readText(); textSize = 19f; setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(textView) }, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.setOnApplyWindowInsetsListener { view, insets -> view.setPadding(28,28 + insets.systemWindowInsetTop,28,28 + insets.systemWindowInsetBottom); insets }
        root.requestApplyInsets()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 301 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.openOutputStream(uri, "w")!!.use { out -> transcript!!.inputStream().use { it.copyTo(out) } }
            Toast.makeText(this, "Texto exportado", Toast.LENGTH_LONG).show()
        } catch (_: Exception) { Toast.makeText(this, "No se pudo exportar el texto", Toast.LENGTH_LONG).show() }
    }
}
