package com.dimitriazzarone.minianimator
import org.junit.Assert.*
import org.junit.Test
class VideoImportPlanTest {
 @Test fun trimAndFrameTiming(){val p=VideoImportPlan(200,1800,2400,640,5,320,180);assertEquals(8,p.count);assertEquals(200L,p.time(0));assertEquals(1600L,(0 until p.count).sumOf{p.duration(it)});assertEquals(1600L,p.time(7));assertTrue(p.time(7)<p.end)}
 @Test fun fractionalFpsKeepsSelectedDuration(){val p=VideoImportPlan(0,1000,1000,320,15,320,180);assertEquals(15,p.count);assertEquals(1000L,(0 until p.count).sumOf{p.duration(it)});assertTrue((0 until p.count).all{p.duration(it)in 66..67})}
 @Test(expected=IllegalArgumentException::class) fun rejectTooManyFrames(){VideoImportPlan(0,30000,30000,320,10,320,180)}
 @Test(expected=IllegalArgumentException::class) fun rejectTooManyPixels(){VideoImportPlan(0,20000,20000,960,5,960,540)}
 @Test(expected=IllegalArgumentException::class) fun rejectOutsideVideo(){VideoImportPlan(100,3000,2000,320,5,320,180)}
}
