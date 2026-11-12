const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {JSDOM}=require('jsdom'),{createCanvas,Image}=require('@napi-rs/canvas'),{indexedDB}=require('fake-indexeddb');
const assets=path.resolve(__dirname,'../app/src/main/assets');
const dom=new JSDOM(fs.readFileSync(path.join(assets,'index.html'),'utf8'),{url:'https://appassets.androidplatform.net/assets/index.html',runScripts:'outside-only',pretendToBeVisual:true});
const w=dom.window;const canvases=new WeakMap();
function backing(e){let c=canvases.get(e);if(!c){c=createCanvas(e.width||300,e.height||150);canvases.set(e,c);}if(c.width!==e.width)c.width=e.width;if(c.height!==e.height)c.height=e.height;return c;}
w.HTMLCanvasElement.prototype.getContext=function(type){if(type!=='2d')throw Error(type);const native=backing(this).getContext('2d');return new Proxy(native,{get(t,k){if(k==='drawImage')return (image,...args)=>t.drawImage(image instanceof w.HTMLCanvasElement?backing(image):image,...args);const value=Reflect.get(t,k,t);return typeof value==='function'?value.bind(t):value;},set(t,k,v){t[k]=v;return true;}});};
w.HTMLCanvasElement.prototype.toDataURL=function(...args){return backing(this).toDataURL(...args);};
w.HTMLCanvasElement.prototype.setPointerCapture=function(){};
w.HTMLCanvasElement.prototype.getBoundingClientRect=function(){return {left:0,top:0,width:parseFloat(this.style.width)||this.width,height:parseFloat(this.style.height)||this.height};};
w.TextEncoder=TextEncoder;w.Image=Image;w.indexedDB=indexedDB;w.confirm=()=>true;w.prompt=(_label,value)=>value;
w.HTMLDialogElement.prototype.showModal=function(){this.setAttribute('open','');};w.HTMLDialogElement.prototype.close=function(){this.removeAttribute('open');};
Object.defineProperty(w.document.getElementById('viewport'),'clientWidth',{value:1000});Object.defineProperty(w.document.getElementById('viewport'),'clientHeight',{value:600});
const exported=[];w.Android={saveBase64:(name,mime,data)=>{exported.push({name,mime,data});w.onNativeExport(true,'ok');},saveProject:(name,data)=>{exported.push({name,data});w.onNativeExport(true,'ok');},exportGif:(name,data)=>{exported.push({name,data});w.onNativeExport(true,'ok');}};
for(const name of ['model.js','pixels.js','formats.js'])w.eval(fs.readFileSync(path.join(assets,name),'utf8'));
w.eval(fs.readFileSync(path.join(assets,'studio.js'),'utf8')+'\nwindow.TEST={actions,getProject:()=>project,setTool,setColor,selectFrame,render,ensureFrame,buffer,composite,exportFile,getHistory:()=>history,autosave};');
const wait=(ms=30)=>new Promise(r=>setTimeout(r,ms));
function pointer(type,x,y,id=1){const c=w.document.getElementById('canvas'),p=w.TEST.getProject(),rect=c.getBoundingClientRect();const e=new w.MouseEvent(type,{clientX:x*rect.width/p.width,clientY:y*rect.height/p.height,bubbles:true,cancelable:true});Object.defineProperties(e,{pointerId:{value:id},pointerType:{value:'mouse'},pressure:{value:.5}});c.dispatchEvent(e);}
async function pixel(frame,x,y){await w.TEST.ensureFrame(frame);const cv=w.TEST.buffer();w.TEST.composite(cv.getContext('2d'),frame,false);return [...cv.getContext('2d').getImageData(x,y,1,1).data];}
(async()=>{
 await wait(80);const T=w.TEST;
 T.setColor('#ff0000');pointer('pointerdown',100,100);pointer('pointermove',120,100);pointer('pointerup',120,100);await wait();
 assert.equal(T.getProject().frames.length,1);assert.equal((await pixel(0,110,100))[0],255);assert.equal((await pixel(0,110,100))[3],255);
 T.actions.duplicateFrame();await wait();assert.equal(T.getProject().frames.length,2);assert.equal((await pixel(1,110,100))[3],255);
 T.actions.addFrame();await wait();assert.equal((await pixel(2,110,100))[3],0,'new frame must remain genuinely blank');
 T.actions.undo();await wait();assert.equal(T.getProject().frames.length,2);
 T.actions.redo();await wait();assert.equal(T.getProject().frames.length,3);
 T.selectFrame(0);await wait();T.setTool('eraser');pointer('pointerdown',110,100);pointer('pointerup',110,100);await wait();assert.equal((await pixel(0,110,100))[3],0,'eraser must erase to alpha, not paint white');
 assert.equal((await pixel(1,110,100))[3],255,'editing duplicate must not alter source cel');
 T.actions.undo();await wait();assert.equal((await pixel(0,110,100))[3],255,'undo restores erased pixels');
 T.actions.addLayer();await wait();T.setColor('#0000ff');T.setTool('brush');pointer('pointerdown',200,150);pointer('pointerup',200,150);await wait();assert.equal((await pixel(0,200,150))[2],255);
 T.getProject().layers[1].opacity=.5;await T.render();const half=await pixel(0,200,150);assert(half[3]>=126&&half[3]<=129,'layer opacity must affect raster output');
 T.selectFrame(2);await wait();await T.render();
 await T.exportFile('png');await wait();assert.equal(exported.at(-1).mime,'image/png');
 const outputImage=new Image();await new Promise((resolve,reject)=>{outputImage.onload=resolve;outputImage.onerror=reject;outputImage.src=Buffer.from(exported.at(-1).data,'base64');});
 const check=createCanvas(outputImage.width,outputImage.height).getContext('2d');check.drawImage(outputImage,0,0);
 assert.deepEqual([...check.getImageData(110,100,1,1).data],[255,255,255,255],'onion-skin pixels must never appear in the exported PNG');
 T.getProject().frames[1].hold=3;T.getProject().fps=8;
 await T.exportFile('gif');await wait();const gif=JSON.parse(exported.at(-1).data);assert.equal(gif.frames.length,3);assert.equal(gif.fps,8);assert.equal(gif.frames[1].hold,3);
 await T.exportFile('project');assert.equal(JSON.parse(exported.at(-1).data).version,4);
 await T.exportFile('jpeg');await wait();assert.equal(exported.at(-1).mime,'image/jpeg');assert.equal(Buffer.from(exported.at(-1).data,'base64')[0],255);
 await T.exportFile('bmp');await wait();assert.equal(exported.at(-1).mime,'image/bmp');assert.equal(Buffer.from(exported.at(-1).data,'base64').subarray(0,2).toString(),'BM');
 T.selectFrame(0);await wait();await T.exportFile('svg');await wait();assert.equal(exported.at(-1).mime,'image/svg+xml');const svg=Buffer.from(exported.at(-1).data,'base64').toString();assert(svg.includes('<path '));assert(!svg.includes('<image'),'native strokes remain actual vector paths');
 const vectorImage=new Image();await new Promise((resolve,reject)=>{vectorImage.onload=resolve;vectorImage.onerror=reject;vectorImage.src=Buffer.from(svg);});const svgPixels=createCanvas(vectorImage.width,vectorImage.height).getContext('2d');svgPixels.drawImage(vectorImage,0,0);const svgPixel=svgPixels.getImageData(110,100,1,1).data;assert.equal(svgPixel[0],255);assert.equal(svgPixel[1],0,'the exported SVG renders the actual red vector stroke');


 T.getProject().layers[1].locked=true;const previous=JSON.stringify(T.getProject());pointer('pointerdown',300,200);pointer('pointerup',300,200);await wait();assert.equal(JSON.stringify(T.getProject()),previous,'locked layers must reject painting');
 await T.autosave();const db=await new Promise(resolve=>{const r=indexedDB.open('MiniAnimatorStudio',1);r.onsuccess=()=>resolve(r.result);});const saved=await new Promise(resolve=>{const r=db.transaction('projects').objectStore('projects').get('current');r.onsuccess=()=>resolve(r.result);});assert.equal(saved.frames.length,3);assert.equal(saved.fps,8);db.close();
 T.getProject().layers[1].locked=false;T.selectFrame(0);await wait();
 T.setTool('select');pointer('pointerdown',190,140);pointer('pointermove',210,160);pointer('pointerup',210,160);await wait();
 T.setTool('moveSelection');pointer('pointerdown',200,150);pointer('pointermove',250,150);pointer('pointerup',250,150);await wait();
 assert.equal((await pixel(0,200,150))[3],0,'moving a selection clears its original position');assert.equal((await pixel(0,250,150))[2],255,'moving a selection preserves its color');
 w.prompt=(label,value)=>label==='Testo da inserire'?'Animazione':value;T.setTool('text');const beforeText=T.getProject().frames[0].cels[T.getProject().layers[1].id];pointer('pointerdown',200,200);pointer('pointerup',200,200);await wait();assert.notEqual(T.getProject().frames[0].cels[T.getProject().layers[1].id],beforeText,'text is committed to the active cel');
 w.document.querySelector('[data-document=flipH]').click();await wait(100);assert.equal((await pixel(0,709,150))[2],255,'horizontal transform reflects actual pixels');assert.equal((await pixel(0,250,150))[3],0);
 w.prompt=(label,value)=>label.startsWith('Larghezza')?'480':label.startsWith('Altezza')?'270':value;w.document.querySelector('[data-document=resize]').click();await wait(200);assert.equal(T.getProject().width,480);assert.equal(T.getProject().height,270);assert.equal((await pixel(0,354,75))[2],255,'resizing preserves scaled artwork');
 console.log('Actual editor integration passed: drawing, history, frame isolation, alpha, onion-free export, autosave, selection/move, text, transforms and document resize');
 dom.window.close();
})().catch(e=>{console.error(e);dom.window.close();process.exitCode=1;});
