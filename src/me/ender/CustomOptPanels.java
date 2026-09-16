package me.ender;

import haven.*;
import me.ender.ui.CFGBox;
import me.ender.ui.CFGSlider;

import static haven.OptWnd.*;

public class CustomOptPanels {
    private static final int STEP = UI.scale(25);
    private static final int H_STEP = UI.scale(10);
    private static final int COL_WIDTH = UI.scale(230);

    public static Button guiLockButton(int w) {
	return new GUILockButton(w);
    }
    
    public static void initColorPanel(OptWnd wnd, OptWnd.Panel panel) {
	int START;
	int x, y;
	int my = 0, tx;
	Widget w;
	
	Widget title = panel.add(new Label("Color settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
	
	x = 0;
	y = START;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_OVERLAY, "Mine support overlay", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_SINGLE_OVERLAY, "Mine support (single) overlay", true), x, y);

	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_VIRTUAL_OVERLAY, "Mine support build preview", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_MINE_SUPPORT_DAMAGED_OVERLAY, "Damaged mine support overlay", true), x, y);

	y+=STEP;
	y = addSlider(CFG.MINE_SUPPORT_DANGER_THRESHOLD, 0, 100, "Mine support danger threshold %d%% HP:", "Mine support with less than this HP threshold will be considered dangerous.", panel, x, y, STEP);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_TILE_GRID, "Tile grid", true), x, y);
	
	y += STEP;
	panel.add(new Label("Hit Box:"), x, y);
	
	y += STEP;
	tx = x;
	tx += panel.add(new CFGColorBtn(CFG.COLOR_HBOX_SOLID, "Solid", true), tx + H_STEP, y).sz.x;
	tx += H_STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_HBOX_PASSABLE, "Passable", true), tx + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Fill in solid hit boxes:", CFG.DISPLAY_GOB_HITBOX_FILLED), x, y);
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_HBOX_FILLED, "With color", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_RIDGE_BOX, "Ridge highlight", true), x, y);
	
	y += STEP;
	panel.add(new Label("Aura colors:"), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_SPEED_BUFF, "Speed Buff", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_RABBIT, "Rabbits", true), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_CRITTERS, "Critters", true), x + H_STEP, y);
	
	my = Math.max(my, y);
	
	x += COL_WIDTH;
	y = START;
	
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_READY, "Workstation ready", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_FULL, "Container: Full", true), x, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_EMPTY, "Container: Empty", true), x, y);
	
	y += STEP;
	
	//combat
	panel.add(new Label("Combat highlights:"), x, y);
	
	tx = x + H_STEP;
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_SELF, "Self", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_LEADER, "Leader", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_PARTY, "Party", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_IN_COMBAT, "Enemy", true), tx, y);
	
	y += STEP;
	panel.add(new CFGColorBtn(CFG.COLOR_GOB_COMBAT_TARGET, "Current target", true), tx, y);
	
	my = Math.max(my, y);
	
	w = panel.add(wnd.new PButton(UI.scale(200), "Back", 27, wnd.main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
	w.c.x = (panel.sz.x - w.sz.x) / 2;
    }
    
    public static void initCombatPanel(OptWnd wnd, OptWnd.Panel panel) {
	int START;
	int x, y;
	int my = 0, tx;
	Widget w;
	
	Widget title = panel.add(new Label("Combat settings", LBL_FNT), 0, 0);
	START = title.sz.y + UI.scale(10);
	
	x = 0;
	y = START;
	//first row
	panel.add(new CFGBox("Use new combat UI", CFG.ALT_COMBAT_UI), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Allow dragging combat UI", CFG.DRAG_COMBAT_UI, "Drag by cooldown circle"), x, y);
	y += STEP;
	panel.add(new CFGBox("Show combat UI even when not in combat", CFG.KEEP_COMBAT_UI_AFTER_COMBAT), x, y);
	y += STEP;
	Label inactiveScale = panel.add(new Label(String.format("Out of combat combat UI scale: %d%%", CFG.COMBAT_UI_INACTIVE_SCALE.get())), x + H_STEP, y);
	y += UI.scale(15);
	panel.add(new CFGSlider(UI.scale(150), 1, 100, CFG.COMBAT_UI_INACTIVE_SCALE, inactiveScale, "Out of combat combat UI scale: %d%%"), x + H_STEP, y);
	y += STEP;
	Label openingDecay = panel.add(new Label(String.format("Out of combat opening recovery: %.1f%%/s", CFG.COMBAT_UI_OPENING_DECAY.get() / 10.0)), x + H_STEP, y);
	y += UI.scale(15);
	panel.add(new CFGSlider(UI.scale(150), 0, 50, CFG.COMBAT_UI_OPENING_DECAY, openingDecay, "") {
	    protected void updateLabel() {
		label.settext(String.format("Out of combat opening recovery: %.1f%%/s", val / 10.0));
	    }
	}, x + H_STEP, y);
	y += STEP;
	panel.add(new Button(UI.scale(150), "Reset combat UI position", false), x + H_STEP, y)
	    .action(() -> Fightsess.resetOffset(wnd.ui));
	y += STEP;
	
	y += STEP;
	panel.add(new CFGBox("Auto peace on combat start", CFG.COMBAT_AUTO_PEACE , "Automatically enter peaceful mode on combat start id enemy is aggressive - useful for taming"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Re-aggro animals", CFG.COMBAT_RE_AGGRO), x, y).settip("Automatically re-start combat with animals that dropped out of combat with you.\nOnly really useful if 'Auto peace on combat start' is enabled.", true);

	y += STEP;
	panel.add(new CFGBox("Disable animal warning while in combat", CFG.HIDE_ANIMAL_WARNING_IN_COMBAT, "Hides the aggressive animal warning circle for animals you are currently fighting."), x, y);

	y += STEP;
	panel.add(new CFGBox("Block attacks on tamed horses", CFG.BLOCK_ATTACK_TAMED_HORSE, "Prevents the attack-cursor click from being sent when the target is a tamed horse - useful to avoid hitting party members' horses during hunts."), x, y);

	y += STEP;
	panel.add(new CFGBox("Always mark current target", CFG.ALWAYS_MARK_COMBAT_TARGET , "Usually current target only marked when there's more than one"), x, y);

	y += STEP;
	Label markerRadius = panel.add(new Label(String.format("Combat marker radius: %d", CFG.COMBAT_MARKER_RADIUS.get())), x + H_STEP, y);
	y += UI.scale(15);
	panel.add(new CFGSlider(UI.scale(150), 3, 12, CFG.COMBAT_MARKER_RADIUS, markerRadius, "Combat marker radius: %d") {
	    public void changed() {
		super.changed();
		if(wnd.ui != null && wnd.ui.sess != null && wnd.ui.sess.glob != null)
		    wnd.ui.sess.glob.oc.gobAction(Gob::markerUpdated);
	    }
	}, x + H_STEP, y);
	
	y = AddCombatHighlight(panel, x, y, "Highlight party members in combat", CFG.HIGHLIGHT_PARTY_IN_COMBAT, CFG.MARK_PARTY_IN_COMBAT);
	y = AddCombatHighlight(panel, x, y, "Highlight self in combat", CFG.HIGHLIGHT_SELF_IN_COMBAT, CFG.MARK_SELF_IN_COMBAT);
	y = AddCombatHighlight(panel, x, y, "Highlight enemies in combat", CFG.HIGHLIGHT_ENEMY_IN_COMBAT, CFG.MARK_ENEMY_IN_COMBAT);
	
	my = Math.max(my, y);
	
	//second row
	x += COL_WIDTH;
	y = START;
	
	panel.add(new CFGBox("Show combat info", CFG.SHOW_COMBAT_INFO, "Will display initiative points and openings over gobs that you are fighting"), x, y);
	
	y += STEP;
	Label label = panel.add(new Label(String.format("Combat info vertical offset: %d", CFG.SHOW_COMBAT_INFO_HEIGHT.get())), x + H_STEP, y);
	y += UI.scale(15);
	panel.add(new CFGSlider(UI.scale(150), 1, 35, CFG.SHOW_COMBAT_INFO_HEIGHT, label, "Combat info vertical offset: %d"), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Show attack range", CFG.SHOW_ATTACK_RANGE, "Displays equipped weapon range and current target distance in the combat UI."), x, y);

	y += STEP;
	panel.add(refreshGobMarkers(new CFGBox("Range circle: Self", CFG.SHOW_ATTACK_RANGE_SELF)), x + H_STEP, y);
	
	y += STEP;
	panel.add(refreshGobMarkers(new CFGBox("Range circle: Party", CFG.SHOW_ATTACK_RANGE_PARTY)), x + H_STEP, y);
	
	y += STEP;
	panel.add(refreshGobMarkers(new CFGBox("Range circle: Enemies", CFG.SHOW_ATTACK_RANGE_ENEMY)), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("Simplified combat openings", CFG.SIMPLE_COMBAT_OPENINGS, "Show openings as solid colors with numbers"), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Display combat keys", CFG.SHOW_COMBAT_KEYS), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Disable menu keys while in combat", CFG.DISABLE_MENU_KEYS_IN_COMBAT, "Prevents the bottom-right action menu hotkeys from firing while you are in combat."), x, y);

	y += STEP;
	panel.add(new CFGBox("Show combat damage", CFG.SHOW_COMBAT_DMG), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Clear player damage after combat", CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Clear all damage after combat", CFG.CLEAR_ALL_DMG_AFTER_COMBAT), x, y);

	y += STEP;
	panel.add(autoReducerStartButton(UI.scale(200)), x, y);

	y += STEP;
	panel.add(new Button(UI.scale(200), "Guarded skills...", false)
	    .action(() -> GuardedCombatSkills.openSettings(wnd.ui)), x, y);

	y += STEP;
	panel.add(new Button(UI.scale(200), "Combat debug...", false)
	    .action(() -> openCombatDebug(wnd.ui)), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Show draggable HP/Stamina/Energy bars", CFG.SHOW_FLOATING_STAT_WDGS), x, y);
	
	y += STEP;
	panel.add(new CFGBox("Only during combat", CFG.SHOW_FLOATING_STATS_COMBAT), x + H_STEP, y);
	
	
	my = Math.max(my, y);
	
	w = panel.add(wnd.new PButton(UI.scale(200), "Back", 27, wnd.main), new Coord(0, my + UI.scale(35)));
	panel.pack();
	title.c.x = (panel.sz.x - title.sz.x) / 2;
	w.c.x = (panel.sz.x - w.sz.x) / 2;
    }
    
    private static int AddCombatHighlight(OptWnd.Panel panel, int x, int y, String name, CFG<Boolean> highlight, CFG<Boolean> mark) {
	y += STEP;
	panel.add(new Label(name), x, y);
	
	y += STEP;
	panel.add(new CFGBox("By coloring", highlight), x + H_STEP, y);
	
	y += STEP;
	panel.add(new CFGBox("By ring", mark), x + H_STEP, y);
	
	return y;
    }

    private static CFGBox refreshGobMarkers(CFGBox box) {
	box.set = value -> {
	    if(box.ui != null && box.ui.sess != null && box.ui.sess.glob != null)
		box.ui.sess.glob.oc.gobAction(Gob::markerUpdated);
	};
	return box;
    }

    private static Button autoReducerStartButton(int w) {
	Button btn = new Button(w, autoReducerStartLabel(), false) {
	    public void click() {
		CombatReducerMode mode = CFG.AUTO_COMBAT_REDUCER_START.get();
		CFG.AUTO_COMBAT_REDUCER_START.set(mode == CombatReducerMode.OFF ? CombatReducerMode.SEMI :
		    mode == CombatReducerMode.SEMI ? CombatReducerMode.ON : CombatReducerMode.OFF);
		change(autoReducerStartLabel());
	    }
	};
	btn.settip("Starting state for Auto combat reducer when combat begins.");
	return btn;
    }

    private static String autoReducerStartLabel() {
	return "Auto reducer start: " + CFG.AUTO_COMBAT_REDUCER_START.get().label;
    }

    private static void openCombatDebug(UI ui) {
	if(ui == null || ui.gui == null)
	    return;
	ui.gui.add(new CombatDebugWindow(), ui.mc);
    }

    private static class CombatDebugWindow extends Window {
	CombatDebugWindow() {
	    super(UI.scale(new Coord(280, 105)), "Combat Debug");
	    justclose = true;
	    int y = UI.scale(8);
	    add(new Label("Debug options"), UI.scale(8), y);
	    y += STEP;
	    add(new CFGBox("Measure opening recovery", CFG.COMBAT_DEBUG_OPENING_RECOVERY,
		"Reports measured opening recovery after combat resumes from a saved out-of-combat sample."), UI.scale(10), y);
	    y += STEP + UI.scale(8);
	    Button close = add(new Button(UI.scale(80), "Close", false), UI.scale(10), y);
	    close.action(this::reqdestroy);
	}
    }

    private static class GUILockButton extends Button implements CFG.Observer<Boolean> {
	GUILockButton(int w) {
	    super(w, label(), false);
	    action(() -> {
		CFG.GUI_LOCK.set(!CFG.GUI_LOCK.get());
		update();
	    });
	    CFG.GUI_LOCK.observe(this);
	}

	private static String label() {
	    return CFG.GUI_LOCK.get() ? "GUI LOCK: ON" : "GUI LOCK: OFF";
	}

	private void update() {
	    change(label());
	}

	public void updated(CFG<Boolean> cfg) {
	    update();
	}

	public void destroy() {
	    CFG.GUI_LOCK.unobserve(this);
	    super.destroy();
	}
    }
}
