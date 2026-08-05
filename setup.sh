#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

ROOT="$HOME/MiniAnimator"
SRC="$ROOT/app/src/main/java/com/dimitriazzarone/minianimator"

mkdir -p "$SRC"
mkdir -p "$ROOT/app/src/main/res/values"
mkdir -p "$ROOT/.github/workflows"

cp "$HOME/MetaLab/build.gradle.kts" "$ROOT/build.gradle.kts"
cp "$HOME/MetaLab/gradle.properties" "$ROOT/gradle.properties"

cat > "$ROOT/settings.gradle.kts" <<'EOF'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MiniAnimator"
include(":app")
EOF

cat > "$ROOT/app/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dimitriazzarone.minianimator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dimitriazzarone.minianimator"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
EOF

cat > "$ROOT/app/src/main/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:label="MiniAnimator"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">

        <activity
            android:name=".MainActivity"
            android:exported="true">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

        </activity>
    </application>

</manifest>
EOF

cat > "$ROOT/app/src/main/res/values/styles.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:fontFamily">sans</item>
        <item name="android:statusBarColor">#202124</item>
        <item name="android:navigationBarColor">#202124</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
EOF

cat > "$SRC/MainActivity.kt" <<'EOF'
package com.dimitriazzarone.minianimator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
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
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    if (!isPlaying && currentFrame > 0) {
                        currentFrame--
                    }
                }
            ) {
                Text("Indietro")
            }

            Button(
                onClick = {
                    if (!isPlaying && currentFrame < frames.lastIndex) {
                        currentFrame++
                    }
                }
            ) {
                Text("Avanti")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        frames.add(mutableListOf())
                        currentFrame = frames.lastIndex
                    }
                }
            ) {
                Text("Nuovo")
            }

            Button(
                onClick = {
                    if (!isPlaying) {
                        frames.add(
                            currentFrame + 1,
                            frames[currentFrame].toMutableList()
                        )
                        currentFrame++
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
                    if (frames.size > 1) {
                        isPlaying = !isPlaying
                    }
                }
            ) {
                Text(if (isPlaying) "Stop" else "Play")
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
EOF

cat > "$ROOT/.github/workflows/build-apk.yml" <<'EOF'
name: Build MiniAnimator APK

on:
  workflow_dispatch:
  push:
    branches:
      - main

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"

      - name: Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Build APK
        run: gradle :app:assembleDebug

      - name: Verify APK
        run: test -f app/build/outputs/apk/debug/app-debug.apk

      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: MiniAnimator-debug-${{ github.run_id }}
          path: app/build/outputs/apk/debug/app-debug.apk
EOF

echo
echo "MINIANIMATOR_CREATO"
find "$ROOT" -maxdepth 5 -type f | sort
