package haven;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MeterWidgetBox extends ResizableDraggableWidget {
    private static final Text.Foundry TEXT_FND = new Text.Foundry(Text.sansbold, 12);
    private static final IBox SMALL_FRAME = new IBox.Scaled("gfx/hud/wnd", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    private static final Color HP_HARD = new Color(205, 154, 0, 230);
    private static final Color HP_SOFT = new Color(216, 0, 0, 240);
    private static final Color STAM_LO = new Color(3, 3, 80, 190);
    private static final Color STAM_MID = new Color(16, 16, 128, 190);
    private static final Color STAM_HI = new Color(16, 16, 255, 190);
    private static final Color ENERGY = new Color(128, 128, 255, 205);
    private static final Color ENERGY_LOW = new Color(220, 80, 80, 220);
    private static final int ENERGY_MAX = 10000;
    private static final Pattern HP_TIP = Pattern.compile(".*?(\\d+)/(\\d+)/(\\d+).*");
    private static final Pattern NUMBERS = Pattern.compile("(\\d+)");
    
    public final IMeter meter;
    private final String metername;

    public MeterWidgetBox(String name, IMeter meter) {
	super("MeterWidgetBox:" + name, UI.scale(60, 14));
	this.metername = name;
	this.meter = add(meter, Coord.z);
	meter.hide();
	resize(meter.sz);
	disposables.add(CFG.LOCK_FLOATING_STAT_WDGS.observe(this::updateState));
	updateState(null);
    }

    private void updateState(CFG<Boolean> cfg) {
	boolean unlocked = !CFG.LOCK_FLOATING_STAT_WDGS.get();
	draggable(unlocked);
	resizable(unlocked);
    }

    @Override
    public void resize(Coord sz) {
	super.resize(sz);
	if(meter != null)
	    meter.resize(sz);
    }

    @Override
    public boolean checkhit(Coord c) {
	return draggable() && c.isect(Coord.z, sz);
    }

    @Override
    public void cdestroy(Widget ch) {
	if(ch == meter)
	    reqdestroy();
	else
	    super.cdestroy(ch);
    }

    @Override
    public void draw(GOut g) {
	drawmeter(g);
    }
    
    private void drawmeter(GOut g) {
	if(metername.equals("hp")) {
	    drawhp(g);
	} else if(metername.equals("stam")) {
	    drawstam(g);
	} else if(metername.equals("nrj")) {
	    drawenergy(g);
	} else {
	    meter.draw(g);
	}
    }
    
    private void frame(GOut g) {
	if(CFG.THEME.get() == Theme.Small)
	    SMALL_FRAME.draw(g, Coord.z, sz);
	else
	    Window.wbox.draw(g, Coord.z, sz);
    }
    
    private void label(GOut g, String text) {
	g.chcolor();
	g.aimage(Text.renderstroked(text, TEXT_FND).tex(), sz.div(2), 0.5, 0.5);
    }
    
    private void drawhp(GOut g) {
	double hhp = meter.meter(0);
	double shp = meter.meter(1);
	if(hhp < 0)
	    hhp = 0;
	if(shp < 0)
	    shp = hhp;
	g.chcolor(HP_HARD);
	g.frect(Coord.z, sz.mul(fill(hhp), 1));
	g.chcolor(HP_SOFT);
	g.frect(Coord.z, sz.mul(fill(shp), 1));
	frame(g);
	label(g, hptext(hhp));
    }
    
    private String hptext(double hhp) {
	if(meter.tip != null) {
	    Matcher matcher = HP_TIP.matcher(meter.tip);
	    if(matcher.matches())
		return String.format("%s/%s/%s", matcher.group(1), matcher.group(2), matcher.group(3));
	}
	return String.format("%.0f%% HP", hhp * 100.0);
    }
    
    private void drawstam(GOut g) {
	double value = Math.max(0, meter.meter(0));
	double shown = fill(value);
	int w1 = (int)(sz.x * Math.min(shown, 0.25));
	int w2 = (int)(sz.x * Math.max(0, Math.min(shown, 0.5) - 0.25));
	int w3 = (int)(sz.x * Math.max(0, shown - 0.5));
	Coord p = Coord.z;
	g.chcolor(STAM_LO);
	g.frect(p, Coord.of(w1, sz.y));
	p = p.add(w1, 0);
	g.chcolor(STAM_MID);
	g.frect(p, Coord.of(w2, sz.y));
	p = p.add(w2, 0);
	g.chcolor(STAM_HI);
	g.frect(p, Coord.of(w3, sz.y));
	frame(g);
	label(g, String.format("%.0f%% Stam", value * 100.0));
    }
    
    private void drawenergy(GOut g) {
	double value = Math.max(0, meter.meter(0));
	g.chcolor(value < 0.30 ? ENERGY_LOW : ENERGY);
	g.frect(Coord.z, sz.mul(fill(value), 1));
	frame(g);
	label(g, energytext(value));
    }
    
    private String energytext(double value) {
	int cur = -1, max = ENERGY_MAX;
	if(meter.tip != null) {
	    Matcher matcher = NUMBERS.matcher(meter.tip);
	    if(matcher.find()) {
		cur = Integer.parseInt(matcher.group(1));
		if(matcher.find())
		    max = Integer.parseInt(matcher.group(1));
	    }
	}
	if(cur < 0)
	    cur = (int)Math.round(value * ENERGY_MAX);
	return String.format("%d/%d", cur, max);
    }
    
    private double fill(double value) {
	return Math.max(0, Math.min(1, value));
    }
}
