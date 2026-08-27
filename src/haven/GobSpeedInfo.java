package haven;

public class GobSpeedInfo extends GobInfo {
    private double savedSpeed = 0;

    public GobSpeedInfo(Gob owner) {
	super(owner);
	up(-2);
	center = new Pair<>(0.5, 0.0);
    }

    @Override
    protected boolean enabled() {
	return CFG.DISPLAY_GOB_SPEED.get() &&
	    !(gob.getattr(Moving.class) instanceof Following) &&
	    (gob.gobSpeed > 0);
    }

    @Override
    protected Tex render() {
        return Text.renderstroked(String.format("%.2f u/s", gob.gobSpeed)).tex();
    }

    @Override
    public void ctick(double dt) {
	synchronized(texLock) {
	    if(savedSpeed != gob.gobSpeed) {
		savedSpeed = gob.gobSpeed;
		clean();
		dirty();
	    }
	}
	super.ctick(dt);
    }
}
