package com.dimitriazzarone.minianimator
import kotlin.math.roundToLong
/** GIF stores hundredths of a second: round cumulative boundaries, not every frame down. */
internal class GifDelayClock {
 private var elapsed=0.0
 private var emitted=0L
 fun next(durationMs:Double):Long{require(durationMs.isFinite()&&durationMs>=10);elapsed+=durationMs;val boundary=(elapsed/10).roundToLong();val delay=(boundary-emitted).coerceAtLeast(1);emitted+=delay;return delay*10}
}
