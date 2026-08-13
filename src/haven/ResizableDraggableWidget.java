package haven;

public class ResizableDraggableWidget extends DraggableWidget {
    private static final Coord HANDLE = UI.scale(10, 10);
    private final Coord minsz;
    private UI.Grab rdm;
    private Coord rdoff;
    private Coord rdsz;
    private boolean resizable = true;

    public ResizableDraggableWidget(String name, Coord minsz) {
	super(name);
	this.minsz = minsz;
    }

    public void resizable(boolean resizable) {
	this.resizable = resizable;
	if(!resizable) {stop_resizing();}
    }

    protected boolean hitresize(Coord c) {
	return resizable && c.isect(sz.sub(HANDLE), HANDLE);
    }

    private void stop_resizing() {
	if(rdm != null) {
	    rdm.remove();
	    rdm = null;
	    updateCfg();
	}
    }

    @Override
    public boolean mousedown(MouseDownEvent ev) {
	if((ev.b == 1) && hitresize(ev.c)) {
	    rdm = ui.grabmouse(this);
	    rdoff = ev.c;
	    rdsz = sz;
	    parent.setfocus(this);
	    return true;
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
	    Coord nsz = rdsz.add(ev.c.sub(rdoff));
	    resize(Math.max(minsz.x, nsz.x), Math.max(minsz.y, nsz.y));
	} else {
	    super.mousemove(ev);
	}
    }

    protected void drawresize(GOut g) {
	if(!resizable) {return;}
	Coord br = sz.sub(1, 1);
	g.chcolor(255, 255, 255, 160);
	g.line(br.sub(UI.scale(8), 0), br, 1);
	g.line(br.sub(UI.scale(4), 0), br.sub(0, UI.scale(4)), 1);
	g.chcolor();
    }
}
