package com.dimitriazzarone.minianimator

import android.os.Bundle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Rect
import android.os.Environment
import androidx.compose.ui.layout.onSizeChanged
import com.squareup.gifencoder.GifEncoder
import com.squareup.gifencoder.ImageOptions
import java.io.File
import java.util.concurrent.TimeUnit
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize

private enum class BrushType {
    PENCIL,
    HARD,
    SOFT,
    MARKER
}

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val width: Float = 8f,
    val opacity: Float = 1f,
    val hardness: Float = 1f,
    val brushType: BrushType = BrushType.HARD
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MiniAnimatorScreen()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun MiniAnimatorScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var projectMessage by remember { mutableStateOf("") }

    val frames = remember {
        mutableStateListOf<MutableList<DrawStroke>>(
            mutableListOf()
        )
    }

    val frameImageUris = remember {
        mutableStateListOf<String?>(
            null
        )
    }

    var currentFrame by remember { mutableStateOf(0) }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(false) }
    var isEraser by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedWidth by remember { mutableStateOf(8f) }
    var selectedOpacity by remember { mutableStateOf(1f) }
    var selectedHardness by remember { mutableStateOf(1f) }
    var selectedBrush by remember { mutableStateOf(BrushType.HARD) }
    var backgroundOpacity by remember { mutableStateOf(1f) }
    var showReferenceImage by remember { mutableStateOf(true) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var showNewAnimationDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null && currentFrame in frameImageUris.indices) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            frameImageUris[currentFrame] = uri.toString()
            projectMessage = "Immagine importata"
        }
    }

    fun exportGif(uri: Uri) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) {
            projectMessage = "Dimensioni Canvas non disponibili"
            return
        }

        try {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Impossibile aprire il file")

            outputStream.use { output ->
                val encoder = GifEncoder(
                    output,
                    canvasSize.width,
                    canvasSize.height,
                    0
                )

                val options = ImageOptions().apply {
                    setDelay(250, TimeUnit.MILLISECONDS)
                }

                frames.forEachIndexed { frameIndex, frame ->
                    val bitmap = Bitmap.createBitmap(
                        canvasSize.width,
                        canvasSize.height,
                        Bitmap.Config.ARGB_8888
                    )

                    val canvas = AndroidCanvas(bitmap)
                    canvas.drawColor(android.graphics.Color.WHITE)

                    if (showReferenceImage) {
                        frameImageUris.getOrNull(frameIndex)?.let { uriString ->
                            try {
                                context.contentResolver
                                    .openInputStream(Uri.parse(uriString))
                                    ?.use { input ->
                                        BitmapFactory.decodeStream(input)
                                    }
                                    ?.let { background ->
                                        val backgroundPaint = Paint().apply {
                                            alpha = (
                                                255f *
                                                    backgroundOpacity.coerceIn(0f, 1f)
                                                ).toInt().coerceIn(0, 255)
                                        }

                                        canvas.drawBitmap(
                                            background,
                                            null,
                                            Rect(
                                                0,
                                                0,
                                                canvasSize.width,
                                                canvasSize.height
                                            ),
                                            backgroundPaint
                                        )
                                        background.recycle()
                                    }
                            } catch (_: Exception) {
                            }
                        }
                    }

                    frame.forEach { stroke ->
                        if (stroke.points.size >= 2) {
                            val path = AndroidPath().apply {
                                moveTo(
                                    stroke.points.first().x,
                                    stroke.points.first().y
                                )
                                stroke.points.drop(1).forEach { point ->
                                    lineTo(point.x, point.y)
                                }
                            }

                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = stroke.color.toArgb()
                                style = Paint.Style.STROKE
                                strokeWidth = stroke.width
                                strokeCap = Paint.Cap.ROUND
                                strokeJoin = Paint.Join.ROUND
                            }

                            canvas.drawPath(path, paint)
                        }
                    }

                    val pixels = IntArray(canvasSize.width * canvasSize.height)
                    bitmap.getPixels(
                        pixels,
                        0,
                        canvasSize.width,
                        0,
                        0,
                        canvasSize.width,
                        canvasSize.height
                    )

                    val rgb = Array(canvasSize.height) { y ->
                        IntArray(canvasSize.width) { x ->
                            pixels[y * canvasSize.width + x] and 0x00FFFFFF
                        }
                    }

                    encoder.addImage(rgb, options)
                    bitmap.recycle()
                }

                encoder.finishEncoding()
            }

            projectMessage = "GIF esportata correttamente"
        } catch (e: Exception) {
            projectMessage = "Errore export GIF: ${e.message ?: "errore sconosciuto"}"
        }
    }

    val gifSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/gif")
    ) { uri: Uri? ->
        if (uri != null) {
            exportGif(uri)
        } else {
            projectMessage = "Esportazione annullata"
        }
    }

    fun saveProject() {
        try {
            val root = JSONObject()
            val framesJson = JSONArray()

            frames.forEach { frame ->
                val frameJson = JSONArray()

                frame.forEach { stroke ->
                    val strokeJson = JSONObject()
                    strokeJson.put("color", stroke.color.toArgb())
                    strokeJson.put("width", stroke.width.toDouble())
                    strokeJson.put("opacity", stroke.opacity.toDouble())
                    strokeJson.put("hardness", stroke.hardness.toDouble())
                    strokeJson.put("brushType", stroke.brushType.name)

                    val pointsJson = JSONArray()
                    stroke.points.forEach { point ->
                        val pointJson = JSONArray()
                        pointJson.put(point.x.toDouble())
                        pointJson.put(point.y.toDouble())
                        pointsJson.put(pointJson)
                    }

                    strokeJson.put("points", pointsJson)
                    frameJson.put(strokeJson)
                }

                framesJson.put(frameJson)
            }

            root.put("frames", framesJson)

            val frameImagesJson = JSONArray()
            frames.indices.forEach { index ->
                frameImagesJson.put(
                    frameImageUris.getOrNull(index) ?: JSONObject.NULL
                )
            }
            root.put("frameImages", frameImagesJson)

            context.openFileOutput(
                "minianimator_project.json",
                android.content.Context.MODE_PRIVATE
            ).use { output ->
                output.write(root.toString().toByteArray())
            }

            projectMessage = "Progetto salvato"
        } catch (e: Exception) {
            projectMessage = "Errore salvataggio"
        }
    }

    fun loadProject() {
        try {
            val text = context.openFileInput("minianimator_project.json")
                .use { input -> input.readBytes().toString(Charsets.UTF_8) }

            val root = JSONObject(text)
            val framesJson = root.getJSONArray("frames")
            val loadedFrames = mutableListOf<MutableList<DrawStroke>>()
            val loadedImageUris = mutableListOf<String?>()
            val frameImagesJson = root.optJSONArray("frameImages")

            for (frameIndex in 0 until framesJson.length()) {
                val frameJson = framesJson.getJSONArray(frameIndex)
                val loadedFrame = mutableListOf<DrawStroke>()

                for (strokeIndex in 0 until frameJson.length()) {
                    val strokeJson = frameJson.getJSONObject(strokeIndex)
                    val pointsJson = strokeJson.getJSONArray("points")
                    val points = mutableListOf<Offset>()

                    for (pointIndex in 0 until pointsJson.length()) {
                        val pointJson = pointsJson.getJSONArray(pointIndex)
                        points.add(
                            Offset(
                                pointJson.getDouble(0).toFloat(),
                                pointJson.getDouble(1).toFloat()
                            )
                        )
                    }

                    loadedFrame.add(
                        DrawStroke(
                            points = points,
                            color = Color(strokeJson.getInt("color")),
                            width = strokeJson.getDouble("width").toFloat(),
                            opacity = strokeJson.optDouble("opacity", 1.0).toFloat(),
                            hardness = strokeJson.optDouble("hardness", 1.0).toFloat(),
                            brushType = try {
                                BrushType.valueOf(
                                    strokeJson.optString("brushType", "HARD")
                                )
                            } catch (_: Exception) {
                                BrushType.HARD
                            }
                        )
                    )
                }

                loadedFrames.add(loadedFrame)

                val imageUri = if (
                    frameImagesJson != null &&
                    frameIndex < frameImagesJson.length() &&
                    !frameImagesJson.isNull(frameIndex)
                ) {
                    frameImagesJson.optString(frameIndex, null)
                } else {
                    null
                }

                loadedImageUris.add(imageUri)
            }

            frames.clear()
            frameImageUris.clear()
            if (loadedFrames.isEmpty()) {
                frames.add(mutableListOf())
                frameImageUris.add(null)
            } else {
                frames.addAll(loadedFrames)
                frameImageUris.addAll(loadedImageUris)
            }

            currentFrame = 0
            activePoints = emptyList()
            isPlaying = false
            isEraser = false
            projectMessage = "Progetto caricato"
        } catch (e: Exception) {
            projectMessage = "Nessun progetto salvato"
        }
    }

    LaunchedEffect(isPlaying, frames.size) {
        while (isPlaying && frames.size > 1) {
            delay(250)
            currentFrame = (currentFrame + 1) % frames.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202124))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(2f)
                .fillMaxWidth()
                .background(Color(0xFF263238))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎬 MiniAnimator",
                color = Color.White
            )
            Button(
                onClick = {
                    if (frames.size > 1) {
                        isPlaying = !isPlaying
                    }
                }
            ) {
                Text(if (isPlaying) "■ Stop" else "▶ Play")
            }

            Button(
                onClick = {
                    if (!isPlaying && currentFrame > 0) {
                        currentFrame--
                    }
                }
            ) {
                Text("<")
            }

            Button(
                onClick = {
                    if (!isPlaying && currentFrame < frames.lastIndex) {
                        currentFrame++
                    }
                }
            ) {
                Text(">")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        frames.add(mutableListOf())
                        frameImageUris.add(null)
                        currentFrame = frames.lastIndex
                    }
                }
            ) {
                Text("+ Fotogramma")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        val copy = frames[currentFrame]
                            .map { stroke ->
                                DrawStroke(
                                    points = stroke.points.toList(),
                                    color = stroke.color,
                                    width = stroke.width,
                                    opacity = stroke.opacity,
                                    hardness = stroke.hardness,
                                    brushType = stroke.brushType
                                )
                            }
                            .toMutableList()

                        frames.add(currentFrame + 1, copy)
                        frameImageUris.add(
                            currentFrame + 1,
                            frameImageUris.getOrNull(currentFrame)
                        )
                        currentFrame += 1
                    }
                }
            ) {
                Text("⧉ Duplica")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        frames[currentFrame] = mutableListOf()
                        activePoints = emptyList()
                    }
                }
            ) {
                Text("⌫ Pulisci")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        isEraser = false
                    }
                }
            ) {
                Text(if (!isEraser) "Penna ✓" else "Penna")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        selectedBrush = when (selectedBrush) {
                            BrushType.PENCIL -> BrushType.HARD
                            BrushType.HARD -> BrushType.SOFT
                            BrushType.SOFT -> BrushType.MARKER
                            BrushType.MARKER -> BrushType.PENCIL
                        }
                        isEraser = false
                    }
                }
            ) {
                Text(
                    when (selectedBrush) {
                        BrushType.PENCIL -> "Matita"
                        BrushType.HARD -> "Duro"
                        BrushType.SOFT -> "Morbido"
                        BrushType.MARKER -> "Marker"
                    }
                )
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        isEraser = true
                    }
                }
            ) {
                Text(if (isEraser) "Gomma ✓" else "Gomma")
            }

            val paletteColors = listOf(
                Color.Black,
                Color(0xFF424242),
                Color(0xFF795548),
                Color(0xFFD32F2F),
                Color(0xFFF57C00),
                Color(0xFFFBC02D),
                Color(0xFF388E3C),
                Color(0xFF009688),
                Color(0xFF1976D2),
                Color(0xFF512DA8),
                Color(0xFFC2185B),
                Color.White
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                paletteColors.forEach { paletteColor ->
                    Button(
                        onClick = {
                            if (!isPlaying) {
                                selectedColor = paletteColor
                                isEraser = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = paletteColor
                        )
                    ) {
                        Text(
                            text = if (selectedColor == paletteColor) "✓" else " ",
                            color = if (
                                paletteColor == Color.Black ||
                                paletteColor == Color(0xFF424242) ||
                                paletteColor == Color(0xFF512DA8)
                            ) Color.White else Color.Black
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        selectedWidth = when (selectedWidth) {
                            4f -> 8f
                            8f -> 16f
                            else -> 4f
                        }
                        isEraser = false
                    }
                }
            ) {
                Text("Penna ${selectedWidth.toInt()}")
            }

            Column(
                modifier = Modifier.width(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Intensità ${(selectedOpacity * 100).toInt()}%",
                    color = Color.White
                )

                Slider(
                    value = selectedOpacity,
                    onValueChange = {
                        selectedOpacity = it
                    },
                    valueRange = 0.05f..1f,
                    enabled = !isPlaying
                )
            }

            Column(
                modifier = Modifier.width(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dimensione ${selectedWidth.toInt()}",
                    color = Color.White
                )

                Slider(
                    value = selectedWidth,
                    onValueChange = {
                        selectedWidth = it
                    },
                    valueRange = 2f..40f,
                    enabled = !isPlaying
                )
            }

            Column(
                modifier = Modifier.width(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Sfondo ${(backgroundOpacity * 100).toInt()}%",
                    color = Color.White
                )

                Slider(
                    value = backgroundOpacity,
                    onValueChange = {
                        backgroundOpacity = it
                        showReferenceImage = it > 0f
                    },
                    valueRange = 0f..1f,
                    enabled = !isPlaying
                )
            }

            Column(
                modifier = Modifier.width(180.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Durezza ${(selectedHardness * 100).toInt()}%",
                    color = Color.White
                )

                Slider(
                    value = selectedHardness,
                    onValueChange = {
                        selectedHardness = it
                    },
                    valueRange = 0f..1f,
                    enabled = !isPlaying
                )
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        imagePicker.launch(arrayOf("image/*"))
                    }
                }
            ) {
                Text("📷 Importa")
            }

        Button(
                onClick = {
                    if (!isPlaying) {
                        showReferenceImage = !showReferenceImage
                    }
                }
            ) {
                Text(if (showReferenceImage) "Sfondo ON" else "Sfondo OFF")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        saveProject()
                    }
                }
            ) {
                Text("💾 Salva")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        loadProject()
                    }
                }
            ) {
                Text("📂 Carica")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        gifSaver.launch(
                            "MiniAnimator_${System.currentTimeMillis()}.gif"
                        )
                    }
                }
            ) {
                Text("🎞 GIF")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        showNewAnimationDialog = true
                    }
                }
            ) {
                Text("＋ Nuova")
            }
        }

        Text(
            text = "Fotogramma ${currentFrame + 1} / ${frames.size}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .zIndex(2f)
                .background(Color(0xAA202124))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (projectMessage.isNotBlank()) {
            Text(
                text = projectMessage,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(2f)
                    .background(Color(0xAA202124))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        if (showNewAnimationDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = {
                    showNewAnimationDialog = false
                },
                title = {
                    Text("Nuova animazione")
                },
                text = {
                    Text("Vuoi cancellare tutti i fotogrammi e iniziare una nuova animazione?")
                },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            frames.clear()
                            frameImageUris.clear()
                            frames.add(mutableListOf())
                            frameImageUris.add(null)
                            currentFrame = 0
                            activePoints = emptyList()
                            isPlaying = false
                            isEraser = false
                            showNewAnimationDialog = false
                        }
                    ) {
                        Text("＋ Nuova")
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            showNewAnimationDialog = false
                        }
                    ) {
                        Text("Annulla")
                    }
                }
            )
        }

        val currentImageUri = frameImageUris.getOrNull(currentFrame)
        val currentBackgroundImage = remember(currentImageUri) {
            currentImageUri?.let { uriString ->
                try {
                    context.contentResolver.openInputStream(
                        android.net.Uri.parse(uriString)
                    )?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { canvasSize = it }
                .background(Color.White)
                .border(2.dp, Color.Gray)
                .pointerInput(currentFrame, isPlaying) {
                    if (!isPlaying) {
                        detectDragGestures(
                            onDragStart = { position ->
                                activePoints = listOf(position)
                            },
                            onDrag = { change, _ ->
                                activePoints = activePoints + change.position
                            },
                            onDragEnd = {
                                if (activePoints.size > 1) {
                                    frames[currentFrame] =
                                        (frames[currentFrame] + DrawStroke(
                                    points = activePoints,
                                    color = if (isEraser) Color.White else selectedColor,
                                    width = if (isEraser) 30f else selectedWidth,
                                    opacity = if (isEraser) 1f else selectedOpacity,
                                    hardness = if (isEraser) 1f else selectedHardness,
                                    brushType = if (isEraser) BrushType.HARD else selectedBrush
                                ))
                                            .toMutableList()
                                }
                                activePoints = emptyList()
                            },
                            onDragCancel = {
                                activePoints = emptyList()
                            }
                        )
                    }
                }
        ) {
            fun drawStroke(stroke: DrawStroke) {
                if (stroke.points.size < 2) return

                val path = Path().apply {
                    moveTo(stroke.points.first().x, stroke.points.first().y)
                    stroke.points.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }

                when (stroke.brushType) {

                    BrushType.PENCIL -> {
                        drawPath(
                            path = path,
                            color = stroke.color.copy(
                                alpha = (stroke.opacity * 0.85f).coerceIn(0f, 1f)
                            ),
                            style = Stroke(
                                width = (stroke.width * 0.55f).coerceAtLeast(1f),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    BrushType.HARD -> {
                        drawPath(
                            path = path,
                            color = stroke.color.copy(
                                alpha = stroke.opacity.coerceIn(0f, 1f)
                            ),
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    BrushType.SOFT -> {
                        val hardness = stroke.hardness.coerceIn(0f, 1f)

                        drawPath(
                            path = path,
                            color = stroke.color.copy(
                                alpha = (stroke.opacity * 0.18f * (1f - hardness * 0.5f))
                                    .coerceIn(0f, 1f)
                            ),
                            style = Stroke(
                                width = stroke.width * 2.2f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        drawPath(
                            path = path,
                            color = stroke.color.copy(
                                alpha = (stroke.opacity * 0.55f).coerceIn(0f, 1f)
                            ),
                            style = Stroke(
                                width = stroke.width,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    BrushType.MARKER -> {
                        drawPath(
                            path = path,
                            color = stroke.color.copy(
                                alpha = (stroke.opacity * 0.55f).coerceIn(0f, 1f)
                            ),
                            style = Stroke(
                                width = stroke.width * 1.4f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            if (showReferenceImage) {
                currentBackgroundImage?.let { image ->
                    drawImage(
                        image = image,
                        dstSize = IntSize(
                            size.width.toInt(),
                            size.height.toInt()
                        ),
                        alpha = backgroundOpacity.coerceIn(0f, 1f)
                    )
                }
            }

            frames[currentFrame].forEach { stroke ->
                drawStroke(stroke)
            }

            if (activePoints.size > 1) {
                drawStroke(
                    DrawStroke(
                        points = activePoints,
                        color = if (isEraser) Color.White else selectedColor,
                        width = if (isEraser) 30f else selectedWidth,
                        opacity = if (isEraser) 1f else selectedOpacity,
                        hardness = if (isEraser) 1f else selectedHardness,
                        brushType = if (isEraser) BrushType.HARD else selectedBrush
                    )
                )
            }
        }
    }
}
