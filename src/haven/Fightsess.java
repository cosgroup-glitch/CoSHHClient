/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import haven.rx.Reactor;

import haven.render.*;
import me.ender.FakeDraggerWdg;

import java.io.PrintWriter;
import java.awt.*;
import java.util.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;

import static haven.KeyBinder.*;

public class Fightsess extends Widget {
    private static final Coord off = new Coord(UI.scale(32), UI.scale(32));
    private static final double SEMI_REDUCER_MIN_DISTANCE = 3.5;
    private static final Coord INACTIVE_DRAGGER_BASE_SZ = UI.scale(new Coord(360, 180));
    private static final Coord INACTIVE_DRAGGER_MIN_SZ = UI.scale(new Coord(120, 70));
    public static final Text.Foundry fnd = new Text.Foundry(Text.sans.deriveFont(Font.BOLD), 14);
    public static final Tex cdframe = Resource.loadtex("gfx/hud/combat/cool");
    public static final Tex actframe = Buff.frame;
    public static final Coord actframeo = Buff.imgoff;
    public static final Tex indframe = Resource.loadtex("gfx/hud/combat/indframe");
    public static final Coord indframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex indbframe = Resource.loadtex("gfx/hud/combat/indbframe");
    public static final Coord indbframeo = (indframe.sz().sub(off)).div(2);
    public static final Tex useframe = Resource.loadtex("gfx/hud/combat/lastframe");
    public static final Coord useframeo = (useframe.sz().sub(off)).div(2);
    public static final int actpitch = UI.scale(50);
    public static final KeyBinder.KeyBind[] keybinds = new KeyBinder.KeyBind[]{
	new KeyBinder.KeyBind(KeyEvent.VK_1, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_2, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_3, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_4, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_5, NONE),
	new KeyBinder.KeyBind(KeyEvent.VK_1, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_2, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_3, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_4, SHIFT),
	new KeyBinder.KeyBind(KeyEvent.VK_5, SHIFT),
    };
    public static KeyBinder.KeyBind reducerKeybind = new KeyBinder.KeyBind(KeyEvent.VK_A, NONE);
    public static KeyBinder.KeyBind targetClosestKeybind = new KeyBinder.KeyBind(KeyEvent.VK_S, NONE);
    public static KeyBinder.KeyBind guardedSkillsKeybind = new KeyBinder.KeyBind(KeyEvent.VK_G, SHIFT);
    public final Action[] actions;
    public int use = -1, useb = -1;
    public Coord pcc;
    public int pho;
    private Fightview fv;
    private static final String DRAGGER = "Fightsess:drag";
    private FakeDraggerWdg dragger = new FakeDraggerWdg(DRAGGER, CFG.DRAG_COMBAT_UI) {
	public boolean mousedown(MouseDownEvent ev) {
	    if(super.mousedown(ev))
		return(true);
	    return(!active && DraggableWidget.guiEditMode() && ev.c.isect(Coord.z, sz));
	}
    };
    private boolean active = true;
    private boolean ended = false;
    private boolean forcedestroy = false;
    private boolean reportedDecay = false;
    private CombatReducerMode reducerMode = CFG.AUTO_COMBAT_REDUCER_START.get();
    private double reducerTimer = 0;
    private QueuedGuardedSkill guardedSkill;
    private final Collection<InactiveBuff> inactivebuffs = new ArrayList<>();
    private static Collection<OpeningSample> lastOpeningSamples = Collections.emptyList();
    private static final double MIN_MEASURED_OPENING_DECAY = 0.5;
    private static final double MAX_MEASURED_OPENING_DECAY = 2.5;
    private static final double INACTIVE_FADE_TIME = 1.0;
    private double inactiveStart = 0;

    public static class Action {
	public final Indir<Resource> res;
	public double cs, ct;

	public Action(Indir<Resource> res) {
	    this.res = res;
	}
    }
    
    private static class InactiveBuff {
	final Indir<Resource> res;
	final Coord relc;
	final int ameter;
	final double start;
	final boolean enemy;
	
	InactiveBuff(Buff buff, Coord relc, boolean enemy) {
	    this.res = buff.res;
	    this.relc = relc;
	    this.ameter = buff.ameter();
	    this.start = Utils.rtime();
	    this.enemy = enemy;
	}
	
	int ameter() {
	    if(ameter < 0)
		return(-1);
	    double elapsed = Utils.rtime() - start;
	    return(Math.max(0, (int)Math.floor(ameter - (elapsed * openingDecayRate()))));
	}
	
	double fadeStart() {
	    double decay = openingDecayRate();
	    if(enemy || (ameter <= 0) || (decay <= 0))
		return(start);
	    return(start + (ameter / decay));
	}
    }
    
    private static class OpeningSample {
	final String side, resname;
	final int value;
	final double time;
	
	OpeningSample(String side, Buff buff, double time) {
	    this.side = side;
	    this.resname = buff.res.get().name;
	    this.value = buff.ameter();
	    this.time = time;
	}
	
	String key() {
	    return(side + ":" + resname);
	}
    }
    
    private static class OpeningMeasurement {
	final String text;
	final double rate;
	
	OpeningMeasurement(String text, double rate) {
	    this.text = text;
	    this.rate = rate;
	}
    }

    @RName("fsess")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    int nact = Utils.iv(args[0]);
	    return(new Fightsess(nact));
	}
    }

    @SuppressWarnings("unchecked")
    public Fightsess(int nact) {
	pho = -UI.scale(40);
	this.actions = new Action[nact];
    }

    protected void added() {
	fv = parent.getparent(GameUI.class).fv;
	presize();
	Cal calendar = ui.gui.calendar;
	calendar.hide();
	dragger.sz = cdframe.sz();
	parent.add(dragger, Coord.z);
    }
    
    @Override
    public void remove() {
	dragger.remove();
	super.remove();
    }
    
    public static void resetOffset(UI ui) {
	if(ui == null || ui.gui == null || ui.gui.fsess == null) {
	    WidgetCfg.reset(DRAGGER);
	} else {
	    ui.gui.fsess.dragger.reset();
	}
    }
    
    public void presize() {
	resize(parent.sz);
	pcc = sz.div(2);
    }
    
    private void updatepos() {
	MapView map;
	Gob pl;
	if(((map = getparent(GameUI.class).map) == null) || ((pl = map.player()) == null))
	    return;
	Coord3f raw = pl.placed.getc();
	if(raw == null)
	    return;
	pcc = map.screenxf(raw).round2();
	pho = (int)(map.screenxf(raw.add(0, 0, UI.scale(20))).round2().sub(pcc).y) - UI.scale(20);
    }

    private static class Effect implements RenderTree.Node {
	Sprite spr;
	RenderTree.Slot slot;
	boolean used = true;

	Effect(Sprite spr) {this.spr = spr;}

	public void added(RenderTree.Slot slot) {
	    slot.add(spr);
	}
    }

    private static final Resource tgtfx = Resource.local().loadwait("gfx/hud/combat/trgtarw");
    private final Collection<Effect> curfx = new ArrayList<>();

    private Effect fxon(long gobid, Resource fx, Effect cur) {
	MapView map = getparent(GameUI.class).map;
	Gob gob = ui.sess.glob.oc.getgob(gobid);
	if((map == null) || (gob == null))
	    return(null);
	Pipe.Op place;
	try {
	    place = gob.placed.curplace();
	} catch(Loading l) {
	    return(null);
	}
	if((cur == null) || (cur.slot == null)) {
	    try {
		cur = new Effect(Sprite.create(new Sprite.UIOwner(this), fx, Message.nil));
		cur.slot = map.basic.add(cur.spr, place);
	    } catch(Loading l) {
		return(null);
	    }
	    curfx.add(cur);
	} else {
	    cur.slot.cstate(place);
	}
	cur.used = true;
	return(cur);
    }

    public void tick(double dt) {
	for(Iterator<Effect> i = curfx.iterator(); i.hasNext();) {
	    Effect fx = i.next();
	    if(!fx.used) {
		if(fx.slot != null) {
		    fx.slot.remove();
		    fx.slot = null;
		}
		i.remove();
	    } else {
		fx.used = false;
		fx.spr.tick(dt);
	    }
	}
	autoCombatReducer(dt);
	releaseGuardedSkill();
    }

    private static final Map<String, String[]> REDUCERS = new HashMap<String, String[]>() {{
	put(Buff.OPEN_RED, new String[]{"paginae/atk/flex", "paginae/atk/yieldground", "paginae/atk/zigzag", "paginae/atk/artevade"});
	put(Buff.OPEN_YELLOW, new String[]{"paginae/atk/jump", "paginae/atk/regain", "paginae/atk/zigzag", "paginae/atk/artevade"});
	put(Buff.OPEN_BLUE, new String[]{"paginae/atk/flex", "paginae/atk/sidestep", "paginae/atk/watchmoves", "paginae/atk/artevade"});
	put(Buff.OPEN_GREEN, new String[]{"paginae/atk/fdodge", "paginae/atk/qdodge", "paginae/atk/regain", "paginae/atk/yieldground", "paginae/atk/artevade"});
    }};

    private static final String[] ANY_REDUCER = {
	"paginae/atk/flex", "paginae/atk/yieldground", "paginae/atk/jump", "paginae/atk/regain",
	"paginae/atk/sidestep", "paginae/atk/watchmoves", "paginae/atk/fdodge", "paginae/atk/qdodge",
	"paginae/atk/zigzag", "paginae/atk/artevade"
    };

    private void autoCombatReducer(double dt) {
	if(!active || fv == null || reducerMode == CombatReducerMode.OFF)
	    return;
	if(hasLoadedMove())
	    return;
	reducerTimer -= dt;
	if(reducerTimer > 0)
	    return;
	reducerTimer = 0.35;
	if(reducerMode == CombatReducerMode.SEMI && enemyTooCloseForSemiReducer())
	    return;
	OpeningState openings = ownOpenings();
	if(openings.biggest == null)
	    return;
	int action = reducerAction(openings);
	if(action >= 0)
	    wdgmsg("use", action, 1, 0);
    }

    private OpeningState ownOpenings() {
	OpeningState ret = new OpeningState();
	for(Buff buff : fv.buffs.children(Buff.class)) {
	    try {
		String name = buff.res.get().name;
		if(Buff.openingColor(name) == null)
		    continue;
		int value = buff.ameter();
		if(value > 0)
		    ret.put(name, value);
	    } catch(Loading ignored) {}
	}
	return ret;
    }

    private int reducerAction(OpeningState openings) {
	String[] prefs = reducerPrefs(openings);
	int ret = actionIndex(withDashFallback(prefs, openings));
	return ret >= 0 ? ret : actionIndex(ANY_REDUCER);
    }

    private String[] reducerPrefs(OpeningState openings) {
	if(Buff.OPEN_YELLOW.equals(openings.biggest) && openings.value(Buff.OPEN_RED) > 0) {
	    if(openings.value(Buff.OPEN_RED) >= 15)
		return new String[]{"paginae/atk/zigzag", "paginae/atk/jump", "paginae/atk/regain", "paginae/atk/artevade"};
	}
	return REDUCERS.get(openings.biggest);
    }

    private String[] withDashFallback(String[] prefs, OpeningState openings) {
	if(prefs == null || openings.count != 1)
	    return prefs;
	java.util.List<String> ret = new ArrayList<>();
	for(String pref : prefs) {
	    if("paginae/atk/artevade".equals(pref) && !ret.contains("paginae/atk/dash"))
		ret.add("paginae/atk/dash");
	    ret.add(pref);
	}
	if(!ret.contains("paginae/atk/dash"))
	    ret.add("paginae/atk/dash");
	return ret.toArray(new String[0]);
    }

    private static class OpeningState {
	final Map<String, Integer> values = new HashMap<>();
	String biggest = null;
	int biggestv = 0;
	int count = 0;

	void put(String name, int value) {
	    values.put(name, value);
	    count++;
	    if(value > biggestv) {
		biggest = name;
		biggestv = value;
	    }
	}

	int value(String name) {
	    return values.getOrDefault(name, 0);
	}
    }

    private int actionIndex(String[] names) {
	if(names == null)
	    return -1;
	double now = Utils.rtime();
	for(String name : names) {
	    for(int i = 0; i < actions.length; i++) {
		Action act = actions[i];
		if(act == null || now < act.ct)
		    continue;
		try {
		    if(name.equals(act.res.get().name))
			return i;
		} catch(Loading ignored) {}
	    }
	}
	return -1;
    }

    public int ownOpening(Fightview fv, String opening) {
	return openingValue(fv == null ? null : fv.buffs, opening);
    }

    public int enemyOpening(Fightview fv, String opening) {
	return openingValue((fv == null || fv.current == null) ? null : fv.current.buffs, opening);
    }

    public int enemyMaxOpening(Fightview fv) {
	Bufflist buffs = (fv == null || fv.current == null) ? null : fv.current.buffs;
	if(buffs == null)
	    return 0;
	int ret = 0;
	ret = Math.max(ret, openingValue(buffs, Buff.OPEN_RED));
	ret = Math.max(ret, openingValue(buffs, Buff.OPEN_YELLOW));
	ret = Math.max(ret, openingValue(buffs, Buff.OPEN_BLUE));
	ret = Math.max(ret, openingValue(buffs, Buff.OPEN_GREEN));
	return ret;
    }

    public int enemyMinOpening(Fightview fv) {
	Bufflist buffs = (fv == null || fv.current == null) ? null : fv.current.buffs;
	if(buffs == null)
	    return 0;
	int ret = 100;
	ret = Math.min(ret, openingValue(buffs, Buff.OPEN_RED));
	ret = Math.min(ret, openingValue(buffs, Buff.OPEN_YELLOW));
	ret = Math.min(ret, openingValue(buffs, Buff.OPEN_BLUE));
	ret = Math.min(ret, openingValue(buffs, Buff.OPEN_GREEN));
	return ret;
    }

    private int openingValue(Bufflist buffs, String opening) {
	if(buffs == null)
	    return 0;
	for(Buff buff : buffs.children(Buff.class)) {
	    try {
		if(opening.equals(buff.res.get().name))
		    return Math.max(0, buff.ameter());
	    } catch(Loading ignored) {}
	}
	return 0;
    }

    private boolean hasLoadedMove() {
	return use >= 0 || useb >= 0;
    }

    private boolean enemyTooCloseForSemiReducer() {
	GameUI gui = getparent(GameUI.class);
	MapView map = gui == null ? null : gui.map;
	Gob player = map == null ? null : map.player();
	if(player == null)
	    return false;
	double maxdist = SEMI_REDUCER_MIN_DISTANCE * 11.0;
	for(Fightview.Relation rel : fv.lsrel) {
	    Gob gob = ui.sess.glob.oc.getgob(rel.gobid);
	    if(gob == null || gob.disposed() || gob.is(GobTag.PARTY) || Boolean.TRUE.equals(gob.isMe()))
		continue;
	    if(player.rc.dist(gob.rc) < maxdist)
		return true;
	}
	return false;
    }

    public void debugEffects(PrintWriter out) {
	int i = 0;
	out.printf("Fightsess target effects: %d active, current=%s%n", curfx.size(), curtgtfx);
	for(Effect fx : curfx) {
	    out.printf("  #%d %s used=%s slot=%s%n",
		i++, fx.spr, fx.used, fx.slot == null ? "none" : "live");
	}
    }
    
    private double inactiveScale() {
	return(Utils.clip(CFG.COMBAT_UI_INACTIVE_SCALE.get(), 1, 100) / 100.0);
    }
    
    private Coord inactiveDraggerSize() {
	Coord sz = INACTIVE_DRAGGER_BASE_SZ.mul(inactiveScale());
	return(Coord.of(Math.max(INACTIVE_DRAGGER_MIN_SZ.x, sz.x), Math.max(INACTIVE_DRAGGER_MIN_SZ.y, sz.y)));
    }
    
    private static double openingDecayRate() {
	return(Utils.clip(CFG.COMBAT_UI_OPENING_DECAY.get(), 0, 50) / 10.0);
    }
    
    private void addinactivebuff(Buff buff, Coord center, Coord dc, boolean enemy) {
	inactivebuffs.add(new InactiveBuff(buff, dc.sub(center), enemy));
    }
    
    private void addopeningsample(Collection<OpeningSample> samples, String side, Buff buff, double now) {
	try {
	    if((Buff.openingColor(buff.res.get().name) != null) && (buff.ameter() >= 0))
		samples.add(new OpeningSample(side, buff, now));
	} catch(Loading l) {
	}
    }
    
    private void snapshotinactive() {
	inactivebuffs.clear();
	if(fv == null)
	    return;
	boolean altui = CFG.ALT_COMBAT_UI.get();
	Coord c0 = dragger.c.add(dragger.sz.div(2));
	int x0 = c0.x, y0 = c0.y;
	Coord center = altui ? c0 : pcc;
	for(Buff buff : fv.buffs.children(Buff.class)) {
	    Coord dc = altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
	    addinactivebuff(buff, center, dc, false);
	}
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
		Coord dc = altui ? new Coord(x0 + buff.c.x + UI.scale(80), y0) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
		addinactivebuff(buff, center, dc, true);
	    }
	}
    }
    
    private void snapshotopenings() {
	if(!CFG.COMBAT_DEBUG_OPENING_RECOVERY.get()) {
	    lastOpeningSamples = Collections.emptyList();
	    return;
	}
	Collection<OpeningSample> samples = new ArrayList<>();
	if(fv != null) {
	    double now = Utils.rtime();
	    for(Buff buff : fv.buffs.children(Buff.class))
		addopeningsample(samples, "mine", buff, now);
	    if(fv.current != null) {
		for(Buff buff : fv.current.buffs.children(Buff.class))
		    addopeningsample(samples, "theirs", buff, now);
	    }
	}
	lastOpeningSamples = samples;
    }
    
    private void reportOpeningDecay() {
	if(!CFG.COMBAT_DEBUG_OPENING_RECOVERY.get())
	    return;
	if(reportedDecay || lastOpeningSamples.isEmpty() || (fv == null) || (fv.current == null))
	    return;
	Map<String, OpeningSample> prev = new HashMap<>();
	for(OpeningSample sample : lastOpeningSamples)
	    prev.put(sample.key(), sample);
	Collection<OpeningMeasurement> measurements = new ArrayList<>();
	double total = 0;
	int n = 0;
	measureOpeningDecay(prev, measurements, "mine", fv.buffs.children(Buff.class));
	measureOpeningDecay(prev, measurements, "theirs", fv.current.buffs.children(Buff.class));
	Collection<String> parts = new ArrayList<>();
	for(OpeningMeasurement measurement : measurements) {
	    parts.add(measurement.text);
	    total += measurement.rate;
	    n++;
	}
	if(!parts.isEmpty()) {
	    reportedDecay = true;
	    String msg = "Measured opening recovery: " + String.join(", ", parts);
	    if(n > 1)
		msg += String.format(" (avg %.2f%%/s)", total / n);
	    if(ui.gui != null)
		ui.gui.msg(msg, GameUI.MsgType.INFO);
	    System.out.println(msg);
	}
    }
    
    private void measureOpeningDecay(Map<String, OpeningSample> prev, Collection<OpeningMeasurement> measurements, String side, Collection<Buff> buffs) {
	for(Buff buff : buffs) {
	    try {
		String name = buff.res.get().name;
		if(Buff.openingColor(name) == null)
		    continue;
		OpeningSample old = prev.get(side + ":" + name);
		int nowv = buff.ameter();
		if((old == null) || (old.value < 0) || (nowv < 0))
		    continue;
		double elapsed = Utils.rtime() - old.time;
		if(elapsed < 0.5)
		    continue;
		double rate = (old.value - nowv) / elapsed;
		if((rate >= MIN_MEASURED_OPENING_DECAY) && (rate <= MAX_MEASURED_OPENING_DECAY))
		    measurements.add(new OpeningMeasurement(String.format("%s %s %d->%d over %.1fs = %.2f%%/s", side, openingName(name), old.value, nowv, elapsed, rate), rate));
	    } catch(Loading l) {
	    }
	}
    }
    
    private static String openingName(String resname) {
	int p = resname.lastIndexOf('/');
	return((p >= 0) ? resname.substring(p + 1) : resname);
    }

    public void destroy() {
	if(!ended) {
	    for(Effect fx : curfx) {
		if(fx.slot != null)
		    fx.slot.remove();
	    }
	    curfx.clear();
	    ui.gui.calendar.show();
	    if(CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT.get()) {
		haven.Action.CLEAR_PLAYER_DAMAGE.run(ui.gui);
	    }
	    if(CFG.CLEAR_ALL_DMG_AFTER_COMBAT.get()) {
		haven.Action.CLEAR_ALL_DAMAGE.run(ui.gui);
	    }
	    if(fv != null) {
		snapshotinactive();
		snapshotopenings();
		inactiveStart = Utils.rtime();
		lastact1 = fv.lastact;
		lastuse1 = fv.lastuse;
		if(fv.current != null) {
		    boolean altui = CFG.ALT_COMBAT_UI.get();
		    lastact2 = fv.current.lastact;
		    lastuse2 = fv.current.lastuse;
		    inactiveip = ipf.render((altui ? "" : "IP: ") + fv.current.ip);
		    inactiveoip = ipf.render((altui ? "" : "IP: ") + fv.current.oip);
		}
	    }
	    ended = true;
	}
	if(forcedestroy) {
	    super.destroy();
	} else if(!CFG.KEEP_COMBAT_UI_AFTER_COMBAT.get()) {
	    super.destroy();
	} else if(active) {
	    active = false;
	    fv = null;
	    Coord center = dragger.c.add(dragger.sz.div(2));
	    dragger.sz = inactiveDraggerSize();
	    dragger.c = center.sub(dragger.sz.div(2));
	}
    }
    
    public void forceDestroy() {
	forcedestroy = true;
	destroy();
    }

    private static final Text.Furnace ipf = new PUtils.BlurFurn(new Text.Foundry(Text.serif, 18, new Color(128, 128, 255)).aa(true), 1, 1, new Color(48, 48, 96));
    private final Indir<Text> ip = Utils.transform(() -> fv.current.ip, v -> ipf.render((CFG.ALT_COMBAT_UI.get() ? "" : "IP: ") + v));
    private final Indir<Text> oip = Utils.transform(() -> fv.current.oip, v -> ipf.render((CFG.ALT_COMBAT_UI.get() ? "" : "IP: ") + v));

    private static Coord actc(int i) {
	int rl = 5;
	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(125) + ((i / rl) * actpitch)));
    }

    private static Coord utilityc(int i) {
	int rl = 5;
	return(new Coord((actpitch * (i % rl)) - (((rl - 1) * actpitch) / 2), UI.scale(225)));
    }

    private static final Coord cmc = UI.scale(new Coord(0, 67));
    private static final Coord usec1 = UI.scale(new Coord(-65, 67));
    private static final Coord usec2 = UI.scale(new Coord(65, 67));
    private Indir<Resource> lastact1 = null, lastact2 = null;
    private double lastuse1 = 0, lastuse2 = 0;
    private Text inactiveip = null, inactiveoip = null;
    private Text lastacttip1 = null, lastacttip2 = null;
    private Effect curtgtfx;
    public void draw(GOut g) {
	if(!active) {
	    drawinactive(g);
	    return;
	}
	updatepos();
        boolean altui = CFG.ALT_COMBAT_UI.get();
	Coord c0 = ui.gui.calendar.rootpos().add(ui.gui.calendar.sz.div(2));
	dragger.origin(c0.sub(dragger.sz.div(2)));
	int xa = c0.x;
	c0 = dragger.c.add(dragger.sz.div(2));
	int x0 = c0.x;
	int y0 = c0.y;
	int bottom = ui.gui.beltwdg.c.y - UI.scale(40);
	double now = Utils.rtime();
	reportOpeningDecay();

	for(Buff buff : fv.buffs.children(Buff.class))
	    buff.draw(g.reclip(altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class))
		buff.draw(g.reclip(altui ? new Coord(x0 + buff.c.x + UI.scale(80), y0) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y), buff.sz));

	    g.aimage(ip.get().tex(), altui ? new Coord(x0 - UI.scale(45), y0 - UI.scale(16)) : pcc.add(-UI.scale(75), 0), 1, 0.5);
	    g.aimage(oip.get().tex(), altui ? new Coord(x0 + UI.scale(45), y0 - UI.scale(16)) : pcc.add(UI.scale(75), 0), 0, 0.5);

	    if(fv.lsrel.size() > (CFG.ALWAYS_MARK_COMBAT_TARGET.get() ? 0 : 1))
		curtgtfx = fxon(fv.current.gobid, tgtfx, curtgtfx);
	}

	{
	    Coord cdc = altui ? new Coord(x0, y0) : pcc.add(cmc);
	    if(now < fv.atkct) {
		double a = (now - fv.atkcs) / (fv.atkct - fv.atkcs);
		g.chcolor(255, 0, 128, 224);
		g.fellipse(cdc, UI.scale(altui ? new Coord(24, 24) : new Coord(22, 22)), Math.PI / 2 - (Math.PI * 2 * Math.min(1.0 - a, 1.0)), Math.PI / 2);
		g.chcolor();
		FastText.aprintf(g, cdc, 0.5, 0.5, "%.1f", fv.atkct - now);
	    }
	    g.image(cdframe, altui ? new Coord(x0, y0).sub(cdframe.sz().div(2)) : cdc.sub(cdframe.sz().div(2)));
	}
	try {
	    Indir<Resource> lastact = fv.lastact;
	    if(lastact != this.lastact1) {
		this.lastact1 = lastact;
		this.lastacttip1 = null;
	    }
	    double lastuse = fv.lastuse;
	    if(lastact != null) {
		Tex ut = lastact.get().flayer(Resource.imgc).tex();
		Coord useul = altui ? new Coord(x0 - UI.scale(69), y0) : pcc.add(usec1).sub(ut.sz().div(2));
		g.image(ut, useul);
		g.image(useframe, useul.sub(useframeo));
		double a = now - lastuse;
		if(a < 1) {
		    Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
		    g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
		    g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
		    g.chcolor();
		}
	    }
	} catch(Loading l) {
	}
	if(fv.current != null) {
	    try {
		Indir<Resource> lastact = fv.current.lastact;
		if(lastact != this.lastact2) {
		    this.lastact2 = lastact;
		    this.lastacttip2 = null;
		}
		double lastuse = fv.current.lastuse;
		if(lastact != null) {
		    Tex ut = lastact.get().flayer(Resource.imgc).tex();
		    Coord useul = altui ? new Coord(x0 + UI.scale(69) - ut.sz().x, y0) : pcc.add(usec2).sub(ut.sz().div(2));
		    g.image(ut, useul);
		    g.image(useframe, useul.sub(useframeo));
		    double a = now - lastuse;
		    if(a < 1) {
			Coord off = new Coord((int)(a * ut.sz().x / 2), (int)(a * ut.sz().y / 2));
			g.chcolor(255, 255, 255, (int)(255 * (1 - a)));
			g.image(ut, useul.sub(off), ut.sz().add(off.mul(2)));
			g.chcolor();
		    }
		}
	    } catch(Loading l) {
	    }
	}
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(actc(i)) : pcc.add(actc(i));
	    Action act = actions[i];
	    try {
		if(act != null) {
		    Tex img = act.res.get().flayer(Resource.imgc).tex();
		    Coord hsz = img.sz().div(2);
		    g.image(img, ca);
		    if(now < act.ct) {
			double a = (now - act.cs) / (act.ct - act.cs);
			g.chcolor(0, 0, 0, 132);
			g.prect(ca.add(hsz), hsz.inv(), hsz, (1.0 - a) * Math.PI * 2);
			g.chcolor();
			g.aimage(Text.renderstroked(String.format("%.1f", act.ct - now)).tex(), ca.add(hsz.x, 0), 0.5, 0);
		    }
		    if(CFG.SHOW_COMBAT_KEYS.get()) {g.aimage(keytex(i), ca.add(img.sz()), 1, 1);}
		    
		    if(i == use) {
			g.image(indframe, ca.sub(indframeo));
		    } else if(i == useb) {
			g.image(indbframe, ca.sub(indbframeo));
		    } else {
			g.image(actframe, ca.sub(actframeo));
		    }
		}
	    } catch(Loading l) {}
	}
	drawReducerBar(g, altui, xa, bottom);
    }

    private void drawReducerBar(GOut g, boolean altui, int xa, int bottom) {
	for(int i = 0; i < 5; i++) {
	    Coord ca = altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(utilityc(i)) : pcc.add(utilityc(i));
	    g.image(actframe, ca.sub(actframeo));
	    if(i == 0) {
		drawReducerIcon(g, ca);
		if(CFG.SHOW_COMBAT_KEYS.get())
		    g.aimage(reducerKeyTex(), ca.add(off), 1, 1);
	    } else if(i == 1) {
		drawTargetClosestIcon(g, ca);
		if(CFG.SHOW_COMBAT_KEYS.get())
		    g.aimage(targetClosestKeyTex(), ca.add(off), 1, 1);
	    } else if(i == 4) {
		drawGuardedSkillIcon(g, ca);
		if(CFG.SHOW_COMBAT_KEYS.get())
		    g.aimage(guardedSkillsKeyTex(), ca.add(off), 1, 1);
	    }
	}
    }

    private void drawReducerIcon(GOut g, Coord ca) {
	Coord center = ca.add(off.div(2));
	g.chcolor(0, 0, 0, 150);
	g.frect(ca.add(UI.scale(4, 4)), off.sub(UI.scale(8, 8)));
	g.chcolor();
	Color first = reducerMode == CombatReducerMode.ON || reducerMode == CombatReducerMode.SEMI ? new Color(60, 230, 80) : new Color(150, 150, 150);
	Color second = reducerMode == CombatReducerMode.ON ? new Color(60, 230, 80) : new Color(150, 150, 150);
	drawArrow(g, center.add(-UI.scale(1), -UI.scale(3)), UI.scale(10), true, first);
	drawArrow(g, center.add(UI.scale(1), UI.scale(4)), UI.scale(10), false, second);
    }

    private void drawTargetClosestIcon(GOut g, Coord ca) {
	Coord center = ca.add(off.div(2));
	boolean on = CFG.MAZES_TARGET_CLOSEST_COMBAT.get();
	Color color = on ? new Color(60, 230, 80) : new Color(150, 150, 150);
	g.chcolor(0, 0, 0, 150);
	g.frect(ca.add(UI.scale(4, 4)), off.sub(UI.scale(8, 8)));
	g.chcolor(color);
	g.line(center.add(-UI.scale(10), 0), center.add(UI.scale(10), 0), UI.scale(2));
	g.line(center.add(0, -UI.scale(10)), center.add(0, UI.scale(10)), UI.scale(2));
	g.fellipse(center, UI.scale(4, 4));
	g.chcolor();
    }

    private void drawGuardedSkillIcon(GOut g, Coord ca) {
	Coord center = ca.add(off.div(2));
	boolean on = CFG.GUARDED_COMBAT_SKILLS_ENABLED.get();
	Color color = on ? new Color(60, 230, 80) : new Color(150, 150, 150);
	g.chcolor(0, 0, 0, 150);
	g.frect(ca.add(UI.scale(4, 4)), off.sub(UI.scale(8, 8)));
	g.chcolor(color);
	g.line(center.add(-UI.scale(8), -UI.scale(8)), center.add(UI.scale(8), UI.scale(8)), UI.scale(2));
	g.line(center.add(UI.scale(8), -UI.scale(8)), center.add(-UI.scale(8), UI.scale(8)), UI.scale(2));
	if(guardedSkill != null) {
	    try {
		Tex img = guardedSkill.res.get().flayer(Resource.imgc).tex();
		g.chcolor(255, 255, 255, 180);
		g.image(img, ca.add(off.sub(img.sz()).div(2)));
	    } catch(Loading ignored) {}
	}
	g.chcolor();
    }

    private void drawArrow(GOut g, Coord c, int r, boolean upper, Color color) {
	g.chcolor(color);
	if(upper) {
	    g.line(c.add(-r, 0), c.add(0, -r), UI.scale(2));
	    g.line(c.add(0, -r), c.add(r, 0), UI.scale(2));
	    g.line(c.add(r, 0), c.add(r - UI.scale(5), -UI.scale(1)), UI.scale(2));
	    g.line(c.add(r, 0), c.add(r - UI.scale(1), -UI.scale(5)), UI.scale(2));
	} else {
	    g.line(c.add(r, 0), c.add(0, r), UI.scale(2));
	    g.line(c.add(0, r), c.add(-r, 0), UI.scale(2));
	    g.line(c.add(-r, 0), c.add(-r + UI.scale(5), UI.scale(1)), UI.scale(2));
	    g.line(c.add(-r, 0), c.add(-r + UI.scale(1), UI.scale(5)), UI.scale(2));
	}
	g.chcolor();
    }
    
    private void drawinactive(GOut g) {
	Coord c0 = ui.gui.calendar.rootpos().add(ui.gui.calendar.sz.div(2));
	Coord nsz = inactiveDraggerSize();
	if(!dragger.sz.equals(nsz)) {
	    Coord center = dragger.c.add(dragger.sz.div(2));
	    dragger.sz = nsz;
	    dragger.c = center.sub(dragger.sz.div(2));
	}
	dragger.origin(c0.sub(dragger.sz.div(2)));
	Coord center = dragger.c.add(dragger.sz.div(2));
	double scale = inactiveScale();
	double now = Utils.rtime();
	double ownAlpha = 0;
	for(InactiveBuff buff : inactivebuffs) {
	    int alpha = inactiveBuffAlpha(buff, now);
	    if(!buff.enemy)
		ownAlpha = Math.max(ownAlpha, alpha / 255.0);
	    drawinactivebuff(g, buff, center.add(buff.relc.mul(scale)), scale, alpha);
	}
	Coord cdc = center.add(cmc.mul(scale));
	Coord cdsz = cdframe.sz().mul(scale);
	int cooldownAlpha = inactiveCooldownAlpha(now, ownAlpha);
	if(cooldownAlpha > 0) {
	    g.chcolor(255, 255, 255, cooldownAlpha);
	    g.image(cdframe, cdc.sub(cdsz.div(2)), cdsz);
	    g.chcolor();
	}
	int endedAlpha = fadeAlpha(inactiveStart, now);
	if(inactiveip != null) {
	    Coord psz = inactiveip.sz().mul(scale);
	    Coord pc = center.add(new Coord(-UI.scale(45), -UI.scale(16)).mul(scale));
	    g.chcolor(255, 255, 255, endedAlpha);
	    g.image(inactiveip.tex(), pc.sub(psz.x, psz.y / 2), psz);
	    g.chcolor();
	}
	if(inactiveoip != null) {
	    Coord psz = inactiveoip.sz().mul(scale);
	    Coord pc = center.add(new Coord(UI.scale(45), -UI.scale(16)).mul(scale));
	    g.chcolor(255, 255, 255, endedAlpha);
	    g.image(inactiveoip.tex(), pc.sub(0, psz.y / 2), psz);
	    g.chcolor();
	}
	drawinactiveuse(g, lastact1, lastuse1, center.add(usec1.mul(scale)), now, scale, cooldownAlpha);
	drawinactiveuse(g, lastact2, lastuse2, center.add(usec2.mul(scale)), now, scale, endedAlpha);
    }
    
    private int inactiveBuffAlpha(InactiveBuff buff, double now) {
	return(fadeAlpha(buff.fadeStart(), now));
    }
    
    private int inactiveCooldownAlpha(double now, double ownAlpha) {
	if(ownAlpha > 0)
	    return((int)Math.round(255 * ownAlpha));
	return(fadeAlpha(inactiveStart, now));
    }
    
    private static int fadeAlpha(double fadeStart, double now) {
	if(fadeStart <= 0)
	    return(255);
	double elapsed = now - fadeStart;
	if(elapsed <= 0)
	    return(255);
	if(elapsed >= INACTIVE_FADE_TIME)
	    return(0);
	return((int)Math.round(255 * (1.0 - (elapsed / INACTIVE_FADE_TIME))));
    }
    
    private void drawinactivebuff(GOut g, InactiveBuff buff, Coord c, double scale, int alpha) {
	if(alpha <= 0)
	    return;
	try {
	    Resource res = buff.res.get();
	    Tex img = res.flayer(Resource.imgc).tex();
	    Coord fsz = Buff.cframe.sz().mul(scale);
	    Coord imgoff = Buff.imgoff.mul(scale);
	    Coord isz = img.sz().mul(scale);
	    g.chcolor(255, 255, 255, alpha);
	    g.image(Buff.frame, c, fsz);
	    Color opening = Buff.openingColor(res.name);
	    if(CFG.SIMPLE_COMBAT_OPENINGS.get() && (opening != null)) {
		g.chcolor(opening.getRed(), opening.getGreen(), opening.getBlue(), alpha);
		g.frect(c.add(imgoff), isz);
		g.chcolor(255, 255, 255, alpha);
		int ameter = buff.ameter();
		if(ameter >= 0) {
		    Tex meteri = Text.renderstroked(Integer.toString(ameter), Buff.nfnd).tex();
		    Coord msz = meteri.sz().mul(scale);
		    g.aimage(meteri, c.add(imgoff).add(isz).sub(1, 1), 1, 1, msz);
		}
	    } else {
		g.image(img, c.add(imgoff), isz);
	    }
	    g.chcolor();
	} catch(Loading l) {
	    g.chcolor();
	}
    }
    
    private void drawinactiveuse(GOut g, Indir<Resource> lastact, double lastuse, Coord c, double now, double scale, int alpha) {
	if((lastact == null) || (alpha <= 0))
	    return;
	try {
	    Tex ut = lastact.get().flayer(Resource.imgc).tex();
	    Coord usz = ut.sz().mul(scale);
	    Coord useul = c.sub(usz.div(2));
	    g.chcolor(255, 255, 255, alpha);
	    g.image(ut, useul, usz);
	    g.image(useframe, useul.sub(useframeo.mul(scale)), useframe.sz().mul(scale));
	    double a = now - lastuse;
	    if(a < 1) {
		Coord off = new Coord((int)(a * usz.x / 2), (int)(a * usz.y / 2));
		g.chcolor(255, 255, 255, (int)(alpha * (1 - a)));
		g.image(ut, useul.sub(off), usz.add(off.mul(2)));
	    }
	    g.chcolor();
	} catch(Loading l) {
	    g.chcolor();
	}
    }
    
    public static final Tex[] keytex = new Tex[keybinds.length];
    public static Tex reducerKeyTex = null;
    public static Tex targetClosestKeyTex = null;
    public static Tex guardedSkillsKeyTex = null;
    
    static {
	Reactor.listen(COMBAT_KEYS_UPDATED, () ->
	{
	    for (int i = 0; i < keytex.length; i++) {
		if(keytex[i] != null) { keytex[i].dispose(); }
		keytex[i] = null;
	    }
	    if(reducerKeyTex != null) {reducerKeyTex.dispose();}
	    reducerKeyTex = null;
	    if(targetClosestKeyTex != null) {targetClosestKeyTex.dispose();}
	    targetClosestKeyTex = null;
	    if(guardedSkillsKeyTex != null) {guardedSkillsKeyTex.dispose();}
	    guardedSkillsKeyTex = null;
	});
    }
    
    private Tex keytex(int i) {
	if(keytex[i] == null) {
	    keytex[i] = Text.renderstroked(keybinds[i].shortcut(true), fnd).tex();
	}
	return keytex[i];
    }

    private Tex reducerKeyTex() {
	if(reducerKeyTex == null)
	    reducerKeyTex = Text.renderstroked(reducerKeybind.shortcut(true), fnd).tex();
	return reducerKeyTex;
    }

    private Tex targetClosestKeyTex() {
	if(targetClosestKeyTex == null)
	    targetClosestKeyTex = Text.renderstroked(targetClosestKeybind.shortcut(true), fnd).tex();
	return targetClosestKeyTex;
    }

    private Tex guardedSkillsKeyTex() {
	if(guardedSkillsKeyTex == null)
	    guardedSkillsKeyTex = Text.renderstroked(guardedSkillsKeybind.shortcut(true), fnd).tex();
	return guardedSkillsKeyTex;
    }
    
    private Widget prevtt = null;
    private Text acttip = null;
    
    public Object tooltip(Coord c, Widget prev) {
	if(!active)
	    return(null);
	boolean altui = CFG.ALT_COMBAT_UI.get();
	int x0 =  ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int xa = x0;
	int y0 =  ui.gui.calendar.rootpos().y + ui.gui.calendar.sz.y / 2;
	int bottom = ui.gui.beltwdg.c.y - 40;
	Coord rca = altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(utilityc(0)) : pcc.add(utilityc(0));
	if(c.isect(rca, off))
	    return "Auto combat reducer: " + reducerMode.label;
	Coord tca = altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(utilityc(1)) : pcc.add(utilityc(1));
	if(c.isect(tca, off))
	    return "Target closest: " + (CFG.MAZES_TARGET_CLOSEST_COMBAT.get() ? "On" : "Off");
	Coord gca = altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(utilityc(4)) : pcc.add(utilityc(4));
	if(c.isect(gca, off))
	    return guardedSkill == null
		? "Guarded skills: " + (CFG.GUARDED_COMBAT_SKILLS_ENABLED.get() ? "On" : "Off")
		: "Queued: " + GuardedCombatSkills.label(guardedSkill.resname);
	for(Buff buff : fv.buffs.children(Buff.class)) {
	    Coord dc = altui ? new Coord(x0 - buff.c.x - Buff.cframe.sz().x - UI.scale(80), y0) : pcc.add(-buff.c.x - Buff.cframe.sz().x - UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
	    if(c.isect(dc, buff.sz)) {
		Object ret = buff.tooltip(c.sub(dc), prevtt);
		if(ret != null) {
		    prevtt = buff;
		    return(ret);
		}
	    }
	}
	if(fv.current != null) {
	    for(Buff buff : fv.current.buffs.children(Buff.class)) {
		Coord dc = altui ? new Coord(x0 + buff.c.x + UI.scale(80), y0) : pcc.add(buff.c.x + UI.scale(20), buff.c.y + pho - Buff.cframe.sz().y);
		if(c.isect(dc, buff.sz)) {
		    Object ret = buff.tooltip(c.sub(dc), prevtt);
		    if(ret != null) {
			prevtt = buff;
			return(ret);
		    }
		}
	    }
	}
	final int rl = 5;
	for(int i = 0; i < actions.length; i++) {
	    Coord ca = altui ? new Coord(x0 - 18, bottom - 150).add(actc(i)).add(16, 16) : pcc.add(actc(i));
	    Indir<Resource> act = (actions[i] == null) ? null : actions[i].res;
	    if(act != null) {
		Tex img = act.get().flayer(Resource.imgc).tex();
		ca = ca.sub(img.sz().div(2));
		if(c.isect(ca, img.sz())) {
		    String tip = act.get().flayer(Resource.tooltip).t + " ($b{$col[255,128,0]{" + keybinds[i].shortcut(true) + "}})";
		    if((acttip == null) || !acttip.text.equals(tip))
			acttip = RichText.render(tip, -1);
		    return(acttip);
		}
	    }
	}
	{
	    Indir<Resource> lastact = this.lastact1;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 - 69, y0).add(usesz.div(2)) : pcc.add(usec1);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip1 == null)
			lastacttip1 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip1);
		}
	    }
	}
	{
	    Indir<Resource> lastact = this.lastact2;
	    if(lastact != null) {
		Coord usesz = lastact.get().flayer(Resource.imgc).sz;
		Coord lac = altui ? new Coord(x0 + 69 - usesz.x, y0).add(usesz.div(2)) : pcc.add(usec2);
		if(c.isect(lac.sub(usesz.div(2)), usesz)) {
		    if(lastacttip2 == null)
			lastacttip2 = Text.render(lastact.get().flayer(Resource.tooltip).t);
		    return(lastacttip2);
		}
	    }
	}
	return(null);
    }
    
    public boolean mousedown(MouseDownEvent ev) {
	if(active && ev.b == 1 && ev.c.isect(reducerButtonCoord(), off)) {
	    cycleReducerMode();
	    return true;
	}
	if(active && ev.b == 1 && ev.c.isect(utilityButtonCoord(1), off)) {
	    toggleTargetClosest();
	    return true;
	}
	if(active && ev.b == 1 && ev.c.isect(utilityButtonCoord(4), off)) {
	    toggleGuardedSkills();
	    return true;
	}
	if(!active && DraggableWidget.guiEditMode() && ev.c.isect(dragger.c, dragger.sz))
	    return(true);
	return(super.mousedown(ev));
    }

    private Coord reducerButtonCoord() {
	return utilityButtonCoord(0);
    }

    private Coord utilityButtonCoord(int i) {
	boolean altui = CFG.ALT_COMBAT_UI.get();
	int xa = ui.gui.calendar.rootpos().x + ui.gui.calendar.sz.x / 2;
	int bottom = ui.gui.beltwdg.c.y - UI.scale(40);
	return altui ? new Coord(xa - UI.scale(18), bottom - UI.scale(150)).add(utilityc(i)) : pcc.add(utilityc(i));
    }

    private void cycleReducerMode() {
	reducerMode = reducerMode.next();
    }

    private void toggleTargetClosest() {
	CFG.MAZES_TARGET_CLOSEST_COMBAT.set(!CFG.MAZES_TARGET_CLOSEST_COMBAT.get());
    }

    private void toggleGuardedSkills() {
	CFG.GUARDED_COMBAT_SKILLS_ENABLED.set(!CFG.GUARDED_COMBAT_SKILLS_ENABLED.get());
	if(!CFG.GUARDED_COMBAT_SKILLS_ENABLED.get())
	    guardedSkill = null;
    }

    public boolean handleUtilityKey(KbdEvent ev) {
	if(!active)
	    return false;
	if(reducerKeybind.match(ev)) {
	    cycleReducerMode();
	    return true;
	}
	if(targetClosestKeybind.match(ev)) {
	    toggleTargetClosest();
	    return true;
	}
	if(guardedSkillsKeybind.match(ev)) {
	    toggleGuardedSkills();
	    return true;
	}
	return false;
    }

    private boolean queueOrUse(int fn, int button, int modflags, Coord mc) {
	Action act = actions[fn];
	if(act == null)
	    return false;
	try {
	    String resname = act.res.get().name;
	    if(CFG.GUARDED_COMBAT_SKILLS_ENABLED.get() && GuardedCombatSkills.enabled(resname) && !GuardedCombatSkills.canUse(resname, this, fv)) {
		guardedSkill = new QueuedGuardedSkill(fn, button, modflags, mc, act.res, resname);
		return true;
	    }
	} catch(Loading ignored) {}
	if(mc == null)
	    wdgmsg("use", fn, button, modflags);
	else
	    wdgmsg("use", fn, button, modflags, mc);
	return false;
    }

    private void releaseGuardedSkill() {
	if(guardedSkill == null || !active || fv == null)
	    return;
	if(guardedSkill.fn < 0 || guardedSkill.fn >= actions.length || actions[guardedSkill.fn] == null) {
	    guardedSkill = null;
	    return;
	}
	if(!GuardedCombatSkills.canUse(guardedSkill.resname, this, fv))
	    return;
	QueuedGuardedSkill use = guardedSkill;
	guardedSkill = null;
	if(use.mc == null)
	    wdgmsg("use", use.fn, use.button, use.modflags);
	else
	    wdgmsg("use", use.fn, use.button, use.modflags, use.mc);
    }

    private static class QueuedGuardedSkill {
	final int fn, button, modflags;
	final Coord mc;
	final Indir<Resource> res;
	final String resname;

	QueuedGuardedSkill(int fn, int button, int modflags, Coord mc, Indir<Resource> res, String resname) {
	    this.fn = fn;
	    this.button = button;
	    this.modflags = modflags;
	    this.mc = mc;
	    this.res = res;
	    this.resname = resname;
	}
    }

    public void uimsg(String msg, Object... args) {
	if(msg == "act") {
	    int n = Utils.iv(args[0]);
	    if(args.length > 1) {
		Indir<Resource> res = ui.sess.getresv(args[1]);
		actions[n] = new Action(res);
	    } else {
		actions[n] = null;
	    }
	} else if(msg == "acool") {
	    int n = Utils.iv(args[0]);
	    double now = Utils.rtime();
	    actions[n].cs = now;
	    actions[n].ct = now + (Utils.dv(args[1]) * 0.06);
	} else if(msg == "use") {
	    this.use = Utils.iv(args[0]);
	    this.useb = (args.length > 1) ? Utils.iv(args[1]) : -1;
	} else if(msg == "used") {
	} else {
	    super.uimsg(msg, args);
	}
    }

    public static final KeyBinding[] kb_acts = {
	KeyBinding.get("fgt/0", KeyMatchFake.forcode(KeyEvent.VK_1, 0)),
	KeyBinding.get("fgt/1", KeyMatchFake.forcode(KeyEvent.VK_2, 0)),
	KeyBinding.get("fgt/2", KeyMatchFake.forcode(KeyEvent.VK_3, 0)),
	KeyBinding.get("fgt/3", KeyMatchFake.forcode(KeyEvent.VK_4, 0)),
	KeyBinding.get("fgt/4", KeyMatchFake.forcode(KeyEvent.VK_5, 0)),
	KeyBinding.get("fgt/5", KeyMatchFake.forcode(KeyEvent.VK_1, KeyMatch.S)),
	KeyBinding.get("fgt/6", KeyMatchFake.forcode(KeyEvent.VK_2, KeyMatch.S)),
	KeyBinding.get("fgt/7", KeyMatchFake.forcode(KeyEvent.VK_3, KeyMatch.S)),
	KeyBinding.get("fgt/8", KeyMatchFake.forcode(KeyEvent.VK_4, KeyMatch.S)),
	KeyBinding.get("fgt/9", KeyMatchFake.forcode(KeyEvent.VK_5, KeyMatch.S)),
    };
    public static final KeyBinding kb_relcycle =  KeyBinding.get("fgt-cycle", KeyMatch.forcode(KeyEvent.VK_TAB, KeyMatch.C), KeyMatch.S);

    /* XXX: This is a bit ugly, but release message do need to be
     * properly sequenced with use messages in some way. */
    private class Release implements Runnable {
	final int n;

	Release(int n) {
	    this.n = n;
	    Environment env = ui.getenv();
	    Render out = env.render();
	    out.fence(this);
	    env.submit(out);
	}


	public void run() {
	    wdgmsg("rel", n);
	}
    }

    private UI.Grab holdgrab = null;
    private int held = -1;
    public boolean globtype(GlobKeyEvent ev) {
	if(!active)
	    return(super.globtype(ev));
	// ev = new KeyEvent((java.awt.Component)ev.getSource(), ev.getID(), ev.getWhen(), ev.getModifiersEx(), ev.getKeyCode(), ev.getKeyChar(), ev.getKeyLocation());
	if(handleUtilityKey(ev))
	    return true;
	{
	    int fn = getAction(ev);
	    if((fn >= 0) && (fn < actions.length)) {
		MapView map = getparent(GameUI.class).map;
		Coord mvc = map.rootxlate(ui.mc);
		final boolean[] queued = {false};
		if(held >= 0) {
		    new Release(held);
		    held = -1;
		}
		if(mvc.isect(Coord.z, map.sz)) {
		    map.new Maptest(mvc) {
			    protected void hit(Coord pc, Coord2d mc) {
				queued[0] = queueOrUse(fn, 1, ui.modflags(), mc.floor(OCache.posres));
			    }

			    protected void nohit(Coord pc) {
				queued[0] = queueOrUse(fn, 1, ui.modflags(), null);
			    }
			}.run();
		}
		if(queued[0])
		    return true;
		if(holdgrab == null)
		    holdgrab = ui.grabkeys(this);
		held = fn;
		return(true);
	    }
	}
	if(kb_relcycle.key().match(ev.awt, KeyMatch.S)) {
	    if((ev.mods & KeyMatch.S) == 0) {
		Fightview.Relation cur = fv.current;
		if(cur != null) {
		    fv.lsrel.remove(cur);
		    fv.lsrel.addLast(cur);
		}
	    } else {
		Fightview.Relation last = fv.lsrel.getLast();
		if(last != null) {
		    fv.lsrel.remove(last);
		    fv.lsrel.addFirst(last);
		}
	    }
	    fv.wdgmsg("bump", (int)fv.lsrel.get(0).gobid);
	    return(true);
	}
	return(super.globtype(ev));
    }

    public boolean keydown(KeyDownEvent ev) {
	return(false);
    }

    public boolean keyup(KeyUpEvent ev) {
	if(!active)
	    return(false);
	if(ev.grabbed && (keybinds[held].match(ev, KeyBinder.MODS))) {
	    MapView map = getparent(GameUI.class).map;
	    new Release(held);
	    holdgrab.remove();
	    holdgrab = null;
	    held = -1;
	    return(true);
	}
	return(false);
    }
    
    private int getAction(GlobKeyEvent ev) {
	for (int i = 0; i < actions.length && i < keybinds.length; i++) {
	    if(keybinds[i].match(ev)) {
		return i;
	    }
	}
	return -1;
    }
    
    public static void updateKeybinds(KeyBind[] combat) {
	if(combat != null) {
	    for (int i = 0; i < combat.length && i < keybinds.length; i++) {
		keybinds[i] = combat[i];
	    }
	}
    }

    public static void updateUtilityKeybinds(KeyBind reducer, KeyBind targetClosest, KeyBind guardedSkills) {
	if(reducer != null)
	    reducerKeybind = reducer;
	if(targetClosest != null)
	    targetClosestKeybind = targetClosest;
	if(guardedSkills != null)
	    guardedSkillsKeybind = guardedSkills;
    }
}
