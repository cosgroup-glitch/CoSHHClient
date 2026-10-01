package haven;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExploredAreaRenderer {
    private static final int MAX_TEXTURES = 256;

    private static class Key {
	final ExploredArea area;
	final long segment;
	final Coord grid;
	final boolean session;

	Key(ExploredArea area, long segment, Coord grid, boolean session) {
	    this.area = area;
	    this.segment = segment;
	    this.grid = grid;
	    this.session = session;
	}

	public boolean equals(Object o) {
	    if(!(o instanceof Key))
		return(false);
	    Key that = (Key)o;
	    return((area == that.area) && (segment == that.segment) &&
		(session == that.session) && grid.equals(that.grid));
	}

	public int hashCode() {
	    int ret = (System.identityHashCode(area) * 31) + Long.hashCode(segment);
	    return((((ret * 31) + grid.hashCode()) * 31) + (session ? 1 : 0));
	}
    }

    private static class Cached {
	final long version;
	final Tex texture;

	Cached(long version, Tex texture) {
	    this.version = version;
	    this.texture = texture;
	}
    }

    private static final Map<Key, Cached> cache = new LinkedHashMap<Key, Cached>(64, 0.75f, true) {
	protected boolean removeEldestEntry(Map.Entry<Key, Cached> eldest) {
	    if(size() <= MAX_TEXTURES)
		return(false);
	    eldest.getValue().texture.dispose();
	    return(true);
	}
    };

    public static void clear(ExploredArea area) {
	java.util.Iterator<Map.Entry<Key, Cached>> entries = cache.entrySet().iterator();
	while(entries.hasNext()) {
	    Map.Entry<Key, Cached> entry = entries.next();
	    if(entry.getKey().area == area) {
		entry.getValue().texture.dispose();
		entries.remove();
	    }
	}
    }

    public static void draw(MiniMap map, GOut g) {
	if(!CFG.MMAP_EXPLORED.get() || (map.ui == null) || (map.ui.gui == null) ||
	   (map.ui.gui.exploredArea == null) || (map.dloc == null) || (map.dgext == null))
	    return;
	ExploredArea area = map.ui.gui.exploredArea;
	Color exploredColor = opacity(CFG.MMAP_EXPLORED_COLOR.get(), CFG.MMAP_EXPLORED_OPACITY.get());
	Color sessionColor = opacity(CFG.MMAP_EXPLORED_SESSION_COLOR.get(), CFG.MMAP_EXPLORED_SESSION_OPACITY.get());
	int scale = 1 << map.dlvl;
	boolean session = area.sessionActive();
	for(Coord grid : area.grids(map.dloc.seg.id)) {
	    Coord dc = grid.div(scale);
	    if(!map.dgext.contains(dc) || (map.display[map.dgext.ri(dc)] == null))
		continue;
	    drawGrid(map, g, area, grid, false, exploredColor);
	    if(session)
		drawGrid(map, g, area, grid, true, sessionColor);
	}
	g.chcolor();
    }

    private static Color opacity(Color color, int percent) {
	int alpha = (Utils.clip(percent, 0, 100) * 255) / 100;
	return(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
    }

    private static void drawGrid(MiniMap map, GOut g, ExploredArea area, Coord grid,
				 boolean session, Color color) {
	long segment = map.dloc.seg.id;
	long version = area.version(segment, grid, session);
	if(version < 0)
	    return;
	Key key = new Key(area, segment, grid, session);
	Cached cached = cache.get(key);
	if((cached == null) || (cached.version != version)) {
	    BitSet mask = area.mask(segment, grid, session);
	    if(mask == null)
		return;
	    Tex texture = texture(mask);
	    if(cached != null)
		cached.texture.dispose();
	    cached = new Cached(version, texture);
	    cache.put(key, cached);
	}
	Coord tile = grid.mul(MCache.cmaps);
	Coord ul = map.xlate(new MiniMap.Location(map.dloc.seg, tile));
	Coord br = map.xlate(new MiniMap.Location(map.dloc.seg, tile.add(MCache.cmaps)));
	if((ul == null) || (br == null))
	    return;
	g.chcolor(color);
	g.image(cached.texture, ul, br.sub(ul));
    }

    private static Tex texture(BitSet mask) {
	BufferedImage image = TexI.mkbuf(MCache.cmaps);
	for(int bit = mask.nextSetBit(0); bit >= 0; bit = mask.nextSetBit(bit + 1)) {
	    int x = bit % MCache.cmaps.x;
	    int y = bit / MCache.cmaps.x;
	    image.setRGB(x, y, 0xffffffff);
	}
	return(new TexI(image));
    }
}
