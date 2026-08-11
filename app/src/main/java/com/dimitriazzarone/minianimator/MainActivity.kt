package com.dimitriazzarone.minianimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private data class DrawStroke(
    val points: List<Offset>
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
    val frames = remember {
        mutableStateListOf<MutableList<DrawStroke>>(
            mutableListOf()
        )
    }

    var currentFrame by remember { mutableStateOf(0) }
    var activePoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(isPlaying, frames.size) {
        while (isPlaying && frames.size > 1) {
            delay(250)
            currentFrame = (currentFrame + 1) % frames.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF202124))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (frames.size > 1) {
                        isPlaying = !isPlaying
                    }
                }
            ) {
                Text(if (isPlaying) "Stop" else "▶ Play")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isPlaying && currentFrame > 0) {
                        currentFrame--
                    }
                }
            ) {
                Text("◀ Indietro")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isPlaying && currentFrame < frames.lastIndex) {
                        currentFrame++
                    }
                }
            ) {
                Text("Avanti ▶")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isPlaying) {
                        frames.add(mutableListOf())
                        currentFrame = frames.lastIndex
                    }
                }
            ) {
                Text("+ Nuovo")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isPlaying) {
                        val copy = frames[currentFrame]
                            .map { stroke ->
                                DrawStroke(stroke.points.toList())
                            }
                            .toMutableList()

                        frames.add(currentFrame + 1, copy)
                        currentFrame += 1
                    }
                }
            ) {
                Text("⧉ Duplica")
            }

            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    if (!isPlaying) {
                        frames[currentFrame] = mutableListOf()
                        activePoints = emptyList()
                    }
                }
            ) {
                Text("Pulisci")
            }
        }

        Text(
            text = "Fotogramma ${currentFrame + 1} / ${frames.size}",
            color = Color.White,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                                        (frames[currentFrame] + DrawStroke(activePoints))
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
            fun drawStroke(points: List<Offset>) {
                if (points.size < 2) return

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { point ->
                        lineTo(point.x, point.y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(
                        width = 8f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            frames[currentFrame].forEach { stroke ->
                drawStroke(stroke.points)
            }

            drawStroke(activePoints)
        }
    }
}
