package me.ender;

import auto.Actions;
import auto.InventorySorter;
import haven.*;
import haven.rx.CharterBook;
import haven.rx.Reactor;
import me.ender.ui.CFGBox;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import static me.ender.ItemHelpers.*;

public class WindowDetector {
    public static final String WND_STUDY = "Study";
    public static final String WND_TABLE = "Table";
    public static final String WND_CHARACTER_SHEET = "Character Sheet";
    public static final String WND_SMELTER = "Ore Smelter";
    public static final String WND_FINERY_FORGE = "Finery Forge";
    public static final String WND_STACK_FURNACE = "Stack furnace";
    public static final String WND_CHANGE_NAME = "Change Name";
    
    private static final Object lock = new Object();
    private static final Set<Window> toDetect = new HashSet<>();
    private static final Set<Window> detected = new HashSet<>();
    private static final Object tableFoodStatLock = new Object();
    private static Window activeTableFoodStatWindow = null;
    private static String activeTableFoodStat = null;
    private static final Map<Window, List<TableFoodStatButton>> tableFoodStatButtons = new WeakHashMap<>();
    private static final Color TABLE_FOOD_STAT_ACTIVE = Color.WHITE;
    private static final Color TABLE_FOOD_STAT_HIGH = new Color(64, 255, 96);
    private static final Color TABLE_FOOD_STAT_MID = new Color(255, 230, 64);
    private static final Color TABLE_FOOD_STAT_LOW = new Color(255, 64, 64);
    private static final String[][] TABLE_FOOD_STATS = {
	{"str", "Strength"},
	{"agi", "Agility"},
	{"int", "Intelligence"},
	{"con", "Constitution"},
	{"prc", "Perception"},
	{"csm", "Charisma"},
	{"dex", "Dexterity"},
	{"wil", "Will"},
	{"psy", "Psyche"},
    };
    
    static {
	Reactor.WINDOW.subscribe(WindowDetector::onWindowEvent);
    }

    public static void process(Widget wdg, Widget parent) {
	if(wdg instanceof Window) {
	    detect((Window) wdg);
	    // Namechange never fires ON_PACK so recognize() can't catch it —
	    // install the watcher straight from process().
	    if(WND_CHANGE_NAME.equals(((Window) wdg).caption())) {
		CharNameCapture.install((Window) wdg);
	    }
	}
	untranslate(wdg, parent);
    }

    public static void detect(Window window) {
	synchronized (toDetect) {
	    toDetect.add(window);
	}
    }

    private static void onWindowEvent(Pair<Window, String> event) {
	synchronized (lock) {
	    Window window = event.a;
	    if(toDetect.contains(window)) {
		String eventName = event.b;
		switch (eventName) {
		    case Window.ON_DESTROY:
			// Wizard closed → name committed; promote before any
			// first-spawn events.
			if(WND_CHANGE_NAME.equals(window.caption())) {
			    CharNameCapture.promote();
			}
			toDetect.remove(window);
			detected.remove(window);
			synchronized (tableFoodStatLock) {
			    if(activeTableFoodStatWindow == window) {
				activeTableFoodStatWindow = null;
				activeTableFoodStat = null;
			    }
			}
			synchronized (tableFoodStatButtons) {
			    tableFoodStatButtons.remove(window);
			}
			updateTableFoodStatButtons();
			break;
		    //Detect window on 'pack' message - this is last message server sends after constructing a window
		    case Window.ON_PACK:
			if(!detected.contains(window)) {
			    detected.add(window);
			    recognize(window);
			}
			break;
		}
	    }
	}
    }

    private static void recognize(Window window) {
	if(isWindowType(window, WND_TABLE)) {
	    extendTableWindow(window);
	} else {
	    AnimalFarm.processCattleInfo(window);
	}
    }

    private static void untranslate(Widget wdg, Widget parent) {
	Label lbl;
	if(parent instanceof Window) {
	    Window window = (Window) parent;
	    String caption = window.caption();
	    if("Milestone".equals(caption) && wdg instanceof Label) {
		lbl  = (Label) wdg;
		if(!lbl.original.equals("Make new trail:")) {
		    lbl.i10n(false);
		}
	    } else if(isProspecting(caption)) {
		if(wdg instanceof Label) {
		    lbl = (Label) wdg;
		    ((ProspectingWnd) parent).text(lbl.original);
		} else if(wdg instanceof Button) {
		    ((Button) wdg).large(false);
		}
	    }
	}
    }

    public static Widget newWindow(Coord sz, String title, boolean lg) {
	if(isPortal(title)) {
	    return new CharterBook(sz, title, lg);
	} else if(isProspecting(title)) {
	    return new ProspectingWnd(sz, title);
	}
	return (new WindowX(sz, title, lg));
    }
    
    public static String getWindowName(Widget wdg) {
	Window wnd;
	if(wdg == null) {return null;}
	if(wdg instanceof Window) {
	    wnd = (Window) wdg;
	} else {
	    wnd = wdg.getparent(Window.class);
	}
	return wnd == null ? null : wnd.caption();
    }
    
    public static boolean isWindowType(Widget wdg, String... types) {
	if(types == null || types.length == 0) {return false;}
	String wnd = getWindowName(wdg);
	if(wnd == null) {return false;}
	for (String type : types) {
	    if(Objects.equals(type, wnd)) {return true;}
	}
	
	return false;
    }
    
    public static boolean isPortal(String title) {
	return "Sublime Portico".equals(title) || "Charter Stone".equals(title);
    }
    
    public static boolean isBelt(String title) {
	return "Belt".equals(title);
    }
    
    public static boolean isProspecting(String title) {
	return "Prospecting".equals(title);
    }
    
    public static Color tableFoodStatOutline(WItem item) {
	String stat;
	synchronized (tableFoodStatLock) {
	    stat = activeTableFoodStat;
	}
	if(stat == null) {return null;}
	try {
	    double selected = 0;
	    double min = Double.POSITIVE_INFINITY;
	    double max = Double.NEGATIVE_INFINITY;
	    boolean found = false;
	    for (haven.resutil.FoodInfo food : ItemInfo.findall(haven.resutil.FoodInfo.class, item.item.info())) {
		for (haven.resutil.FoodInfo.Event ev : food.evs) {
		    if(ev.ev != null && tableFoodStatEventMatches(stat, ev.ev.nm)) {
			selected += ev.a;
			found = true;
		    }
		    if(ev.ev != null) {
			min = Math.min(min, ev.a);
			max = Math.max(max, ev.a);
		    }
		}
	    }
	    if(!found) {return null;}
	    if(selected >= max) {return TABLE_FOOD_STAT_HIGH;}
	    if(selected <= min) {return TABLE_FOOD_STAT_LOW;}
	    double ratio = (selected - min) / (max - min);
	    if(ratio < 0.4) {return TABLE_FOOD_STAT_LOW;}
	    if(ratio <= 0.6) {return TABLE_FOOD_STAT_MID;}
	    return TABLE_FOOD_STAT_HIGH;
	} catch (Loading ignored) {}
	return null;
    }

    private static void setTableFoodStat(Window wnd, String stat) {
	synchronized (tableFoodStatLock) {
	    if((activeTableFoodStatWindow == wnd) && Objects.equals(activeTableFoodStat, stat)) {
		activeTableFoodStatWindow = null;
		activeTableFoodStat = null;
	    } else {
		activeTableFoodStatWindow = wnd;
		activeTableFoodStat = stat;
	    }
	}
	updateTableFoodStatButtons();
    }

    private static boolean tableFoodStatEventMatches(String stat, String event) {
	if(event == null) {return false;}
	String ev = event.toLowerCase(Locale.ROOT);
	switch (stat) {
	    case "str": return ev.startsWith("strength") || ev.equals("str");
	    case "agi": return ev.startsWith("agility") || ev.equals("agi");
	    case "int": return ev.startsWith("intelligence") || ev.equals("int");
	    case "con": return ev.startsWith("constitution") || ev.equals("con");
	    case "prc": return ev.startsWith("perception") || ev.equals("prc");
	    case "csm": return ev.startsWith("charisma") || ev.equals("csm");
	    case "dex": return ev.startsWith("dexterity") || ev.equals("dex");
	    case "wil": return ev.startsWith("will") || ev.equals("wil");
	    case "psy": return ev.startsWith("psyche") || ev.equals("psy");
	}
	return false;
    }

    private static void updateTableFoodStatButtons() {
	String stat;
	synchronized (tableFoodStatLock) {
	    stat = activeTableFoodStat;
	}
	synchronized (tableFoodStatButtons) {
	    for(List<TableFoodStatButton> buttons : tableFoodStatButtons.values()) {
		for (TableFoodStatButton button : buttons) {
		    button.active(Objects.equals(button.stat, stat));
		}
	    }
	}
    }

    private static void extendTableWindow(Window wnd) {
	Inventory food = null;
	for (Inventory inventory : wnd.children(Inventory.class)) {
	    Coord isz = inventory.isz;
	    if(isz.equals(DISHES_SZ) || isz.equals(TABLECLOTH_SZ) || isz.equals(ALCHEMY_SZ)) {continue;}
	    food = inventory;
	    break;
	}
	if(food != null) {
	    Coord p = wnd.xlate(food.parentpos(wnd, food.pos("ur")), false);
	    final Inventory tmp = food;
	    wnd.adda(new IButton("gfx/hud/btn-sort", "", "-d", "-h"), p, 1, 1)
		.action(() -> InventorySorter.sort(tmp))
		.settip("Sort");
	}
	
	Button btn = findFeastButton(wnd);
	if(btn == null) {return;}
	if(food != null) {
	    addTableFoodStatButtons(wnd, food);
	}
	
	btn.c = wnd.add(new CFGBox("Preserve cutlery", CFG.PRESERVE_SYMBEL), btn.pos("ul"))
	    .settip("Prevent eating from this table if some of the cutlery is almost broken").pos("bl");
	
	wnd.add(new Button(55, "Salt All", false, () -> Actions.saltFood(wnd.ui.gui)), btn.pos("ur").adds(-55, -20))
	    .settip("Salt all food");
    }

    private static Button findFeastButton(Window wnd) {
	for(Button btn : wnd.children(Button.class)) {
	    if((btn.text != null) && Objects.equals(btn.text.text, "Feast!")) {
		return btn;
	    }
	}
	return null;
    }

    private static void addTableFoodStatButtons(Window wnd, Inventory food) {
	Coord fp = food.parentpos(wnd, Coord.z);
	Coord base = Coord.of(UI.scale(151), Math.max(UI.scale(0), fp.y - UI.scale(105)));
	Coord step = UI.scale(31, 22);
	List<TableFoodStatButton> buttons = new ArrayList<>();
	for(int i = 0; i < TABLE_FOOD_STATS.length; i++) {
	    String code = TABLE_FOOD_STATS[i][0];
	    String name = TABLE_FOOD_STATS[i][1];
	    Coord c = base.add(step.x * (i % 3), step.y * (i / 3));
	    buttons.add(wnd.add(new TableFoodStatButton(wnd, code, name), c));
	}
	synchronized (tableFoodStatButtons) {
	    tableFoodStatButtons.put(wnd, buttons);
	}
	updateTableFoodStatButtons();
    }

    private static class TableFoodStatButton extends Button {
	private final Window wnd;
	private final String stat;
	private final String label;
	private boolean active;

	private TableFoodStatButton(Window wnd, String stat, String name) {
	    super(UI.scale(29), stat, false);
	    this.wnd = wnd;
	    this.stat = stat;
	    this.label = stat;
	    settip("Highlight table food with " + name + " FEP");
	}

	public void click() {
	    setTableFoodStat(wnd, stat);
	}

	private void active(boolean active) {
	    if(this.active != active) {
		this.active = active;
		if(active) {
		    change(label, TABLE_FOOD_STAT_ACTIVE);
		} else {
		    change(label);
		}
	    }
	}
    }
}
