package haven;

public class MovableWidgetBox extends ResizableDraggableWidget {
    private final Frame frame;
    public final Widget child;
    private final SpeedBoxes speedBoxes;
    private final SpeedImages speedImages;
    private final SpeedButton[] speedButtons;
    private boolean layingOut = false;

    public MovableWidgetBox(String name, Widget child) {
	super("MovableWidgetBox:" + name, minsz(child));
	this.child = child;
	Coord inner = (child instanceof Speedget) ? SpeedLayout.displaysz() : child.sz;
	frame = add(new Frame(inner, true) {
	    @Override
	    public void cdestroy(Widget ch) {
		if(ch == MovableWidgetBox.this.child)
		    MovableWidgetBox.this.reqdestroy();
		else
		    super.cdestroy(ch);
	    }
	}, Coord.z);
	frame.add(child, frame.box.btloff());
	if(child instanceof Speedget) {
	    child.hide();
	    speedBoxes = frame.add(new SpeedBoxes(), frame.box.btloff());
	    speedImages = frame.add(new SpeedImages((Speedget)child), frame.box.btloff());
	    speedButtons = new SpeedButton[4];
	    for(int i = 0; i < speedButtons.length; i++)
		speedButtons[i] = frame.add(new SpeedButton((Speedget)child, i));
	    placeSpeedButtons();
	} else {
	    speedBoxes = null;
	    speedImages = null;
	    speedButtons = null;
	}
	resize(frame.sz);
	disposables.add(CFG.LOCK_FLOATING_STAT_WDGS.observe(this::updateState));
	updateState(null);
    }

    private static Coord minsz(Widget child) {
	if(child instanceof Speedget)
	    return SpeedLayout.displaysz().add(Window.wbox.bisz());
	return child.sz.add(Window.wbox.bisz()).max(UI.scale(32, 24));
    }

    private void updateState(CFG<Boolean> cfg) {
	boolean unlocked = !CFG.LOCK_FLOATING_STAT_WDGS.get();
	draggable(unlocked);
	resizable(unlocked);
    }

    private void placeSpeedButtons() {
	if(speedButtons == null)
	    return;
	Coord area = SpeedLayout.displaysz();
	Coord inner = frame.inner();
	Coord c = frame.box.btloff().add(inner.sub(area).div(2));
	child.move(c);
	child.resize(area);
	if(speedBoxes != null) {
	    speedBoxes.move(c);
	    speedBoxes.resize(area);
	}
	if(speedImages != null) {
	    speedImages.move(c);
	    speedImages.resize(area);
	}
	for(int i = 0; i < speedButtons.length; i++) {
	    Coord bc = SpeedLayout.boxc(i);
	    speedButtons[i].move(c.add(bc));
	    speedButtons[i].resize(SpeedLayout.boxsz());
	}
    }

    @Override
    protected void added() {
	super.added();
	if(speedBoxes != null) {
	    resize(sz);
	    updateCfg();
	}
    }

    @Override
    public void cresize(Widget ch) {
	if(layingOut)
	    return;
	if((ch == child) || (ch == frame)) {
	    Coord inner = (child instanceof Speedget) ? frame.inner().max(SpeedLayout.displaysz()) : child.sz;
	    layingOut = true;
	    try {
		frame.resize(inner.add(frame.box.bisz()));
		placeSpeedButtons();
	    } finally {
		layingOut = false;
	    }
	    resize(frame.sz);
	} else {
	    super.cresize(ch);
	}
    }

    @Override
    public void resize(Coord sz) {
	Coord nsz = sz.max(minsz(child));
	super.resize(nsz);
	if(frame != null) {
	    layingOut = true;
	    try {
		frame.resize(nsz);
		placeSpeedButtons();
	    } finally {
		layingOut = false;
	    }
	}
    }

    @Override
    public void draw(GOut g) {
	super.draw(g);
	drawresize(g);
    }

    @Override
    public boolean checkhit(Coord c) {
	return draggable() && frame.checkhit(c);
    }

    private static class SpeedLayout {
	private static final int GAP = UI.scale(2);
	private static final Coord BOX_PAD = UI.scale(1, 2);

	public static int slotw() {
	    int w = 0;
	    for(int i = 0; i < Speedget.imgs.length; i++)
		w = Math.max(w, Speedget.imgs[i][0].sz().x);
	    return(w + UI.scale(2));
	}

	public static int slotx(int slot) {
	    return(slot * (slotw() + GAP));
	}

	public static Coord displaysz() {
	    return(Coord.of((slotw() * 4) + (GAP * 3), Speedget.tsz.y));
	}

	public static Coord boxc(int slot) {
	    return(Coord.of(slotx(slot), 0).add(BOX_PAD));
	}

	public static Coord boxsz() {
	    return(Coord.of(slotw(), Speedget.tsz.y).sub(BOX_PAD.mul(2)));
	}
    }

    private static class SpeedBoxes extends Widget {
	public SpeedBoxes() {
	    super(SpeedLayout.displaysz());
	}

	public void draw(GOut g) {
	    for(int i = 0; i < 4; i++) {
		Coord sc = SpeedLayout.boxc(i);
		Coord bs = SpeedLayout.boxsz();
		g.chcolor(25, 16, 4, 180);
		g.frect(sc, bs);
		g.chcolor(238, 188, 68, 230);
		g.line(sc, sc.add(bs.x - 1, 0), 1);
		g.line(sc, sc.add(0, bs.y - 1), 1);
		g.line(sc.add(bs.x - 1, 0), sc.add(bs.x - 1, bs.y - 1), 1);
		g.line(sc.add(0, bs.y - 1), sc.add(bs.x - 1, bs.y - 1), 1);
	    }
	    g.chcolor();
	}
    }

    private static class SpeedImages extends Widget {
	private final Speedget speed;

	public SpeedImages(Speedget speed) {
	    super(SpeedLayout.displaysz());
	    this.speed = speed;
	}

	public boolean mousedown(MouseDownEvent ev) {
	    return(ev.b == 1);
	}

	public boolean mousewheel(MouseWheelEvent ev) {
	    return(speed.mousewheel(ev));
	}

	public void draw(GOut g) {
	    for(int i = 0; i < 4; i++) {
		Coord sc = SpeedLayout.boxc(i);
		Coord bs = SpeedLayout.boxsz();
		Tex tex;
		if(i == speed.cur)
		    tex = Speedget.imgs[i][2];
		else if(i > speed.max)
		    tex = Speedget.imgs[i][0];
		else
		    tex = Speedget.imgs[i][1];
		g.image(tex, sc.add((bs.x - tex.sz().x) / 2, (bs.y - tex.sz().y) / 2));
	    }
	    g.chcolor();
	}
    }

    private static class SpeedButton extends Widget {
	private final Speedget speed;
	private final int index;

	public SpeedButton(Speedget speed, int index) {
	    super(Coord.z);
	    this.speed = speed;
	    this.index = index;
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.b == 1) {
		speed.set(index);
		return(true);
	    }
	    return(super.mousedown(ev));
	}

	public Object tooltip(Coord c, Widget prev) {
	    if(index < Speedget.tips.length)
		return(Speedget.tips[index]);
	    return(super.tooltip(c, prev));
	}

	public boolean mousewheel(MouseWheelEvent ev) {
	    return(speed.mousewheel(ev));
	}

	public void draw(GOut g) {
	}
    }
}
