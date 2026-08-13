package haven;

public class MovableWidgetBox extends DraggableWidget {
    private final Frame frame;
    public final Widget child;

    public MovableWidgetBox(String name, Widget child) {
	super("MovableWidgetBox:" + name);
	this.child = child;
	frame = add(new Frame(child.sz, true), Coord.z);
	frame.add(child, frame.box.btloff());
	resize(frame.sz);
	disposables.add(CFG.LOCK_FLOATING_STAT_WDGS.observe(cfg -> draggable(!cfg.get())));
	draggable(!CFG.LOCK_FLOATING_STAT_WDGS.get());
    }

    @Override
    public void cresize(Widget ch) {
	if((ch == child) || (ch == frame)) {
	    frame.resize(child.sz.add(frame.box.bisz()));
	    resize(frame.sz);
	} else {
	    super.cresize(ch);
	}
    }

    @Override
    public boolean checkhit(Coord c) {
	return draggable() && frame.checkhit(c);
    }
}
