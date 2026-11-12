const assert=require('node:assert/strict'),{floodFill}=require('../app/src/main/assets/pixels.js');
let a=new Uint8ClampedArray(5*3*4);for(let y=0;y<3;y++){const i=(y*5+2)*4;a[i]=255;a[i+3]=255;}
floodFill(a,5,3,0,1,[0,80,200,255]);
assert.equal(a[(1*5+1)*4+3],255);assert.equal(a[(1*5+3)*4+3],0,'fill must not cross the barrier');
assert.equal(a[(1*5+2)*4],255,'boundary unchanged');assert.equal(floodFill(a,5,3,0,1,[0,80,200,255]),false);
assert.equal(floodFill(a,5,3,-1,0,[0,0,0,255]),false);
console.log('Flood fill boundaries, alpha and no-op checks passed');
