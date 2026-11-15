package com.dimitriazzarone.minianimator
import com.squareup.gifencoder.*
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Test
class GifTimingTest {
 private fun delays(bytes:ByteArray):List<Int>{
  assertTrue(String(bytes.copyOfRange(0,6)).startsWith("GIF8"));var p=13;val result=mutableListOf<Int>();var delay=0
  fun byte():Int{check(p<bytes.size);return bytes[p++].toInt() and 255}
  fun blocks(){while(true){val size=byte();if(size==0)return;p+=size;check(p<=bytes.size)}}
  val global=bytes[10].toInt() and 255;if(global and 128!=0)p+=3*(1 shl((global and 7)+1))
  while(p<bytes.size){when(byte()){
   0x21->{val label=byte();if(label==0xf9){assertEquals(4,byte());byte();val lo=byte();delay=lo+(byte() shl 8);byte();assertEquals(0,byte())}else blocks()}
   0x2c->{repeat(8){byte()};val packed=byte();if(packed and 128!=0)p+=3*(1 shl((packed and 7)+1));byte();blocks();result.add(delay);delay=0}
   0x3b->return result
   else->error("Invalid GIF block")
  }};error("Missing GIF trailer")
 }
 @Test fun realGifKeepsOneSecondAtFractionalFrameRates(){for(fps in listOf(5,10,12,15,30)){val data=ByteArrayOutputStream();val encoder=GifEncoder(data,2,2,0);val clock=GifDelayClock();repeat(fps){i->val color=if(i%2==0)0xff0000 else 0x0000ff;encoder.addImage(IntArray(4){color},2,ImageOptions().setDelay(clock.next(1000.0/fps),TimeUnit.MILLISECONDS))};encoder.finishEncoding();val timing=delays(data.toByteArray());assertEquals(fps,timing.size);assertTrue(timing.all{it>0});assertEquals("GIF duration at $fps FPS",100,timing.sum())}}
 @Test fun existingGifDelaysStayExact(){val clock=GifDelayClock();for(d in listOf(60L,140L,230L,90L))assertEquals(d,clock.next(d.toDouble()))}
}
