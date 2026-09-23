package haven;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class OreAndStoneCounter extends WindowX {
    private static final int RADIUS = 44;
    private static final int MAX_ROWS = 21;
    private static final Map<String, String> ORE_NAMES = new LinkedHashMap<>();

    static {
	ORE_NAMES.put("argentite", "Silvershine (Argentite)");
	ORE_NAMES.put("blackcoal", "Coal (Black Coal)");
	ORE_NAMES.put("cuprite", "Wine Glance (Cuprite)");
	ORE_NAMES.put("cassiterite", "Cassiterite");
	ORE_NAMES.put("chalcopyrite", "Chalcopyrite");
	ORE_NAMES.put("hematite", "Bloodstone (Hematite)");
	ORE_NAMES.put("hornsilver", "Horn Silver");
	ORE_NAMES.put("ilmenite", "Heavy Earth (Ilmenite)");
	ORE_NAMES.put("leadglance", "Lead Glance");
	ORE_NAMES.put("limonite", "Iron Ochre (Limonite)");
	ORE_NAMES.put("malachite", "Malachite");
	ORE_NAMES.put("magnetite", "Black Ore (Magnetite)");
	ORE_NAMES.put("nagyagite", "Leaf Ore (Nagyagite)");
	ORE_NAMES.put("peacockore", "Peacock Ore");
	ORE_NAMES.put("petzite", "Direvein (Petzite)");
	ORE_NAMES.put("sylvanite", "Schrifterz (Sylvanite)");
	ORE_NAMES.put("galena", "Galena");
    }

    private final GameUI gui;
    private final List<Widget> rows = new ArrayList<>();
    private final Map<String, Integer> scanOres = new TreeMap<>();
    private final Map<String, Integer> scanStones = new TreeMap<>();
    private double nextScan;
    private Coord scanCenter;
    private int scanIndex;

    public OreAndStoneCounter(GameUI gui) {
	super(UI.scale(250, 35), "Ore & Stone Counter");
	this.gui = gui;
	justclose = true;
    }

    public void tick(double dt) {
	super.tick(dt);
	double now = Utils.rtime();
	if(scanCenter == null && now >= nextScan) {
	    nextScan = now + 5.0;
	    startScan();
	}
	if(scanCenter != null)
	    scanBatch();
    }

    private void startScan() {
	Gob player = (gui.map == null) ? null : gui.map.player();
	if(player == null)
	    return;
	scanOres.clear();
	scanStones.clear();
	scanCenter = player.rc.floor(MCache.tilesz);
	scanIndex = 0;
    }

    private void scanBatch() {
	int width = (RADIUS * 2) + 1;
	int total = width * width;
	for(int end = Math.min(total, scanIndex + 512); scanIndex < end; scanIndex++) {
	    int x = (scanIndex % width) - RADIUS;
	    int y = (scanIndex / width) - RADIUS;
		try {
		    int id = gui.ui.sess.glob.map.gettile(scanCenter.add(x, y));
		    Resource res = gui.ui.sess.glob.map.tilesetr(id);
		    if(res != null && res.name.startsWith("gfx/tiles/rocks/")) {
			String base = res.basename();
			String ore = ORE_NAMES.get(base);
			Map<String, Integer> target = (ore == null) ? scanStones : scanOres;
			String name = (ore == null) ? pretty(base) : ore;
			target.put(name, target.getOrDefault(name, 0) + 1);
		    }
		} catch(Loading ignored) {
		}
	}
	if(scanIndex < total)
	    return;
	scanCenter = null;
	updateRows();
    }

    private void updateRows() {
	rows.forEach(Widget::reqdestroy);
	rows.clear();
	int y = 0;
	for(Map.Entry<String, Integer> entry : scanOres.entrySet())
	    y = addRow(entry, true, y);
	for(Map.Entry<String, Integer> entry : scanStones.entrySet()) {
	    if((rows.size() / 2) >= MAX_ROWS)
		break;
	    y = addRow(entry, false, y);
	}
	resize(UI.scale(250), Math.max(UI.scale(35), y));
    }

    private int addRow(Map.Entry<String, Integer> entry, boolean ore, int y) {
	if((rows.size() / 2) >= MAX_ROWS)
	    return y;
	Label name = add(new Label(entry.getKey()), UI.scale(5), y);
	if(ore)
	    name.setcolor(Color.YELLOW);
	Label count = add(new Label(Integer.toString(entry.getValue())), UI.scale(205), y);
	if(ore && entry.getValue() > 50)
	    count.setcolor(Color.RED);
	rows.add(name);
	rows.add(count);
	return y + UI.scale(18);
    }

    private static String pretty(String name) {
	if(name.isEmpty())
	    return name;
	return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    public void destroy() {
	Utils.setprefc("wndc-ore-stone-counter", c);
	if(gui.oreAndStoneCounter == this)
	    gui.oreAndStoneCounter = null;
	super.destroy();
    }
}
