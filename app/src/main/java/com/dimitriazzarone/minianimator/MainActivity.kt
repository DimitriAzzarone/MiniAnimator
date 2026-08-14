package com.dimitriazzarone.minianimator

import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color = Color.Black,
    val width: Float = 8f
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

    var currentFrame by remember { mutableStateOf(0) }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(false) }
    var isEraser by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var selectedWidth by remember { mutableStateOf(8f) }
    var showNewAnimationDialog by remember { mutableStateOf(false) }

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
                            width = strokeJson.getDouble("width").toFloat()
                        )
                    )
                }

                loadedFrames.add(loadedFrame)
            }

            frames.clear()
            if (loadedFrames.isEmpty()) {
                frames.add(mutableListOf())
            } else {
                frames.addAll(loadedFrames)
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
                .background(Color(0xDD202124))
                .horizontalScroll(rememberScrollState())
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    if (frames.size > 1) {
                        isPlaying = !isPlaying
                    }
                }
            ) {
                Text(if (isPlaying) "Stop" else "Play")
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
                        currentFrame = frames.lastIndex
                    }
                }
            ) {
                Text("+ Frame")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        val copy = frames[currentFrame]
                            .map { stroke ->
                                DrawStroke(
                                    points = stroke.points.toList(),
                                    color = stroke.color,
                                    width = stroke.width
                                )
                            }
                            .toMutableList()

                        frames.add(currentFrame + 1, copy)
                        currentFrame += 1
                    }
                }
            ) {
                Text("Duplica")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        frames[currentFrame] = mutableListOf()
                        activePoints = emptyList()
                    }
                }
            ) {
                Text("Pulisci")
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
                        isEraser = true
                    }
                }
            ) {
                Text(if (isEraser) "Gomma ✓" else "Gomma")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        selectedColor = when (selectedColor) {
                            Color.Black -> Color.Red
                            Color.Red -> Color.Blue
                            Color.Blue -> Color.Green
                            else -> Color.Black
                        }
                        isEraser = false
                    }
                }
            ) {
                Text(
                    when (selectedColor) {
                        Color.Red -> "Rosso"
                        Color.Blue -> "Blu"
                        Color.Green -> "Verde"
                        else -> "Nero"
                    }
                )
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

            Button(
                onClick = {
                    if (!isPlaying) {
                        saveProject()
                    }
                }
            ) {
                Text("Salva")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        loadProject()
                    }
                }
            ) {
                Text("Carica")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        showNewAnimationDialog = true
                    }
                }
            ) {
                Text("Nuova")
            }
        }

        Text(
            text = "Fotogramma ${currentFrame + 1} / ${frames.size}",
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopStart)
                .zIndex(2f)
                .background(Color(0xAA202124))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        if (projectMessage.isNotBlank()) {
            Text(
                text = projectMessage,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
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
                            frames.add(mutableListOf())
                            currentFrame = 0
                            activePoints = emptyList()
                            isPlaying = false
                            isEraser = false
                            showNewAnimationDialog = false
                        }
                    ) {
                        Text("Nuova")
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

        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
                                    width = if (isEraser) 30f else selectedWidth
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

                drawPath(
                    path = path,
                    color = stroke.color,
                    style = Stroke(
                        width = stroke.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            frames[currentFrame].forEach { stroke ->
                drawStroke(stroke)
            }

            if (activePoints.size > 1) {
                drawStroke(
                    DrawStroke(
                        points = activePoints,
                        color = if (isEraser) Color.White else selectedColor,
                        width = if (isEraser) 30f else selectedWidth
                    )
                )
            }
        }
    }
}
