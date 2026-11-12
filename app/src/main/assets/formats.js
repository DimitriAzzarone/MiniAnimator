(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.AnimationFormats=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
function bmp(data,width,height){const row=(width*3+3)&~3,bytes=new Uint8Array(54+row*height),v=new DataView(bytes.buffer);bytes[0]=66;bytes[1]=77;v.setUint32(2,bytes.length,true);v.setUint32(10,54,true);v.setUint32(14,40,true);v.setInt32(18,width,true);v.setInt32(22,height,true);v.setUint16(26,1,true);v.setUint16(28,24,true);v.setUint32(34,row*height,true);v.setInt32(38,2835,true);v.setInt32(42,2835,true);for(let y=0;y<height;y++)for(let x=0;x<width;x++){const p=(y*width+x)*4,out=54+(height-1-y)*row+x*3,a=data[p+3]/255;bytes[out]=Math.round(data[p+2]*a+255*(1-a));bytes[out+1]=Math.round(data[p+1]*a+255*(1-a));bytes[out+2]=Math.round(data[p]*a+255*(1-a));}return bytes;}
const escape=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&apos;'}[c]));
const n=(value,fallback=0)=>Number.isFinite(+value)?Math.max(-1000000,Math.min(1000000,+value)):fallback;
const color=v=>/^#[0-9a-f]{6}$/i.test(v)?v:'#22283a';
function svg(project,index){let masks=[],serial=0,vectorCount=0,rasterCount=0,pointsUsed=0;const width=project.width,height=project.height;
 function image(data){if(typeof data!=='string'||!/^data:image\/(png|jpeg|webp);base64,[a-z0-9+/=]+$/i.test(data))return '';rasterCount++;return '<image width="'+width+'" height="'+height+'" href="'+data+'"/>';}
 function operation(op){if(!op||typeof op!=='object')return '';const alpha=Math.max(0,Math.min(1,n(op.opacity,1))),ink=color(op.color),w=Math.max(.1,n(op.width,8));
  if(op.type==='stroke'&&Array.isArray(op.points)){vectorCount++;pointsUsed+=op.points.length;if(pointsUsed>120000)throw Error('SVG troppo complesso: esporta il fotogramma in PNG o semplifica i tracciati.');const points=op.points;let content='';for(let i=0;i<points.length;i++){const p=points[i],r=Math.max(.1,n(p.w,w));if(i===0)content+='<circle cx="'+n(p.x)+'" cy="'+n(p.y)+'" r="'+r/2+'" fill="'+ink+'" opacity="'+alpha+'"/>';else{const a=points[i-1];content+='<path d="M '+n(a.x)+' '+n(a.y)+' L '+n(p.x)+' '+n(p.y)+'" fill="none" stroke="'+ink+'" stroke-width="'+r+'" stroke-linecap="round" opacity="'+alpha+'"/>';}}return content;}
  const a=op.start||{},b=op.end||{},common=' fill="none" stroke="'+ink+'" stroke-width="'+w+'" stroke-linecap="round" stroke-linejoin="round" opacity="'+alpha+'"';
  if(op.type==='line'){vectorCount++;return '<path d="M '+n(a.x)+' '+n(a.y)+' L '+n(b.x)+' '+n(b.y)+'"'+common+'/>';}
  if(op.type==='rectangle'){vectorCount++;return '<rect x="'+Math.min(n(a.x),n(b.x))+'" y="'+Math.min(n(a.y),n(b.y))+'" width="'+Math.abs(n(b.x)-n(a.x))+'" height="'+Math.abs(n(b.y)-n(a.y))+'"'+common+'/>';}
  if(op.type==='ellipse'){vectorCount++;return '<ellipse cx="'+(n(a.x)+n(b.x))/2+'" cy="'+(n(a.y)+n(b.y))/2+'" rx="'+Math.abs(n(b.x)-n(a.x))/2+'" ry="'+Math.abs(n(b.y)-n(a.y))/2+'"'+common+'/>';}
  if(op.type==='text'){vectorCount++;const size=Math.max(8,Math.min(300,n(op.size,48)));return '<text x="'+n(op.x)+'" y="'+(n(op.y)+size*.85)+'" font-family="sans-serif" font-size="'+size+'" fill="'+ink+'" opacity="'+alpha+'">'+String(op.text||'').slice(0,10000).split('\n').map((line,i)=>'<tspan x="'+n(op.x)+'" dy="'+(i?size*1.25:0)+'">'+escape(line)+'</tspan>').join('')+'</text>';}
  return '';
 }
 let content=project.background==='transparent'?'':'<rect width="100%" height="100%" fill="'+color(project.background)+'"/>';
 for(const l of project.layers){if(!l.visible)continue;const f=project.frames[index],v=f.vectors?.[l.id];let group=image(v?v.base:f.cels[l.id]);if(v&&(v.ops||[]).length>10000)throw Error('Troppi elementi per questo SVG.');if(v)for(const op of (v.ops||[])){
  if(op.eraser&&op.type==='stroke'){const mask='erase-'+(++serial);const erased=operation({...op,color:'#000000'});masks.push('<mask id="'+mask+'" maskUnits="userSpaceOnUse" x="0" y="0" width="'+width+'" height="'+height+'" style="mask-type:luminance"><rect width="100%" height="100%" fill="white"/>'+erased+'</mask>');group='<g mask="url(#'+mask+')">'+group+'</g>';}else group+=operation(op);
 }content+='<g opacity="'+Math.max(0,Math.min(1,n(l.opacity,1)))+'" data-layer="'+escape(l.name)+'">'+group+'</g>';}
 return{svg:'<?xml version="1.0" encoding="UTF-8"?>\n<svg xmlns="http://www.w3.org/2000/svg" width="'+width+'" height="'+height+'" viewBox="0 0 '+width+' '+height+'"><title>'+escape(project.name)+'</title><defs>'+masks.join('')+'</defs>'+content+'</svg>',vectorCount,rasterCount};
}
return{bmp,svg};
});
