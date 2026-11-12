/* Serializable animation document model. No DOM dependencies. */
(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.AnimationModel=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';let sequence=0;const id=()=>Date.now().toString(36)+'-'+(++sequence).toString(36);
const clone=x=>JSON.parse(JSON.stringify(x));
const clamp=(v,a,b,d)=>Number.isFinite(+v)?Math.max(a,Math.min(b,+v)):d;
function layer(name){return{id:id(),name:name||'Livello',visible:true,locked:false,opacity:1};}
function frame(){return{id:id(),hold:1,cels:{},vectors:{}};}
function create(options={}){return{format:'MiniAnimator',version:4,name:String(options.name||'Senza titolo').slice(0,120),width:Math.round(clamp(options.width,64,1920,960)),height:Math.round(clamp(options.height,64,1920,540)),fps:Math.round(clamp(options.fps,1,30,12)),background:options.background==='transparent'?'transparent':'#ffffff',layers:[layer('Disegno 1')],frames:[frame()]};}
function validate(raw){
 if(!raw||!Array.isArray(raw.frames)||!raw.frames.length||raw.frames.length>240)throw Error('Progetto non valido: da 1 a 240 fotogrammi.');
 if(raw.version===4){
  if(!Array.isArray(raw.layers)||!raw.layers.length||raw.layers.length>24)throw Error('Da 1 a 24 livelli.');
  if(!Number.isFinite(+raw.width)||!Number.isFinite(+raw.height)||+raw.width<64||+raw.height<64||+raw.width*+raw.height>2073600)throw Error('Formato del progetto non valido.');
  const p=create(raw);p.layers=raw.layers.map((v,i)=>({id:String(v.id||id()),name:String(v.name||'Livello '+(i+1)).slice(0,80),visible:v.visible!==false,locked:!!v.locked,opacity:clamp(v.opacity,0,1,1)}));
  if(new Set(p.layers.map(x=>x.id)).size!==p.layers.length)throw Error('Identificativi dei livelli duplicati.');
  const allowed=new Set(p.layers.map(x=>x.id));
  p.frames=raw.frames.map(v=>{const f=frame();f.hold=Math.round(clamp(v.hold,1,24,1));for(const [key,data]of Object.entries(v.cels||{})){if(allowed.has(key)&&data){if(typeof data!=='string'||!/^data:image\/(png|jpeg|webp);base64,/.test(data)||data.length>16000000)throw Error('Immagine di un livello non valida.');f.cels[key]=data;}}for(const [key,vector]of Object.entries(v.vectors||{})){if(allowed.has(key)&&vector&&Array.isArray(vector.ops)&&vector.ops.length<=10000){const base=vector.base;if(base!==null&&base!==undefined&&(typeof base!=='string'||!/^data:image\/(png|jpeg|webp);base64,/.test(base)))throw Error('Base vettoriale non valida.');if(JSON.stringify(vector).length>8000000)throw Error('Tracciati del livello troppo grandi.');f.vectors[key]={base:base||null,ops:clone(vector.ops)};}}return f;});
  return p;
 }
 // Migrate legacy 3.x raster projects while preserving each frame's appearance.
 if(raw.frames.some(f=>!Array.isArray(f.layers)))throw Error('Formato del progetto non supportato.');
 const p=create({...raw,width:raw.width||900,height:raw.height||600});
 // Reuse matching legacy tracks, but keep distinct tracks whenever visibility
 // or opacity differed between frames so each imported frame retains its look.
 p.layers=[];p.frames=[];const tracks=new Map();
 raw.frames.forEach((old,i)=>{const f=frame();old.layers.forEach((v,j)=>{const signature=JSON.stringify([j,v.name||'Livello '+(j+1),v.visible!==false,clamp(v.opacity,0,1,1)]);let l=tracks.get(signature);if(!l){l=layer(String(v.name||'Livello '+(j+1)));l.visible=v.visible!==false;l.opacity=clamp(v.opacity,0,1,1);tracks.set(signature,l);p.layers.push(l);}if(v.img){if(!/^data:image\/(png|jpeg|webp);base64,/.test(v.img))throw Error('Immagine precedente non valida.');f.cels[l.id]=v.img;}});p.frames.push(f);});
 if(!p.layers.length)p.layers=[layer('Disegno 1')];
 // Import legacy projects up to 24 tracks without silently dropping anything.
 if(p.layers.length>24)throw Error('Questo progetto precedente supera 24 tracce: unisci i livelli prima di importarlo.');
 return p;
}
function addFrame(p,at,duplicate=false){if(p.frames.length>=240)throw Error('Limite di 240 fotogrammi.');const f=duplicate?clone(p.frames[at]):frame();f.id=id();p.frames.splice(at+1,0,f);return at+1;}
function deleteFrame(p,at){if(p.frames.length===1)throw Error('Conserva almeno un fotogramma.');p.frames.splice(at,1);return Math.min(at,p.frames.length-1);}
function moveFrame(p,at,delta){const to=Math.max(0,Math.min(p.frames.length-1,at+delta));if(to!==at)p.frames.splice(to,0,p.frames.splice(at,1)[0]);return to;}
function addLayer(p,at,duplicate=false){if(p.layers.length>=24)throw Error('Limite di 24 livelli.');const old=p.layers[at];const l=duplicate?{...old,id:id(),name:old.name+' copia'}:layer('Disegno '+(p.layers.length+1));p.layers.splice(at+1,0,l);if(duplicate)for(const f of p.frames){if(f.cels[old.id])f.cels[l.id]=f.cels[old.id];if(f.vectors?.[old.id]){f.vectors=f.vectors||{};f.vectors[l.id]=clone(f.vectors[old.id]);}}return at+1;}
function deleteLayer(p,at){if(p.layers.length===1)throw Error('Conserva almeno un livello.');const [l]=p.layers.splice(at,1);for(const f of p.frames){delete f.cels[l.id];if(f.vectors)delete f.vectors[l.id];}return Math.min(at,p.layers.length-1);}
function moveLayer(p,at,delta){const to=Math.max(0,Math.min(p.layers.length-1,at+delta));if(to!==at)p.layers.splice(to,0,p.layers.splice(at,1)[0]);return to;}
function duration(p){return p.frames.reduce((sum,f)=>sum+f.hold,0)/p.fps;}
class History{
 constructor(maxBytes=24000000){this.undoStack=[];this.redoStack=[];this.maxBytes=maxBytes;}
 push(value){const s=JSON.stringify(value);if(s.length>this.maxBytes)throw Error('Progetto troppo grande per la cronologia: esporta e semplifica i livelli.');this.undoStack.push(s);this.redoStack=[];while(this.undoStack.length>24||this.undoStack.reduce((n,x)=>n+x.length,0)>this.maxBytes)this.undoStack.shift();}
 undo(current){if(!this.undoStack.length)return null;this.redoStack.push(JSON.stringify(current));return JSON.parse(this.undoStack.pop());}
 redo(current){if(!this.redoStack.length)return null;this.undoStack.push(JSON.stringify(current));return JSON.parse(this.redoStack.pop());}
 clear(){this.undoStack=[];this.redoStack=[];}
}
return{create,validate,clone,addFrame,deleteFrame,moveFrame,addLayer,deleteLayer,moveLayer,duration,History};
});
