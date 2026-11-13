package com.dimitriazzarone.minianimator

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.webkit.WebViewAssetLoader
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.roundToLong

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var chooser: ValueCallback<Array<Uri>>? = null
    private val selectedContent = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val worker = Executors.newSingleThreadExecutor()
    private val exporting = AtomicBoolean(false)
    private var exportFile: File? = null
    @Volatile private var closed = false
    private val origin = "https://appassets.androidplatform.net/assets/"

    private val picker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val cb = chooser; chooser = null
        if (uri != null) selectedContent.add(uri.toString())
        cb?.onReceiveValue(uri?.let { arrayOf(it) })
    }
    private val savePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val file = exportFile; exportFile = null
        val uri = if (result.resultCode == Activity.RESULT_OK) result.data?.data else null
        if (file == null || uri == null) {
            file?.delete(); exporting.set(false)
            exportResult(false, "Esportazione annullata: il progetto resta nell'app.")
        } else worker.execute {
            try {
                contentResolver.openOutputStream(uri, "w")?.use { output ->
                    file.inputStream().use { input -> check(input.copyTo(output) == file.length()) }
                } ?: error("Destinazione non disponibile")
                exportResult(true, "File esportato correttamente")
            } catch (e: Exception) {
                exportResult(false, "Salvataggio non riuscito: ${e.message ?: "errore"}")
            } finally { file.delete(); exporting.set(false) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(22, 59, 105)
        window.navigationBarColor = Color.rgb(22, 59, 105)
        web = WebView(this)
        web.setBackgroundColor(Color.rgb(22, 59, 105))
        val container = android.widget.FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(22, 59, 105))
            addView(web, android.widget.FrameLayout.LayoutParams(-1, -1))
            setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets.consumeSystemWindowInsets()
            }
        }
        setContentView(container)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = true
        web.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (request.url.scheme in setOf("blob", "data")) return null
                if (request.url.scheme == "content" && selectedContent.contains(request.url.toString())) {
                    return try { WebResourceResponse(contentResolver.getType(request.url) ?: "application/octet-stream", null, contentResolver.openInputStream(request.url)) } catch (_: Exception) { null }
                }
                if (request.url.scheme == "https" && request.url.host == "appassets.androidplatform.net") {
                    loader.shouldInterceptRequest(request.url)?.let { return it }
                }
                return WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", emptyMap(), ByteArrayInputStream(byteArrayOf()))
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                !request.url.toString().startsWith(origin)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                chooser?.onReceiveValue(null); chooser = callback
                val imageTypes = params?.acceptTypes?.flatMap { it.split(",") }?.map { it.trim() }?.filter { it.startsWith("image/") }?.distinct().orEmpty()
                picker.launch(if (imageTypes.isNotEmpty()) imageTypes.toTypedArray() else arrayOf("application/json", "text/plain", "application/octet-stream"))
                return true
            }
            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                android.app.AlertDialog.Builder(this@MainActivity).setMessage(message)
                    .setPositiveButton("OK") { _, _ -> result.confirm() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult): Boolean {
                android.app.AlertDialog.Builder(this@MainActivity).setMessage(message)
                    .setPositiveButton("Conferma") { _, _ -> result.confirm() }
                    .setNegativeButton("Annulla") { _, _ -> result.cancel() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
            override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult): Boolean {
                val input = android.widget.EditText(this@MainActivity).apply { setText(defaultValue); selectAll() }
                android.app.AlertDialog.Builder(this@MainActivity).setTitle(message).setView(input)
                    .setPositiveButton("OK") { _, _ -> result.confirm(input.text.toString()) }
                    .setNegativeButton("Annulla") { _, _ -> result.cancel() }.setOnCancelListener { result.cancel() }.show()
                return true
            }
        }
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl(origin + "index.html")
    }
    private fun exportResult(ok: Boolean, message: String) {
        runOnUiThread {
            if (!closed) web.evaluateJavascript("window.onNativeExport&&window.onNativeExport($ok,${JSONObject.quote(message)});", null)
        }
    }
    private fun progress(message: String) {
        runOnUiThread { if (!closed) web.evaluateJavascript("window.onNativeProgress&&window.onNativeProgress(${JSONObject.quote(message)});", null) }
    }
    private fun prepare(filename: String, mime: String, job: (File) -> Unit) {
        runOnUiThread {
            if (closed || !web.url.orEmpty().startsWith(origin)) return@runOnUiThread
            if (!exporting.compareAndSet(false, true)) { exportResult(false, "Un'esportazione è già in corso"); return@runOnUiThread }
            worker.execute {
                val file = File.createTempFile("mini-export-", ".tmp", cacheDir)
                try {
                    job(file)
                    runOnUiThread {
                        if (closed) { file.delete(); exporting.set(false) }
                        else {
                            exportFile = file
                            val safe = filename.replace(Regex("[\\\\/\\p{Cntrl}]"), "_").take(100)
                            savePicker.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE); type = mime
                                putExtra(Intent.EXTRA_TITLE, safe)
                            })
                        }
                    }
                } catch (e: Exception) { file.delete(); exporting.set(false); exportResult(false, "Esportazione: ${e.message ?: "errore"}") }
            }
        }
    }
    inner class Bridge {
        @JavascriptInterface fun saveProject(filename: String, raw: String) {
            if (raw.length > 60000000) { exportResult(false, "Progetto troppo grande"); return }
            prepare(filename, "application/json") { file ->
                val data = JSONObject(raw)
                check(data.optInt("version") == 4 && data.getJSONArray("frames").length() in 1..240)
                file.writeText(raw, Charsets.UTF_8)
            }
        }
        @JavascriptInterface fun saveBase64(filename: String, mime: String, data: String) {
            if (mime !in setOf("image/png", "image/jpeg", "image/bmp", "image/svg+xml") || data.length > 40000000) { exportResult(false, "Formato o dimensione del file non valido"); return }
            prepare(filename, mime) { it.writeBytes(Base64.decode(data, Base64.DEFAULT)) }
        }
        @JavascriptInterface fun exportGif(filename: String, raw: String) {
            if (raw.length > 60000000) { exportResult(false, "Animazione troppo grande"); return }
            prepare(filename, "image/gif") { file ->
                val payload = JSONObject(raw)
                val fps = payload.getInt("fps").coerceIn(1, 30)
                val frames = payload.getJSONArray("frames")
                check(frames.length() in 1..240) { "Numero di fotogrammi non valido" }
                file.outputStream().use { output ->
                    var encoder: GifEncoder? = null
                    var width = 0; var height = 0
                    for (i in 0 until frames.length()) {
                        val frame = frames.getJSONObject(i)
                        val data = Base64.decode(frame.getString("png"), Base64.DEFAULT)
                        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
                        check(bounds.outWidth in 1..640 && bounds.outHeight in 1..640) { "Dimensioni GIF non valide" }
                        check(bounds.outWidth.toLong() * bounds.outHeight * frames.length() <= 40000000) { "GIF troppo grande" }
                        val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size) ?: error("Fotogramma non leggibile")
                        try {
                            if (encoder == null) {
                                width = bitmap.width; height = bitmap.height
                                encoder = GifEncoder(output, width, height, 0)
                            }
                            check(bitmap.width == width && bitmap.height == height)
                            val pixels = IntArray(width * height)
                            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                            val rgb = Array(height) { y -> IntArray(width) { x -> pixels[y * width + x] and 0x00ffffff } }
                            val options = ImageOptions().apply {
                                val duration = frame.optLong("durationMs", 0)
                                setDelay(if (duration > 0) duration.coerceIn(10L, 655350L) else (1000.0 * frame.optInt("hold", 1).coerceIn(1, 24) / fps).roundToLong(), TimeUnit.MILLISECONDS)
                            }
                            encoder.addImage(rgb, options)
                            progress("Codifico GIF: ${i + 1} / ${frames.length()}")
                        } finally { bitmap.recycle() }
                    }
                    encoder?.finishEncoding()
                }
            }
        }
    }
    override fun onPause() {
        if (::web.isInitialized) web.evaluateJavascript("if(window.autosave)autosave();if(window.stopPlayback)stopPlayback();", null)
        super.onPause()
    }
    override fun onDestroy() {
        closed = true
        chooser?.onReceiveValue(null); chooser = null
        exportFile?.delete(); exportFile = null
        worker.shutdown()
        if (::web.isInitialized) { web.removeJavascriptInterface("Android"); web.destroy() }
        super.onDestroy()
    }
}
