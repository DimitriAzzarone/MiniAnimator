const assert=require('node:assert/strict'),{GIFEncoder}=require('gifenc'),codec=require('gifuct-js'),G=require('../app/src/main/assets/gif-import.js');
(async()=>{
 const e=GIFEncoder();const palette=[[255,0,0],[0,255,0]];e.writeFrame(new Uint8Array([0,0]),2,1,{palette,delay:40,dispose:1});e.writeFrame(new Uint8Array([1,1]),2,1,{palette,delay:130,dispose:1});e.finish();const bytes=e.bytes(),r=await G.decode(bytes.buffer.slice(bytes.byteOffset,bytes.byteOffset+bytes.byteLength),codec);
 assert.equal(r.frames.length,2);assert.equal(r.frames[0].durationMs,40);assert.equal(r.frames[1].durationMs,130);assert.deepEqual([...r.frames[0].rgba.slice(0,4)],[255,0,0,255]);assert.deepEqual([...r.frames[1].rgba.slice(0,4)],[0,255,0,255]);
 const d={left:0,top:0,width:2,height:1};const f=(patch,dispose=1)=>({image:{descriptor:d},decoded:{dims:d,patch:new Uint8ClampedArray(patch),transparentIndex:0,delay:100,disposalType:dispose}});
 const base=[255,0,0,255,255,0,0,255],blue=[0,0,255,255,0,0,0,0],green=[0,0,0,0,0,255,0,255];
 const restored=await G.compose({lsd:{width:2,height:1},frames:[f(base),f(blue,3),f(green)]},x=>x.decoded);
 assert.deepEqual([...restored.frames[2].rgba],[255,0,0,255,0,255,0,255],'disposal 3 restores the previous canvas');
 const cleared=await G.compose({lsd:{width:2,height:1},frames:[f(base,2),f(blue)]},x=>x.decoded);assert.equal(cleared.frames[1].rgba[7],0,'disposal 2 clears to transparent for transparent GIF frames');
 await assert.rejects(()=>G.compose({lsd:{width:1920,height:1080},frames:Array(20).fill(f(base))},x=>x.decoded),/troppo/);
 console.log('Actual GIF decoding, frame delays, transparency, disposal 2/3 and limits passed');
})().catch(e=>{console.error(e);process.exitCode=1;});
