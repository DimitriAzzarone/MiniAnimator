(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.AnimationGif=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
async function compose(gif,decodeFrame,options={}){
 const width=gif.lsd?.width,height=gif.lsd?.height,frames=(gif.frames||[]).filter(f=>f.image);
 if(!width||!height||width>1920||height>1920||width*height>2073600)throw Error('GIF troppo grande: massimo 2 megapixel per fotogramma.');
 if(!frames.length||frames.length>240||width*height*frames.length>24000000)throw Error('GIF troppo lunga o grande: massimo 240 fotogrammi e 24 milioni di pixel complessivi.');
 for(const f of frames){const d=f.image.descriptor;if(!d||d.width<1||d.height<1||d.width*d.height>2073600)throw Error('Fotogramma GIF non valido.');}
 const screen=new Uint8ClampedArray(width*height*4),result=[],bg=gif.gct?.[gif.lsd.backgroundColorIndex]||[0,0,0];let previous=null;
 function clear(rect,transparent){for(let y=Math.max(0,rect.top);y<Math.min(height,rect.top+rect.height);y++)for(let x=Math.max(0,rect.left);x<Math.min(width,rect.left+rect.width);x++){const p=(y*width+x)*4;screen[p]=transparent?0:bg[0];screen[p+1]=transparent?0:bg[1];screen[p+2]=transparent?0:bg[2];screen[p+3]=transparent?0:255;}}
 for(let i=0;i<frames.length;i++){
  if(previous?.disposal===2)clear(previous.rect,previous.transparent);
  else if(previous?.disposal===3&&previous.saved)screen.set(previous.saved);
  const f=decodeFrame(frames[i],gif.gct,true);if(!f?.patch)throw Error('Fotogramma GIF non leggibile.');
  if(i===0&&f.transparentIndex===undefined)clear({top:0,left:0,width,height},false);
  const saved=f.disposalType===3?screen.slice():null,d=f.dims;
  for(let y=0;y<d.height;y++)for(let x=0;x<d.width;x++){const source=(y*d.width+x)*4,tx=d.left+x,ty=d.top+y;if(tx<0||ty<0||tx>=width||ty>=height||!f.patch[source+3])continue;const target=(ty*width+tx)*4;screen[target]=f.patch[source];screen[target+1]=f.patch[source+1];screen[target+2]=f.patch[source+2];screen[target+3]=255;}
  const durationMs=Math.max(10,Math.min(655350,Math.round(f.delay||100)));
  if(options.onFrame){await options.onFrame({index:i,total:frames.length,width,height,rgba:screen,durationMs});result.push({durationMs});}
  else result.push({durationMs,rgba:screen.slice()});
  previous={disposal:f.disposalType||0,rect:d,saved,transparent:f.transparentIndex!==undefined};
 }
 return{width,height,frames:result};
}
async function decode(buffer,codec,options={}){if(buffer.byteLength>30000000)throw Error('GIF oltre il limite di 30 MB.');const bytes=new Uint8Array(buffer);if(bytes.length<13||String.fromCharCode(...bytes.slice(0,6)).match(/^GIF8[79]a$/)===null)throw Error('Questo file non è una GIF valida.');return compose(codec.parseGIF(buffer),codec.decompressFrame,options);}
return{decode,compose};
});
