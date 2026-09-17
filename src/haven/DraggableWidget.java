package haven;

import java.awt.Color;

public class DraggableWidget extends Widget {
    private static final Text.Foundry DFND = new Text.Foundry(Text.sansbold, 10).aa(true);
    protected static final Color EDIT_LINE = new Color(86, 188, 255, 210);
    protected static final Color EDIT_FILL = new Color(24, 100, 190, 255);
    protected static final Color EDIT_ACTIVE = new Color(120, 220, 255, 255);
    protected static final Color EDIT_SNAP = new Color(255, 221, 64, 255);
    protected static final Color EDIT_PRECISION = new Color(255, 148, 32, 255);
    private static final Coord EYE_SZ = UI.scale(18, 14);
    
    private final String name;
    private UI.Grab dm;
    private Coord doff;
    protected WidgetCfg cfg;
    private boolean draggable = true;
    private boolean customPosition = false;
    private boolean contentsVisible = true;
    private Coord defaultSize;
    
    public DraggableWidget(String name) {
	this.name = name;
    }
    
    public void draggable(boolean draggable) {
	this.draggable = draggable;
	if(!draggable) {stop_dragging();}
    }
    
    public boolean draggable() {return draggable;}

    public boolean hasCustomPosition() {return customPosition;}

    public String widgetName() {return name;}

    public void resetPosition(Coord c) {
	this.c = c;
	customPosition = true;
	show();
	updateCfg();
    }

    public void resetSize() {
	WidgetCfg defaults = WidgetCfg.getDefault(name);
	Coord size = ((defaults != null) && (defaults.sz != null)) ? defaults.sz : defaultSize;
	if((size == null) || (size.x <= 0) || (size.y <= 0))
	    return;
	resize(size);
	updateCfg();
    }

    public boolean contentsVisible() {return contentsVisible;}

    public static boolean guiEditMode() {return !CFG.GUI_LOCK.get();}

    protected boolean initialContentsVisible() {
	return true;
    }

    protected boolean canEditMove() {
	return draggable && guiEditMode();
    }

    protected boolean moving() {
	return dm != null;
    }

    public static int editGridSize() {
	return Math.max(1, CFG.GUI_EDIT_GRID_SIZE.get());
    }

    public static int snap(int v) {
	if(!CFG.GUI_EDIT_GRID.get())
	    return v;
	int grid = editGridSize();
	return Math.round(v / (float)grid) * grid;
    }

    public static Coord snap(Coord c) {
	return Coord.of(snap(c.x), snap(c.y));
    }

    public static Coord snapBottomLeft(Coord c, Coord sz) {
	if(!CFG.GUI_EDIT_GRID.get())
	    return c;
	return Coord.of(snap(c.x), snap(c.y + sz.y) - sz.y);
    }
    
    private void stop_dragging() {
	if(dm != null) {
	    dm.remove();
	    dm = null;
	    customPosition = true;
	    updateCfg();
	}
    }

    protected Coord moveHandleSize() {
	return Coord.of(Math.max(UI.scale(18), (int)Math.round(sz.x * 0.60)),
			Math.max(UI.scale(14), (int)Math.round(sz.y * 0.60)));
    }

    protected Coord moveHandleCoord() {
	return sz.sub(moveHandleSize()).div(2);
    }

    protected boolean hitmove(Coord c) {
	return canEditMove() && c.isect(moveHandleCoord(), moveHandleSize());
    }

    protected Coord eyeCoord() {
	return Coord.of(Math.max(0, sz.x - EYE_SZ.x - UI.scale(3)), UI.scale(3));
    }

    protected boolean hitEye(Coord c) {
	return showEditEye() && guiEditMode() && c.isect(eyeCoord(), EYE_SZ);
    }

    protected boolean showEditEye() {
	return true;
	}

    private void toggleContentsVisible() {
	contentsVisible = !contentsVisible;
	updateCfg();
    }

    private void drawEye(GOut g) {
	Coord ec = eyeCoord();
	Coord cen = ec.add(EYE_SZ.div(2));
	g.chcolor(12, 16, 10, 225);
	g.frect(ec, EYE_SZ);
	g.chcolor(EDIT_LINE);
	g.line(ec, ec.add(EYE_SZ.x - 1, 0), 1);
	g.line(ec, ec.add(0, EYE_SZ.y - 1), 1);
	g.line(ec.add(EYE_SZ.x - 1, 0), ec.add(EYE_SZ.x - 1, EYE_SZ.y - 1), 1);
	g.line(ec.add(0, EYE_SZ.y - 1), ec.add(EYE_SZ.x - 1, EYE_SZ.y - 1), 1);
	g.line(ec.add(UI.scale(3), EYE_SZ.y / 2), cen, 1);
	g.line(cen, ec.add(EYE_SZ.x - UI.scale(3), EYE_SZ.y / 2), 1);
	g.chcolor(contentsVisible ? EDIT_ACTIVE : EDIT_PRECISION);
	g.fellipse(cen, UI.scale(3, 3));
	if(!contentsVisible) {
	    g.chcolor(255, 148, 32, 255);
	    g.line(ec.add(UI.scale(3), EYE_SZ.y - UI.scale(3)), ec.add(EYE_SZ.x - UI.scale(3), UI.scale(3)), 1);
	}
	g.chcolor();
    }

    protected void drawEditOverlay(GOut g) {
	if(!guiEditMode() || !visible)
	    return;
	int t = UI.scale(8);
	Coord br = sz.sub(1, 1);
	g.chcolor(EDIT_LINE);
	g.line(Coord.z, Coord.of(br.x, 0), 1);
	g.line(Coord.z, Coord.of(0, br.y), 1);
	g.line(Coord.of(br.x, 0), br, 1);
	g.line(Coord.of(0, br.y), br, 1);
	g.frect(Coord.z, Coord.of(t, 2));
	g.frect(Coord.z, Coord.of(2, t));
	g.frect(Coord.of(sz.x - t, 0), Coord.of(t, 2));
	g.frect(Coord.of(sz.x - 2, 0), Coord.of(2, t));
	g.frect(Coord.of(0, sz.y - 2), Coord.of(t, 2));
	g.frect(Coord.of(0, sz.y - t), Coord.of(2, t));
	g.frect(sz.sub(t, 2), Coord.of(t, 2));
	g.frect(sz.sub(2, t), Coord.of(2, t));
	g.chcolor(EDIT_FILL);
	g.frect(moveHandleCoord(), moveHandleSize());
	g.chcolor(EDIT_LINE);
	Tex tdim = Text.renderstroked(String.format("%dpx by %dpx", sz.x, sz.y), DFND).tex();
	g.aimage(tdim, sz.div(2), 0.5, 0.5);
	g.chcolor();
	if(showEditEye())
	    drawEye(g);
    }

    @Override
    public void draw(GOut g) {
	if(contentsVisible)
	    super.draw(g);
	drawEditOverlay(g);
    }
    
    @Override
    public boolean mousedown(MouseDownEvent ev) {
	if((ev.b == 1) && hitEye(ev.c)) {
	    toggleContentsVisible();
	    parent.setfocus(this);
	    return true;
	}
	if(hitmove(ev.c)) {
	    if(ev.b == 1) {
		dm = ui.grabmouse(this);
		doff = ev.c;
	    }
	    parent.setfocus(this);
	    return true;
	}
	if(!contentsVisible)
	    return false;
	if(ev.propagate(this)) {
	    parent.setfocus(this);
	    return true;
	}
	return false;
    }
    
    @Override
    public boolean mouseup(MouseUpEvent ev) {
	if(dm != null) {
	    stop_dragging();
	} else {
	    return super.mouseup(ev);
	}
	return (true);
    }
    
    @Override
    public void mousemove(MouseMoveEvent ev) {
	if(dm != null) {
	    this.c = snapBottomLeft(this.c.add(ev.c.add(doff.inv())), sz);
	} else {
	    super.mousemove(ev);
	}
    }
    
    protected void added() {
	initCfg();
    }
    
    protected void initCfg() {
	if(defaultSize == null)
	    defaultSize = sz;
	cfg = WidgetCfg.get(name);
	customPosition = (cfg != null) && cfg.getValue("custom-position", false);
	contentsVisible = !showEditEye() || ((cfg == null) ? initialContentsVisible() :
		cfg.getValue("contents-visible", initialContentsVisible()));
	if(cfg != null) {
	    c = cfg.c == null ? c : cfg.c;
	    sz = cfg.sz == null ? sz : cfg.sz;
	    if(!showEditEye() && !cfg.getValue("contents-visible", true))
		updateCfg();
	} else {
	    updateCfg();
	}
    }
    
    protected void updateCfg() {
	setCfg();
	storeCfg();
    }
    
    protected void setCfg() {
	if(cfg == null) {
	    cfg = new WidgetCfg();
	}
	cfg.c = c;
	cfg.sz = sz;
	cfg.setValue("custom-position", customPosition);
	cfg.setValue("contents-visible", contentsVisible);
    }
    
    protected void storeCfg() {
	WidgetCfg.set(name, cfg);
    }
}
