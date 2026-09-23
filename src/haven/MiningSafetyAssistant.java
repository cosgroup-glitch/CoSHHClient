package haven;

import haven.res.gfx.fx.mscover.Global;
import me.ender.ui.CFGBox;

import java.util.Map;
import java.util.WeakHashMap;

public class MiningSafetyAssistant extends WindowX {
    private static final Map<GameUI, State> states = new WeakHashMap<>();
    private final GameUI gui;

    private static class State {
	double nextCheck;
	double lastStop;
    }

    public MiningSafetyAssistant(GameUI gui) {
	super(Coord.z, "Mining Safety Assistant");
	this.gui = gui;
	justclose = true;
	Widget prev = add(new CFGBox("Prevent unsupported area selection", CFG.MINING_PREVENT_UNSAFE_SELECTION,
	    "Blocks a mining selection if any selected tile is outside visible, real support coverage."), 0, 0);
	prev = add(new CFGBox("Stop mining outside support", CFG.MINING_STOP_UNSUPPORTED), prev.pos("bl").adds(0, 5));
	prev = add(new CFGBox("Stop mining below 50% support", CFG.MINING_STOP_SUPPORT_50), prev.pos("bl").adds(0, 5));
	prev = add(new CFGBox("Stop mining below 25% support", CFG.MINING_STOP_SUPPORT_25), prev.pos("bl").adds(0, 5));
	add(new CFGBox("Stop mining near loose rock", CFG.MINING_STOP_LOOSE_ROCK,
	    "Stops when a visible loose rock is within about 11 tiles of the tile being mined."), prev.pos("bl").adds(0, 5));
	pack();
    }

    public void destroy() {
	Utils.setprefc("wndc-mining-safety", c);
	if(gui.miningSafetyAssistant == this)
	    gui.miningSafetyAssistant = null;
	super.destroy();
    }

    public static boolean allowSelection(GameUI gui, Coord one, Coord two) {
	if(!CFG.MINING_PREVENT_UNSAFE_SELECTION.get())
	    return true;
	Coord ul = new Coord(Math.min(one.x, two.x), Math.min(one.y, two.y));
	Coord br = new Coord(Math.max(one.x, two.x), Math.max(one.y, two.y)).add(1, 1);
	boolean safe = Global.get(gui.ui.sess.glob).supports(Area.corn(ul, br));
	if(!safe)
	    gui.error("Mining selection extends outside visible support coverage.");
	return safe;
    }

    public static void monitor(GameUI gui) {
	if(!CFG.MINING_STOP_UNSUPPORTED.get() && !CFG.MINING_STOP_SUPPORT_50.get() &&
	   !CFG.MINING_STOP_SUPPORT_25.get() && !CFG.MINING_STOP_LOOSE_ROCK.get())
	    return;
	double now = Utils.rtime();
	State state;
	synchronized(states) {
	    state = states.computeIfAbsent(gui, key -> new State());
	}
	if(now < state.nextCheck)
	    return;
	state.nextCheck = now + 0.2;
	Gob player = (gui.map == null) ? null : gui.map.player();
	if(player == null || !player.hasPose("pickan", "choppan"))
	    return;
	Coord2d target = player.rc.add(Math.cos(player.a) * 13.75, Math.sin(player.a) * 13.75);
	Coord tc = target.floor(MCache.tilesz);
	double health = Global.get(gui.ui.sess.glob).supportHealth(tc);
	String reason = null;
	if(CFG.MINING_STOP_UNSUPPORTED.get() && health < 0) {
	    reason = "Stopped mining outside support coverage.";
	} else {
	    double threshold = CFG.MINING_STOP_SUPPORT_50.get() ? 0.5 :
		(CFG.MINING_STOP_SUPPORT_25.get() ? 0.25 : -1);
	    if(health >= 0 && threshold >= 0 && health <= threshold)
		reason = String.format("Stopped mining under a support at %.0f%% health.", health * 100);
	}
	if(reason == null && CFG.MINING_STOP_LOOSE_ROCK.get()) {
	    synchronized(gui.ui.sess.glob.oc) {
		for(Gob gob : gui.ui.sess.glob.oc) {
		    try {
			if(gob.getres() != null && gob.getres().name.equals("gfx/terobjs/looserock") && gob.rc.dist(target) <= 125) {
			    gob.highlight();
			    reason = "Stopped mining near a loose rock.";
			    break;
			}
		    } catch(Loading ignored) {
		    }
		}
	    }
	}
	if(reason != null && now - state.lastStop > 1.0) {
	    state.lastStop = now;
	    gui.ui.root.wdgmsg("gk", 27);
	    gui.error(reason);
	}
    }
}
