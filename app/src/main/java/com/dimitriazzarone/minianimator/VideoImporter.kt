package com.dimitriazzarone.minianimator
import android.app.*
import android.content.*
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.view.*
import android.webkit.*
import android.widget.*
import org.json.*
import java.io.File
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

internal class VideoImporter(private val activity:Activity,private val web:WebView,private val worker:ExecutorService,private val launch:()->Unit){
 val busy=AtomicBoolean(false)
 private val canceled=AtomicBoolean(false)
 @Volatile private var closed=false
 @Volatile private var directory:File?=null
 @Volatile private var jobId=""
 @Volatile private var extracting=false
 private var preview:VideoView?=null
 private var dialog:AlertDialog?=null
 fun pick(id:String){if(closed)return;if(!Regex("[a-zA-Z0-9-]{1,80}").matches(id))return;if(!busy.compareAndSet(false,true)){emitError(id,"Un’importazione è già in corso. Attendi.");return};jobId=id;canceled.set(false);launch()}
 fun picked(uri:Uri?){if(jobId.isEmpty())jobId=UUID.randomUUID().toString();val id=jobId;if(uri==null){fail("Importazione video annullata.");return};busy.set(true);canceled.set(false);worker.execute{val m=MediaMetadataRetriever();try{m.setDataSource(activity,uri);val duration=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()?:0;var w=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()?:0;var h=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()?:0;val rotation=m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull()?:0;if(rotation%180!=0){val old=w;w=h;h=old};require(duration>=100&&w>0&&h>0){"Il file non contiene un video leggibile."};val width=w;val height=h;activity.runOnUiThread{if(!closed&&id==jobId&&!canceled.get())showOptions(uri,duration,width,height)}}catch(e:Exception){fail("Impossibile leggere il video: ${e.message?:"formato non supportato"}",id)}finally{runCatching{m.release()}}}}
 private fun label(text:String)=TextView(activity).apply{this.text=text;setTextColor(Color.WHITE);setPadding(8,8,8,4)}
 private fun seconds(t:Long)=String.format(java.util.Locale.US,"%.3f",t/1000.0)
 private fun showOptions(uri:Uri,total:Long,width:Int,height:Int){
 val root=LinearLayout(activity).apply{orientation=LinearLayout.VERTICAL;setPadding(14,10,14,10);setBackgroundColor(Color.rgb(22,59,105))};val playbackInfo=label("Caricamento anteprima…");preview=VideoView(activity).apply{setMediaController(MediaController(activity));setVideoURI(uri);setOnPreparedListener{seekTo(1);playbackInfo.text="Anteprima pronta: usa i controlli per riprodurre e cercare."};setOnErrorListener{_,_,_->playbackInfo.text="Anteprima non disponibile: il codec potrebbe non essere supportato.";true}};root.addView(preview,LinearLayout.LayoutParams(-1,(160*activity.resources.displayMetrics.density).toInt()));root.addView(playbackInfo);root.addView(label("Durata ${seconds(total)} s · Il video diventerà fotogrammi modificabili, senza audio."))
 fun input(title:String,value:String):EditText{root.addView(label(title));return EditText(activity).apply{setText(value);setTextColor(Color.WHITE);inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;root.addView(this)}}
 val start=input("Inizio (secondi)","0.00");val end=input("Fine (secondi)",seconds(min(5000,total)))
 val positions=LinearLayout(activity);for((name,field)in listOf("Inizio qui" to start,"Fine qui" to end)){val b=Button(activity).apply{text=name;isAllCaps=false;setOnClickListener{field.setText(seconds(preview?.currentPosition?.toLong()?:0))}};positions.addView(b,LinearLayout.LayoutParams(0,-2,1f))};root.addView(positions)
 fun spinner(title:String,values:Array<String>,selected:Int):Spinner{root.addView(label(title));return Spinner(activity).apply{adapter=object:ArrayAdapter<String>(activity,android.R.layout.simple_spinner_dropdown_item,values){override fun getView(position:Int,convertView:View?,parent:ViewGroup):View=super.getView(position,convertView,parent).apply{(this as? TextView)?.setTextColor(Color.WHITE)}};setSelection(selected);root.addView(this)}}
 val size=spinner("Lato massimo",arrayOf("320 px","480 px","640 px","960 px"),2);val fps=spinner("Fotogrammi al secondo",arrayOf("5 FPS","10 FPS","12 FPS","15 FPS"),1);root.addView(label("Massimo 240 fotogrammi e 24 milioni di pixel totali. Riduci intervallo, dimensioni o FPS se necessario."))
 val scroll=ScrollView(activity).apply{addView(root)}
 val d=AlertDialog.Builder(activity).setTitle("Importa video").setView(scroll).setNegativeButton("Annulla"){_,_->preview?.stopPlayback();fail("Importazione video annullata.")}.setPositiveButton("Importa",null).create();dialog=d;d.setOnCancelListener{preview?.stopPlayback();fail("Importazione video annullata.")};d.setOnShowListener{d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener{try{
 fun time(e:EditText):Long{val n=e.text.toString().replace(',','.').toDouble();require(n.isFinite()&&n>=0&&n<=total/1000.0+1){"Tempo non valido."};return(n*1000).roundToLong()}
 val side=intArrayOf(320,480,640,960)[size.selectedItemPosition];val rate=intArrayOf(5,10,12,15)[fps.selectedItemPosition];val scale=min(1.0,min(side.toDouble()/width,side.toDouble()/height));val plan=VideoImportPlan(time(start),time(end),total,side,rate,max(1,(width*scale).roundToInt()),max(1,(height*scale).roundToInt()));preview?.stopPlayback();preview=null;d.dismiss();dialog=null;extract(uri,plan)
 }catch(e:Exception){Toast.makeText(activity,e.message?:"Opzioni non valide",Toast.LENGTH_LONG).show()}}};d.show()
 }
 private fun frame(m:MediaMetadataRetriever,time:Long,side:Int):Bitmap?{if(Build.VERSION.SDK_INT>=27)return m.getScaledFrameAtTime(time*1000,MediaMetadataRetriever.OPTION_CLOSEST,side,side);val raw=m.getFrameAtTime(time*1000,MediaMetadataRetriever.OPTION_CLOSEST)?:return null;val ratio=min(1f,min(side.toFloat()/raw.width,side.toFloat()/raw.height));if(ratio==1f)return raw;val small=Bitmap.createScaledBitmap(raw,max(1,(raw.width*ratio).roundToInt()),max(1,(raw.height*ratio).roundToInt()),true);if(small!==raw)raw.recycle();return small}
 private fun extract(uri:Uri,plan:VideoImportPlan){val id=jobId;activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);worker.execute{extracting=true;var dir:File?=null;val m=MediaMetadataRetriever();try{
 check(!closed&&!canceled.get()&&id==jobId);dir=File(activity.cacheDir,"mini-video-$id").apply{mkdirs()};directory=dir;m.setDataSource(activity,uri);val frames=JSONArray();var width=0;var height=0;var bytes=0L
 for(i in 0 until plan.count){if(closed||canceled.get()||Thread.currentThread().isInterrupted)throw InterruptedException();val bitmap=frame(m,plan.time(i),plan.side)?:error("Fotogramma ${i+1} non leggibile");try{if(i==0){width=bitmap.width;height=bitmap.height;check(width.toLong()*height*plan.count<=24000000){"Video troppo grande: riduci dimensioni o durata."}};check(bitmap.width==width&&bitmap.height==height){"Dimensioni del video variabili non supportate."};val name=String.format(java.util.Locale.US,"%04d.jpg",i);val file=File(dir,name);file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,95,it))};bytes+=file.length();check(bytes<=12000000){"Fotogrammi troppo pesanti: riduci durata o risoluzione."};frames.put(JSONObject().put("file",name).put("durationMs",plan.duration(i)))}finally{bitmap.recycle()};emit("window.onNativeVideoProgress",JSONObject().put("id",id).put("message","Importo fotogramma ${i+1} di ${plan.count}").toString())}
 if(closed||canceled.get())throw InterruptedException();val manifest=JSONObject().put("id",id).put("name","Video importato").put("width",width).put("height",height).put("fps",plan.fps).put("frames",frames);File(dir,"manifest.json").writeText(manifest.toString());emit("window.onNativeVideoReady",manifest.toString())
 }catch(e:Exception){dir?.deleteRecursively();fail(if(canceled.get()||closed)"Importazione video annullata." else e.message?:"Importazione non riuscita.",id)}catch(e:OutOfMemoryError){dir?.deleteRecursively();fail("Memoria insufficiente: prova un video o una risoluzione più piccoli.",id)}finally{extracting=false;runCatching{m.release()};activity.runOnUiThread{activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)}}}}
 fun open(path:String):WebResourceResponse?{val dir=directory?:return null;if(!Regex("[0-9]{4}\\.jpg").matches(path))return null;val f=File(dir,path);if(!f.isFile)return null;return runCatching{WebResourceResponse("image/jpeg",null,200,"OK",mapOf("Cache-Control" to "no-store"),f.inputStream())}.getOrNull()}
 fun finish(id:String){if(id!=jobId)return;if(extracting){canceled.set(true);return};directory?.deleteRecursively();directory=null;jobId="";busy.set(false)}
 fun cancel(id:String){if(id!=jobId)return;canceled.set(true);preview?.stopPlayback();dialog?.dismiss();dialog=null;if(!extracting)fail("Importazione video annullata.",id)}
 private fun fail(message:String,id:String=jobId){if(id!=jobId)return;busy.set(false);directory?.deleteRecursively();directory=null;jobId="";emitError(id,message)}
 private fun emitError(id:String,message:String){emit("window.onNativeVideoError",JSONObject().put("id",id).put("message",message).toString())}
 private fun emit(function:String,payload:String){activity.runOnUiThread{if(!closed)web.evaluateJavascript("$function&&$function($payload);",null)}}
 fun close(){closed=true;canceled.set(true);preview?.stopPlayback();dialog?.dismiss();directory?.deleteRecursively();directory=null}
}
