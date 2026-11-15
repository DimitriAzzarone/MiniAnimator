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
function connectedMask(data,width,height,x,y){x=Math.floor(x);y=Math.floor(y);if(x<0||y<0||x>=width||y>=height||!data[(y*width+x)*4+3])return null;const mask=new Uint8Array(width*height),queue=new Int32Array(width*height);let head=0,tail=0,minX=x,maxX=x,minY=y,maxY=y;queue[tail++]=y*width+x;mask[y*width+x]=1;while(head<tail){const i=queue[head++],cx=i%width,cy=Math.floor(i/width);minX=Math.min(minX,cx);maxX=Math.max(maxX,cx);minY=Math.min(minY,cy);maxY=Math.max(maxY,cy);for(let dy=-1;dy<=1;dy++)for(let dx=-1;dx<=1;dx++){const nx=cx+dx,ny=cy+dy;if(nx<0||ny<0||nx>=width||ny>=height)continue;const n=ny*width+nx;if(!mask[n]&&data[n*4+3]){mask[n]=1;queue[tail++]=n;}}}return{mask,count:tail,x:minX,y:minY,w:maxX-minX+1,h:maxY-minY+1};}
return{floodFill,connectedMask};
});
