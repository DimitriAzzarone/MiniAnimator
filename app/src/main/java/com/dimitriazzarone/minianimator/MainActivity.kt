package com.dimitriazzarone.minianimator

import android.annotation.SuppressLint
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private var chooser: ValueCallback<Array<Uri>>? = null

    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val cb = chooser
        chooser = null
        cb?.onReceiveValue(if (uri == null) null else arrayOf(uri))
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                chooser?.onReceiveValue(null)
                chooser = filePathCallback
                picker.launch("image/*")
                return true
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }

    inner class Bridge {
        @JavascriptInterface
        fun saveBase64(filename: String, mime: String, data: String) {
            runOnUiThread {
                try {
                    val bytes = Base64.decode(data, Base64.DEFAULT)
                    if (Build.VERSION.SDK_INT >= 29) {
                        val v = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, filename)
                            put(MediaStore.Downloads.MIME_TYPE, mime)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                            ?: error("Impossibile creare il file")
                        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                        v.clear()
                        v.put(MediaStore.Downloads.IS_PENDING, 0)
                        contentResolver.update(uri, v, null, null)
                    } else {
                        val dir = getExternalFilesDir(null) ?: filesDir
                        FileOutputStream(File(dir, filename)).use { it.write(bytes) }
                    }
                    Toast.makeText(this@MainActivity, "Salvato: $filename", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Errore: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
