const assert=require('node:assert/strict'),F=require('../app/src/main/assets/formats.js'),M=require('../app/src/main/assets/model.js');
const bmp=F.bmp(new Uint8ClampedArray([255,0,0,255,0,0,255,255,0,255,0,255,0,0,0,0]),2,2),v=new DataView(bmp.buffer);
assert.equal(String.fromCharCode(bmp[0],bmp[1]),'BM');assert.equal(v.getUint32(2,true),70);assert.equal(v.getInt32(18,true),2);assert.equal(v.getUint16(28,true),24);
assert.deepEqual([...bmp.slice(54,60)],[0,255,0,255,255,255],'BMP stores bottom row first, composites alpha on white and writes BGR');
const p=M.create({name:'A < B',width:100,height:100});const id=p.layers[0].id;p.frames[0].vectors={[id]:{base:null,ops:[{type:'stroke',color:'#ff0000',opacity:1,width:8,points:[{x:10,y:10,w:8},{x:90,y:10,w:8}]},{type:'stroke',eraser:true,opacity:1,points:[{x:50,y:10,w:10}]},{type:'text',text:'<script>alert(1)</script>',x:0,y:20,size:20,color:'#000000',opacity:1}]}};
const result=F.svg(p,0);assert(result.svg.includes('<path '));assert(result.svg.includes('<mask '));assert(result.svg.includes('&lt;script&gt;'));assert(!result.svg.includes('<script>'));assert(!result.svg.includes('<image'));assert.equal(result.rasterCount,0);assert.equal(result.vectorCount,3);
console.log('BMP pixel layout, SVG paths/masks, vector preservation and XML escaping checks passed');
