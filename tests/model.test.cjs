const assert=require('node:assert/strict');const M=require('../app/src/main/assets/model.js');
const p=M.create({fps:12,width:960,height:540});
const ink='data:image/png;base64,aGVsbG8=';p.frames[0].cels[p.layers[0].id]=ink;
M.addFrame(p,0,true);p.frames[1].hold=3;
assert.equal(p.frames[1].cels[p.layers[0].id],ink);assert.notEqual(p.frames[0].id,p.frames[1].id);
p.frames[1].cels[p.layers[0].id]='data:image/png;base64,Yg==';assert.equal(p.frames[0].cels[p.layers[0].id],ink);
M.addLayer(p,0,true);assert.equal(p.frames[0].cels[p.layers[1].id],ink);
M.deleteLayer(p,0);assert.equal(Object.keys(p.frames[0].cels).length,1);
assert.equal(M.duration(p),4/12);
const h=new M.History();h.push(p);p.name='Changed';const old=h.undo(p);assert.notEqual(old.name,'Changed');assert.equal(h.redo(old).name,'Changed');
const q=M.validate(JSON.parse(JSON.stringify(p)));assert.equal(q.frames[1].hold,3);assert.equal(q.width,960);
assert.throws(()=>M.validate({...p,frames:[]}));assert.throws(()=>M.validate({...p,layers:[p.layers[0],p.layers[0]]}));
const legacy=M.validate({frames:[{layers:[{name:'A',visible:false,opacity:.4,img:ink}]},{layers:[{name:'B',img:ink}]}]});
assert.equal(legacy.layers[0].visible,false);assert.equal(legacy.layers[0].opacity,.4);assert.equal(legacy.frames[1].cels[legacy.layers[0].id],undefined);
assert.equal(legacy.frames[1].cels[legacy.layers[1].id],ink);
M.moveFrame(p,0,1);assert.equal(p.frames[0].hold,3);
console.log('Animation document, migration, layer/frame editing and history checks passed');

const many=M.validate({frames:Array.from({length:40},()=>({layers:[{name:'A',img:ink}]}))});assert.equal(many.layers.length,1);assert.equal(many.frames.length,40);

const vectorProject=M.create();const vectorId=vectorProject.layers[0].id;vectorProject.frames[0].vectors[vectorId]={base:null,ops:[{type:'stroke',points:[{x:4,y:5,w:2}],color:'#ff0000',opacity:1}]};M.addFrame(vectorProject,0,true);vectorProject.frames[1].vectors[vectorId].ops[0].points[0].x=99;assert.equal(vectorProject.frames[0].vectors[vectorId].ops[0].points[0].x,4);const reopened=M.validate(JSON.parse(JSON.stringify(vectorProject)));assert.equal(reopened.frames[0].vectors[vectorId].ops[0].points[0].x,4);

const timing=M.create();timing.frames[0].durationMs=130;const timed=M.validate(JSON.parse(JSON.stringify(timing)));assert.equal(timed.frames[0].durationMs,130);assert.equal(M.duration(timed),.13);assert.throws(()=>M.validate({...timing,frames:[{...timing.frames[0],durationMs:-1}]}));
