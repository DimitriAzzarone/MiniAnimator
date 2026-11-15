(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.AnimationWorld=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
function bounds(op){
 if(['raster','eraseImage','clear'].includes(op.type))return{x:op.x,y:op.y,w:op.w,h:op.h};
 if(op.type==='text'){const lines=String(op.text).split('\n');return{x:op.x,y:op.y,w:Math.max(...lines.map(t=>t.length),1)*op.size,h:lines.length*op.size*1.3};}
 const points=op.type==='stroke'?op.points:[op.start,op.end];if(!points?.length)return null;let minX=Infinity,minY=Infinity,maxX=-Infinity,maxY=-Infinity,pad=op.width||1;for(const p of points){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);pad=Math.max(pad,p.w||1);}pad=pad/2+2;return{x:minX-pad,y:minY-pad,w:maxX-minX+pad*2,h:maxY-minY+pad*2};
}
function intersects(a,b){return a&&b&&a.x<=b.x+b.w&&a.y<=b.y+b.h&&a.x+a.w>=b.x&&a.y+a.h>=b.y;}
function union(a,b){if(!a)return {...b};const x=Math.min(a.x,b.x),y=Math.min(a.y,b.y);return{x,y,w:Math.max(a.x+a.w,b.x+b.w)-x,h:Math.max(a.y+a.h,b.y+b.h)-y};}
function validate(scene){
 if(!scene||!Array.isArray(scene.ops)||scene.ops.length>10000||JSON.stringify(scene).length>24000000)throw Error('Contenuto dell’area di lavoro troppo grande.');
 const result=JSON.parse(JSON.stringify(scene));
 for(const op of result.ops){if(!['stroke','line','rectangle','ellipse','text','raster','eraseImage','clear'].includes(op.type))throw Error('Elemento dell’area di lavoro non valido.');
  const points=op.type==='stroke'?op.points:['line','rectangle','ellipse'].includes(op.type)?[op.start,op.end]:[{x:op.x,y:op.y}];
  if(!Array.isArray(points)||points.length>120000||!points.length||points.some(p=>!p||!Number.isFinite(p.x)||!Number.isFinite(p.y)||Math.abs(p.x)>1000000||Math.abs(p.y)>1000000))throw Error('Coordinate del disegno non valide.');
  if(op.width!==undefined&&(!Number.isFinite(op.width)||op.width<=0||op.width>1000)||op.opacity!==undefined&&(!Number.isFinite(op.opacity)||op.opacity<0||op.opacity>1)||points.some(p=>p.w!==undefined&&(!Number.isFinite(p.w)||p.w<=0||p.w>1000)))throw Error('Pennello o opacità non validi.');
  if(['raster','eraseImage','clear'].includes(op.type)&&(!Number.isFinite(op.w)||!Number.isFinite(op.h)||op.w<=0||op.h<=0||op.w*op.h>8388608))throw Error('Immagine dell’area di lavoro troppo grande.');
  if(['raster','eraseImage'].includes(op.type)&&(typeof op.data!=='string'||op.data.length>16000000||!/^data:image\/(png|jpeg|webp);base64,[a-z0-9+/=]+$/i.test(op.data)))throw Error('Immagine dell’area di lavoro non valida.');
  if(op.type==='text'&&(typeof op.text!=='string'||op.text.length>10000||!Number.isFinite(op.size)||op.size<1||op.size>300))throw Error('Testo non valido.');
 }
 return result;
}
return{bounds,intersects,union,validate};
});
