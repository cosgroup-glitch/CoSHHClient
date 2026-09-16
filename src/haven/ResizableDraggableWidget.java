package haven;

public class ResizableDraggableWidget extends DraggableWidget {
    private static final Text.Foundry DFND = new Text.Foundry(Text.sansbold, 10).aa(true);
    private static final Coord HANDLE = UI.scale(10, 10);
    private final Coord minsz;
    private UI.Grab rdm;
    private Coord rdoff;
    private Coord rdsz;
    private Coord rdpos;
    private Coord rdc;
    private Handle resizeHandle = null;
    private boolean resizable = true;

    public ResizableDraggableWidget(String name, Coord minsz) {
	super(name);
	this.minsz = minsz;
    }

    public void resizable(boolean resizable) {
	this.resizable = resizable;
	if(!resizable) {stop_resizing();}
    }

    protected boolean canEditResize() {
	return resizable && guiEditMode();
    }

    protected boolean hitresize(Coord c) {
	return hitResizeHandle(c) != null;
    }

    private Handle hitResizeHandle(Coord c) {
	if(!canEditResize())
	    return null;
	for(Handle h : Handle.values()) {
	    if(c.isect(handleCoord(h), HANDLE))
		return h;
	}
	return null;
    }

    private Handle hitPrecisionHandle(Coord c) {
	if(!canEditResize())
	    return null;
	for(Handle h : Handle.values()) {
	    if(h.side() && c.isect(precisionCoord(h), HANDLE))
		return h;
	}
	return null;
    }

    private Coord handleCoord(Handle h) {
	switch(h) {
	case N: return Coord.of((sz.x - HANDLE.x) / 2, 0);
	case S: return Coord.of((sz.x - HANDLE.x) / 2, sz.y - HANDLE.y);
	case W: return Coord.of(0, (sz.y - HANDLE.y) / 2);
	case E: return Coord.of(sz.x - HANDLE.x, (sz.y - HANDLE.y) / 2);
	case NW: return Coord.z;
	case NE: return Coord.of(sz.x - HANDLE.x, 0);
	case SW: return Coord.of(0, sz.y - HANDLE.y);
	case SE: return sz.sub(HANDLE);
	default: return Coord.z;
	}
    }

    private Coord precisionCoord(Handle h) {
	Coord gap = UI.scale(2, 2);
	Coord hc = handleCoord(h);
	switch(h) {
	case N:
	case S:
	    return Coord.of(Math.min(sz.x - HANDLE.x, hc.x + HANDLE.x + gap.x), hc.y);
	case W:
	case E:
	    return Coord.of(hc.x, Math.min(sz.y - HANDLE.y, hc.y + HANDLE.y + gap.y));
	default:
	    return hc;
	}
    }

    private void stop_resizing() {
	if(rdm != null) {
	    rdm.remove();
	    rdm = null;
	    resizeHandle = null;
	    rdpos = null;
	    rdc = null;
	    updateCfg();
	}
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
	if((ev.b == 1) && hitEye(ev.c))
	    return super.mousedown(ev);
	if(ev.b == 1) {
	    Handle precision = hitPrecisionHandle(ev.c);
	    if(precision != null) {
		resizeHandle = precision;
		showSizePrompt(precision);
		parent.setfocus(this);
		return true;
	    }
	    Handle h = hitResizeHandle(ev.c);
	    if(h != null) {
		rdoff = ev.c;
		rdsz = sz;
		rdc = c;
		rdpos = c.add(ev.c);
		resizeHandle = h;
		rdm = ui.grabmouse(this);
		parent.setfocus(this);
		return true;
	    }
	}
	return super.mousedown(ev);
    }

    @Override
    public boolean mouseup(MouseUpEvent ev) {
	if(rdm != null) {
	    stop_resizing();
	    return true;
	}
	return super.mouseup(ev);
    }

    @Override
    public void mousemove(MouseMoveEvent ev) {
	if(rdm != null) {
	    Coord d = c.add(ev.c).sub(rdpos);
	    int w = rdsz.x, h = rdsz.y;
	    int x = rdc.x, y = rdc.y;
	    if(resizeHandle.east)
		w += d.x;
	    if(resizeHandle.west) {
		w -= d.x;
		x += d.x;
	    }
	    if(resizeHandle.south)
		h += d.y;
	    if(resizeHandle.north) {
		h -= d.y;
		y += d.y;
	    }
	    if(w < minsz.x) {
		if(resizeHandle.west)
		    x -= minsz.x - w;
		w = minsz.x;
	    }
	    if(h < minsz.y) {
		if(resizeHandle.north)
		    y -= minsz.y - h;
		h = minsz.y;
	    }
	    if(CFG.GUI_EDIT_GRID.get()) {
		w = snap(w);
		h = snap(h);
		x = snap(x);
		y = snap(y);
	    }
	    if(resizeHandle.west || resizeHandle.north)
		move(Coord.of(x, y));
	    resize(Math.max(minsz.x, w), Math.max(minsz.y, h));
	} else {
	    super.mousemove(ev);
	}
    }

    protected void drawresize(GOut g) {
	if(!canEditResize()) {return;}
	for(Handle h : Handle.values()) {
	    g.chcolor(h == resizeHandle ? EDIT_ACTIVE : ((h == Handle.SW) && CFG.GUI_EDIT_GRID.get()) ? EDIT_SNAP : EDIT_FILL);
	    g.frect(handleCoord(h), HANDLE);
	}
	g.chcolor(EDIT_PRECISION);
	for(Handle h : Handle.values()) {
	    if(h.side())
		g.frect(precisionCoord(h), HANDLE);
	}
	if(moving()) {
	    g.chcolor(EDIT_LINE);
	    String dim = String.format("%dx%d", sz.x, sz.y);
	    Tex t = Text.renderstroked(dim, DFND).tex();
	    g.aimage(t, Coord.of(sz.x / 2, Math.max(t.sz().y / 2, -UI.scale(2))), 0.5, 1.0);
	}
	g.chcolor();
    }

    private void showSizePrompt(Handle handle) {
	SizePrompt wnd = new SizePrompt(this, handle);
	ui.gui.add(wnd, this.rootpos().add(UI.scale(14, 14)));
	wnd.raise();
    }

    private void applySize(Handle handle, String text) {
	try {
	    int v = Integer.parseInt(text.trim());
	    if(handle.north || handle.south)
		resize(sz.x, Math.max(minsz.y, v));
	    else
		resize(Math.max(minsz.x, v), sz.y);
	    updateCfg();
	} catch(NumberFormatException ignored) {
	}
	resizeHandle = null;
    }

    private enum Handle {
	N(false, true, false, true),
	S(false, false, false, true),
	W(true, false, false, false),
	E(false, false, true, false),
	NW(true, true, false, false),
	NE(false, true, true, false),
	SW(true, false, false, true),
	SE(false, false, true, true);

	final boolean west, north, east, south;
	Handle(boolean west, boolean north, boolean east, boolean south) {
	    this.west = west;
	    this.north = north;
	    this.east = east;
	    this.south = south;
	}

	boolean side() {
	    int n = 0;
	    if(west) n++;
	    if(north) n++;
	    if(east) n++;
	    if(south) n++;
	    return n == 1;
	}
    }

    private static class SizePrompt extends Window {
	private final ResizableDraggableWidget target;
	private final Handle handle;
	private final TextEntry entry;

	SizePrompt(ResizableDraggableWidget target, Handle handle) {
	    super(UI.scale(220, 58), "Widget size");
	    this.target = target;
	    this.handle = handle;
	    justclose = true;
	    adda(new Label(handle.north || handle.south ? "Height px:" : "Width px:"), UI.scale(8, 10), 0, 0);
	    entry = add(new TextEntry(UI.scale(92), handle.north || handle.south ? Integer.toString(target.sz.y) : Integer.toString(target.sz.x)) {
		public void activate(String text) {
		    apply();
		}
	    }, UI.scale(84, 4));
	    entry.canactivate = true;
	    add(new Button(UI.scale(54), "Apply", false, this::apply), UI.scale(84, 31));
	}

	private void apply() {
	    target.applySize(handle, entry.text());
	    reqdestroy();
	}

	public void reqdestroy() {
	    target.resizeHandle = null;
	    super.reqdestroy();
	}
    }
}
