package com.dimitriazzarone.minianimator
import kotlin.math.ceil
import kotlin.math.roundToLong
internal data class VideoImportPlan(val start:Long,val end:Long,val total:Long,val side:Int,val fps:Int,val width:Int,val height:Int){
 val count=ceil((end-start)*fps/1000.0).toInt()
 init {require(start>=0&&end<=total&&end-start>=100){"Scegli un intervallo valido di almeno 0,1 secondi."};require(side in listOf(320,480,640,960)&&fps in listOf(5,10,12,15)){"Opzioni video non valide."};require(count in 1..240){"Massimo 240 fotogrammi: riduci durata o FPS."};require(width>0&&height>0&&width<=side&&height<=side&&width.toLong()*height*count<=24000000){"Troppi pixel: riduci durata, dimensioni o FPS."}}
 fun time(index:Int)=start+(end-start)*index/count
 fun duration(index:Int)=((end-start)*(index+1).toDouble()/count).roundToLong()-((end-start)*index.toDouble()/count).roundToLong()
}
