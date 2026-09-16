package haven;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;

public class BrowserMapServer {
    private static final int maxcache = 2048;
    private static HttpServer server;
    private static int port;
    private static MapWnd current;
    private static final Map<String, byte[]> tilecache = new LinkedHashMap<String, byte[]>(maxcache, 0.75f, true) {
	protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
	    return(size() > maxcache);
	}
    };

    public static synchronized URI open(MapWnd map) throws IOException {
	if(current != map)
	    tilecache.clear();
	current = map;
	if(server == null) {
	    server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
	    server.createContext("/", BrowserMapServer::page);
	    server.createContext("/state", BrowserMapServer::state);
	    server.createContext("/tile", BrowserMapServer::tile);
	    server.createContext("/overlay", BrowserMapServer::overlay);
	    server.createContext("/marker", BrowserMapServer::marker);
	    server.createContext("/icon", BrowserMapServer::icon);
	    server.setExecutor(null);
	    server.start();
	    port = server.getAddress().getPort();
	}
	return(URI.create("http://127.0.0.1:" + port + "/"));
    }

    public static synchronized void close(MapWnd map) {
	if(current == map) {
	    current = null;
	    tilecache.clear();
	    if(server != null) {
		server.stop(0);
		server = null;
		port = 0;
	    }
	}
    }

    private static synchronized MapWnd current() {
	return(current);
    }

    private static void page(HttpExchange ex) throws IOException {
	byte[] data = ("<!doctype html><html><head><meta charset=\"utf-8\">" +
		      "<title>Kami Map</title><style>" +
		      "html,body{margin:0;width:100%;height:100%;overflow:hidden;background:#050505;color:#ddd;font:13px sans-serif}" +
		      "canvas{display:block;width:100vw;height:100vh;image-rendering:pixelated;cursor:grab}" +
		      "canvas.drag{cursor:grabbing}.hud{position:fixed;left:10px;top:10px;background:rgba(0,0,0,.55);padding:7px 9px;border:1px solid rgba(255,255,255,.15)}" +
		      "button{margin-left:6px;background:#263845;color:#eee;border:1px solid #587184;padding:3px 7px}button.on{background:#3f6178;color:white}" +
		      "</style></head><body><canvas id=\"map\"></canvas><div class=\"hud\">Kami Map <button id=\"follow\">Follow</button><button data-toggle=\"markers\">Markers</button><button data-toggle=\"labels\">Names</button><button data-toggle=\"icons\">Icons</button><button data-toggle=\"party\">Party</button><button data-toggle=\"grid\">Grid</button><button data-toggle=\"view\">View</button><button data-toggle=\"claims\">Claims</button><button data-toggle=\"village\">Village</button><button data-toggle=\"realm\">Realm</button></div><script>" +
		      "const cv=document.getElementById('map'),ctx=cv.getContext('2d'),btn=document.getElementById('follow');" +
		      "let st=null,center={x:0,y:0},ppu=2,follow=true,drag=null,cache=new Map();" +
		      "let layers={markers:true,labels:true,icons:true,party:true,grid:false,view:false,claims:false,village:false,realm:false};" +
		      "for(let b of document.querySelectorAll('[data-toggle]')){let k=b.dataset.toggle;b.classList.toggle('on',layers[k]);b.onclick=()=>{layers[k]=!layers[k];b.classList.toggle('on',layers[k])}}" +
		      "function resize(){cv.width=innerWidth*devicePixelRatio;cv.height=innerHeight*devicePixelRatio;ctx.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0)}addEventListener('resize',resize);resize();" +
		      "btn.onclick=()=>follow=true;" +
		      "cv.onmousedown=e=>{drag={x:e.clientX,y:e.clientY,cx:center.x,cy:center.y};follow=false;cv.classList.add('drag')};" +
		      "addEventListener('mouseup',()=>{drag=null;cv.classList.remove('drag')});" +
		      "addEventListener('mousemove',e=>{if(drag){center.x=drag.cx-(e.clientX-drag.x)/ppu;center.y=drag.cy-(e.clientY-drag.y)/ppu}});" +
		      "cv.onwheel=e=>{e.preventDefault();let old=ppu;ppu=Math.max(.25,Math.min(8,ppu*(e.deltaY<0?1.2:.8333)));let r=cv.getBoundingClientRect();center.x+=(e.clientX-r.width/2)*(1/old-1/ppu);center.y+=(e.clientY-r.height/2)*(1/old-1/ppu);follow=false};" +
		      "async function poll(){try{let r=await fetch('state',{cache:'no-store'});if(r.ok){st=await r.json();if(follow&&st.player)center={x:st.player.x,y:st.player.y};else if(follow)center={x:st.x,y:st.y};}}catch(e){}}poll();setInterval(poll,1000);" +
		      "function img(seg,x,y){let k=seg+':'+x+':'+y,o=cache.get(k);if(o)return o;let im=new Image();im.onerror=()=>cache.delete(k);im.src='tile?seg='+seg+'&x='+x+'&y='+y+'&lvl=0';cache.set(k,im);if(cache.size>700)cache.delete(cache.keys().next().value);return im}" +
		      "function ol(seg,x,y,t){let k='o:'+t+':'+seg+':'+x+':'+y,o=cache.get(k);if(o)return o;let im=new Image();im.onerror=()=>cache.delete(k);im.src='overlay?tag='+t+'&seg='+seg+'&x='+x+'&y='+y+'&lvl=0';cache.set(k,im);if(cache.size>700)cache.delete(cache.keys().next().value);return im}" +
		      "function asset(kind,id){let k=kind+':'+id,o=cache.get(k);if(o)return o;let im=new Image();im.onerror=()=>cache.delete(k);im.src=kind+'?id='+id;cache.set(k,im);return im}" +
		      "function dot(x,y,r,c){let sx=(x-center.x)*ppu+innerWidth/2,sy=(y-center.y)*ppu+innerHeight/2;ctx.fillStyle='rgba(0,0,0,.75)';ctx.beginPath();ctx.arc(sx,sy,r+2,0,Math.PI*2);ctx.fill();ctx.fillStyle=c;ctx.beginPath();ctx.arc(sx,sy,r,0,Math.PI*2);ctx.fill();return {sx,sy}}" +
		      "function stamp(kind,o,r){let sx=(o.x-center.x)*ppu+innerWidth/2,sy=(o.y-center.y)*ppu+innerHeight/2,im=asset(kind,o.id);if(im.complete&&im.naturalWidth){ctx.drawImage(im,sx-im.naturalWidth/2,sy-im.naturalHeight/2);return {sx,sy}}return dot(o.x,o.y,r,o.color)}" +
		      "function label(t,p){ctx.fillStyle='#fff';ctx.strokeStyle='#000';ctx.lineWidth=3;ctx.strokeText(t,p.sx+7,p.sy-7);ctx.fillText(t,p.sx+7,p.sy-7)}" +
		      "function grid(ulx,uly,brx,bry,ts){ctx.strokeStyle='rgba(255,255,255,.22)';ctx.lineWidth=1;ctx.beginPath();for(let x=Math.floor(ulx/ts)*ts;x<=brx;x+=ts){let sx=(x-center.x)*ppu+innerWidth/2;ctx.moveTo(sx,0);ctx.lineTo(sx,innerHeight)}for(let y=Math.floor(uly/ts)*ts;y<=bry;y+=ts){let sy=(y-center.y)*ppu+innerHeight/2;ctx.moveTo(0,sy);ctx.lineTo(innerWidth,sy)}ctx.stroke()}" +
		      "function view(){if(!st||!st.player)return;let p={sx:(st.player.x-center.x)*ppu+innerWidth/2,sy:(st.player.y-center.y)*ppu+innerHeight/2};ctx.strokeStyle='rgba(160,210,255,.6)';ctx.lineWidth=2;ctx.beginPath();ctx.arc(p.sx,p.sy,st.view*ppu,0,Math.PI*2);ctx.stroke()}" +
		      "function draw(){ctx.setTransform(devicePixelRatio,0,0,devicePixelRatio,0,0);ctx.clearRect(0,0,innerWidth,innerHeight);if(st){let ts=st.tile,ulx=center.x-innerWidth/(2*ppu),uly=center.y-innerHeight/(2*ppu),brx=center.x+innerWidth/(2*ppu),bry=center.y+innerHeight/(2*ppu);for(let gy=Math.floor(uly/ts);gy<=Math.floor(br y/ts);gy++)for(let gx=Math.floor(ulx/ts);gx<=Math.floor(brx/ts);gx++){let dx=(gx*ts-center.x)*ppu+innerWidth/2,dy=(gy*ts-center.y)*ppu+innerHeight/2,im=img(st.seg,gx,gy);if(im.complete&&im.naturalWidth)ctx.drawImage(im,dx,dy,ts*ppu,ts*ppu);let ovs=[];if(layers.claims)ovs.push('cplot');if(layers.village)ovs.push('vlg');if(layers.realm)ovs.push('realm');for(let tag of ovs){let oi=ol(st.seg,gx,gy,tag);if(oi.complete&&oi.naturalWidth){ctx.globalAlpha=.55;ctx.drawImage(oi,dx,dy,ts*ppu,ts*ppu);ctx.globalAlpha=1}}}if(layers.grid)grid(ulx,uly,brx,bry,ts);if(layers.view)view();if(layers.markers)for(let m of st.markers){let p=stamp('marker',m,3);if(layers.labels&&ppu>=2.5)label(m.name,p)}if(layers.icons)for(let i of st.icons){let p=stamp('icon',i,4);if(layers.labels&&ppu>=2.5)label(i.name,p)}if(layers.party)for(let pty of st.party)dot(pty.x,pty.y,5,pty.color);if(st.player)dot(st.player.x,st.player.y,6,'#ff4040')}requestAnimationFrame(draw)}draw();" +
		      "</script></body></html>").replace("br y", "bry").getBytes(StandardCharsets.UTF_8);
	send(ex, 200, "text/html; charset=utf-8", data);
    }

    private static void state(HttpExchange ex) throws IOException {
	MapWnd map = current();
	if(map == null) {
	    send(ex, 404, "text/plain; charset=utf-8", "No active map window.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	String json;
	try {
	    synchronized(map.ui) {
		json = map.browserStateJson();
	    }
	} catch(Loading l) {
	    send(ex, 503, "text/plain; charset=utf-8", "Map is still loading.".getBytes(StandardCharsets.UTF_8));
	    return;
	} catch(RuntimeException e) {
	    e.printStackTrace(Debug.log);
	    send(ex, 500, "text/plain; charset=utf-8", "Could not read map state.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	send(ex, 200, "application/json; charset=utf-8", json.getBytes(StandardCharsets.UTF_8));
    }

    private static void tile(HttpExchange ex) throws IOException {
	serveTile(ex, false);
    }

    private static void overlay(HttpExchange ex) throws IOException {
	serveTile(ex, true);
    }

    private static void marker(HttpExchange ex) throws IOException {
	serveAsset(ex, true);
    }

    private static void icon(HttpExchange ex) throws IOException {
	serveAsset(ex, false);
    }

    private static void serveAsset(HttpExchange ex, boolean marker) throws IOException {
	MapWnd map = current();
	if(map == null) {
	    send(ex, 404, "text/plain; charset=utf-8", "No active map window.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	Map<String, String> q = query(ex);
	byte[] data;
	try {
	    String id = required(q, "id");
	    String key = (marker ? "a:m:" : "a:i:") + id;
	    synchronized(tilecache) {
		data = tilecache.get(key);
	    }
	    if(data == null) {
		BufferedImage img;
		synchronized(map.ui) {
		    if(marker)
			img = map.browserMarkerImage(Integer.parseInt(id));
		    else
			img = map.browserIconImage(Long.parseLong(id));
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		ImageIO.write(img, "png", buf);
		data = buf.toByteArray();
		synchronized(tilecache) {
		    tilecache.put(key, data);
		}
	    }
	} catch(IllegalArgumentException e) {
	    send(ex, 400, "text/plain; charset=utf-8", "Bad asset request.".getBytes(StandardCharsets.UTF_8));
	    return;
	} catch(Loading l) {
	    send(ex, 503, "text/plain; charset=utf-8", "Map asset is still loading.".getBytes(StandardCharsets.UTF_8));
	    return;
	} catch(RuntimeException e) {
	    e.printStackTrace(Debug.log);
	    send(ex, 500, "text/plain; charset=utf-8", "Could not render map asset.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	send(ex, 200, "image/png", data, true);
    }

    private static void serveTile(HttpExchange ex, boolean overlay) throws IOException {
	MapWnd map = current();
	if(map == null) {
	    send(ex, 404, "text/plain; charset=utf-8", "No active map window.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	Map<String, String> q = query(ex);
	byte[] data;
	try {
	    long seg = Long.parseUnsignedLong(required(q, "seg"));
	    int x = Integer.parseInt(required(q, "x"));
	    int y = Integer.parseInt(required(q, "y"));
	    int lvl = Integer.parseInt(q.getOrDefault("lvl", "0"));
	    String tag = overlay ? required(q, "tag") : "";
	    String key = (overlay ? "o:" + tag : "m") + ":" + seg + ":" + lvl + ":" + x + ":" + y + ":" + CFG.PVP_MAP.get() + ":" + CFG.REMOVE_BIOME_BORDER_FROM_MINIMAP.get();
	    synchronized(tilecache) {
		data = tilecache.get(key);
	    }
	    if(data == null) {
		BufferedImage img;
		synchronized(map.ui) {
		    if(overlay)
			img = map.browserOverlayTile(seg, lvl, Coord.of(x, y), tag);
		    else
			img = map.browserTile(seg, lvl, Coord.of(x, y));
		}
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		ImageIO.write(img, "png", buf);
		data = buf.toByteArray();
		synchronized(tilecache) {
		    tilecache.put(key, data);
		}
	    }
	} catch(IllegalArgumentException e) {
	    send(ex, 400, "text/plain; charset=utf-8", "Bad tile request.".getBytes(StandardCharsets.UTF_8));
	    return;
	} catch(Loading l) {
	    send(ex, 503, "text/plain; charset=utf-8", "Map tile is still loading.".getBytes(StandardCharsets.UTF_8));
	    return;
	} catch(RuntimeException e) {
	    e.printStackTrace(Debug.log);
	    send(ex, 500, "text/plain; charset=utf-8", "Could not render map tile.".getBytes(StandardCharsets.UTF_8));
	    return;
	}
	send(ex, 200, "image/png", data, true);
    }

    private static String required(Map<String, String> q, String name) {
	String ret = q.get(name);
	if(ret == null)
	    throw(new IllegalArgumentException(name));
	return(ret);
    }

    private static Map<String, String> query(HttpExchange ex) {
	Map<String, String> ret = new HashMap<>();
	String raw = ex.getRequestURI().getRawQuery();
	if(raw == null)
	    return(ret);
	for(String part : raw.split("&")) {
	    int p = part.indexOf('=');
	    if(p >= 0)
		ret.put(part.substring(0, p), part.substring(p + 1));
	}
	return(ret);
    }

    private static void send(HttpExchange ex, int code, String type, byte[] data) throws IOException {
	send(ex, code, type, data, false);
    }

    private static void send(HttpExchange ex, int code, String type, byte[] data, boolean cache) throws IOException {
	ex.getResponseHeaders().set("Content-Type", type);
	if(cache)
	    ex.getResponseHeaders().set("Cache-Control", "private, max-age=86400, immutable");
	else
	    ex.getResponseHeaders().set("Cache-Control", "no-store, max-age=0");
	ex.sendResponseHeaders(code, data.length);
	try(OutputStream out = ex.getResponseBody()) {
	    out.write(data);
	}
    }
}
