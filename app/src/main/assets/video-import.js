/* Native video decoding, atomic document import. Frames become embedded JPEG cels. */
(() => {
'use strict';let active=null;
const cancel=document.createElement('button');cancel.id='cancelVideoImport';cancel.textContent='Annulla importazione';cancel.hidden=true;$('busy').append(cancel);
function release(job){if(active!==job)return;active=null;cancel.hidden=true;setBusy(false);}
function fail(job,message){if(active!==job)return;job.controller.abort();window.Android?.finishVideoImport?.(job.id);release(job);toast(message);}
actions.importVideo=()=>{if(exporting)return;if(!window.Android?.pickVideo){toast('Importazione video disponibile nell’app Android.');return;}stopPlayback();cancelStroke();if(project.frames.some(f=>Object.values(f.cels).some(Boolean)||Object.values(f.world||{}).some(v=>v.ops.length))&&!confirm('Importare il video come nuovo progetto a fotogrammi? Salva prima il lavoro attuale per conservarne una copia.'))return;const id='video-'+Date.now().toString(36)+'-'+Math.random().toString(36).slice(2);active={id,controller:new AbortController()};cancel.hidden=false;setBusy(true,'Scegli il video e l’intervallo da importare…');try{Android.pickVideo(id);}catch(e){fail(active,'Impossibile aprire il selettore video.');}};
cancel.onclick=()=>{const job=active;if(!job)return;job.controller.abort();window.Android?.cancelVideoImport?.(job.id);window.Android?.finishVideoImport?.(job.id);release(job);toast('Importazione annullata: il progetto attuale è conservato.');};
window.onNativeVideoProgress=result=>{if(active?.id===result?.id)$('busyText').textContent=result.message;};
window.onNativeVideoError=result=>{if(active?.id===result?.id)fail(active,result.message||'Importazione video non riuscita.');};
function dataUrl(blob){return new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result);reader.onerror=()=>reject(Error('Fotogramma non leggibile.'));reader.readAsDataURL(blob);});}
window.onNativeVideoReady=async manifest=>{
 const job=active;if(!job||job.id!==manifest?.id){window.Android?.finishVideoImport?.(manifest?.id||'');return;}
 try{
  if(!Number.isInteger(manifest.width)||!Number.isInteger(manifest.height)||manifest.width<1||manifest.height<1||manifest.width>960||manifest.height>960||!Array.isArray(manifest.frames)||manifest.frames.length<1||manifest.frames.length>240||manifest.width*manifest.height*manifest.frames.length>24000000||![5,10,12,15].includes(manifest.fps))throw Error('Dati video non validi o troppo grandi.');
  const next=M.create({name:manifest.name||'Video importato',width:manifest.width,height:manifest.height,fps:manifest.fps,background:'#ffffff'});next.frames=[];next.layers[0].name='Video importato';let encoded=0;
  for(let i=0;i<manifest.frames.length;i++){
   if(active!==job||job.controller.signal.aborted)return;const frame=manifest.frames[i];if(!/^[0-9]{4}\.jpg$/.test(frame.file)||!Number.isInteger(frame.durationMs)||frame.durationMs<10||frame.durationMs>655350)throw Error('Fotogramma video non valido.');
   const response=await fetch('/video/'+frame.file+'?job='+encodeURIComponent(job.id),{signal:job.controller.signal,cache:'no-store'});if(!response.ok)throw Error('Un fotogramma non è stato caricato.');const blob=await response.blob();if(blob.type!=='image/jpeg'||blob.size>4000000)throw Error('Fotogramma non valido o troppo grande.');const data=await dataUrl(blob);encoded+=data.length;if(encoded>20000000)throw Error('Video troppo pesante: riduci durata o dimensione.');
   const image=new Image();await new Promise((resolve,reject)=>{image.onload=resolve;image.onerror=()=>reject(Error('Impossibile decodificare un fotogramma.'));image.src=data;});if(image.width!==manifest.width||image.height!==manifest.height)throw Error('Dimensioni dei fotogrammi non coerenti.');image.src='';
   next.frames.push({id:job.id+'-'+i,hold:1,durationMs:frame.durationMs,cels:{[next.layers[0].id]:data},vectors:{}});$('busyText').textContent='Preparo fotogramma '+(i+1)+' di '+manifest.frames.length;await new Promise(r=>setTimeout(r,0));
  }
  if(active!==job||job.controller.signal.aborted)return;const validated=M.validate(next);beforeEdit();project=validated;fi=li=0;selectionRect=null;cache.clear();changed();fit();await autosave();toast('Video importato: '+next.frames.length+' fotogrammi modificabili, senza audio.');
 }catch(e){if(active===job)toast(e.name==='AbortError'?'Importazione annullata.':'Video: '+e.message);}
 finally{window.Android?.finishVideoImport?.(job.id);release(job);}
};
const entry=document.createElement('button');entry.textContent='Importa video';entry.dataset.fileAction='importVideo';entry.onclick=()=>{$('fileDialog').close();actions.importVideo();};$('fileDialog').querySelector('.document-actions').append(entry);
})();
