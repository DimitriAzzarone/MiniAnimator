(function(root,factory){const api=factory();if(typeof module==='object')module.exports=api;else root.PixelTools=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
function floodFill(data,width,height,x,y,rgba,tolerance=18){
 x=Math.floor(x);y=Math.floor(y);if(x<0||y<0||x>=width||y>=height)return false;
 const offset=(y*width+x)*4,target=Array.from(data.slice(offset,offset+4));
 if(target.every((v,i)=>Math.abs(v-rgba[i])<2))return false;
 const visited=new Uint8Array(width*height),stack=[y*width+x];
 const match=i=>{const at=i*4;return !visited[i]&&target.every((v,k)=>Math.abs(data[at+k]-v)<=tolerance);};
 while(stack.length){const seed=stack.pop();if(!match(seed))continue;const row=Math.floor(seed/width);let left=seed%width,right=left;
  while(left>0&&match(row*width+left-1))left--;while(right<width-1&&match(row*width+right+1))right++;
  let above=false,below=false;
  for(let col=left;col<=right;col++){const i=row*width+col;visited[i]=1;for(let k=0;k<4;k++)data[i*4+k]=rgba[k];
   if(row>0){const hit=match(i-width);if(hit&&!above)stack.push(i-width);above=hit;}
   if(row<height-1){const hit=match(i+width);if(hit&&!below)stack.push(i+width);below=hit;}
  }
 }
 return true;
}
return{floodFill};
});
