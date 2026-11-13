const path=require('node:path');
require('esbuild').buildSync({entryPoints:[path.resolve(__dirname,'gif-codec-entry.cjs')],bundle:true,platform:'browser',format:'iife',globalName:'GifCodec',outfile:path.resolve(__dirname,'../app/src/main/assets/gif-codec.js'),minify:true,legalComments:'inline',nodePaths:(process.env.NODE_PATH||'').split(path.delimiter).filter(Boolean)});
