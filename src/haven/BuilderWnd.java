package haven;

import me.ender.ui.CFGBox;

import java.util.Locale;

public class BuilderWnd extends GameUI.Hidewnd {
    private static final int W = UI.scale(420);
    private static final int GAP = UI.scale(8);
    private static final int LABELW = UI.scale(135);
    private static final int SLIDERW = UI.scale(170);
    private static final int ENTRYW = UI.scale(64);
    private static final int[] ANGLE_VALS = {4, 5, 6, 8, 9, 10, 12, 15, 18, 20, 24, 30, 36, 40, 45, 60, 72, 90, 120, 180, 360};
    private final Control gridInt, gridReal;
    private final AngleControl angle;

    public BuilderWnd() {
	super(Coord.z, "Builder");
	int y = GAP;
	add(new Label("Object fine-placement granularity"), GAP, y);
	y += UI.scale(20);
	gridInt = new Control("Position (whole)", 0, 16, 1.0, true, true,
	    () -> Math.round(MapView.plobpgran),
	    val -> MapView.setPlaceGrid(Math.round(val)));
	y = addControl(gridInt, y);
	gridReal = new Control("Position (fine)", 0, 1000, 0.1, false, false,
	    () -> MapView.plobpgran,
	    MapView::setPlaceGrid);
	y = addControl(gridReal, y);
	angle = new AngleControl("Angle", 0, ANGLE_VALS.length - 1);
	y = addControl(angle, y);
	y += UI.scale(2);
	add(new CFGBox("Display grid-lines", CFG.SHOW_WORLD_GRID, null, true), GAP, y);
	add(new CFGBox("Make terrain flat", CFG.FLAT_TERRAIN, null, true), UI.scale(170), y);
	pack();
	hide();
    }

    private int addControl(Control control, int y) {
	add(new Label(control.name), GAP, y);
	control.entry = add(new TextEntry(ENTRYW, format(control.source.get(), control.whole)) {
	    public void activate(String text) {
		control.apply(text);
	    }
	}, W - ENTRYW - GAP, y);
	control.entry.canactivate = true;
	control.entry.dshow = true;
	control.entry.settip("Press Enter to apply");
	control.slider = add(new HSlider(SLIDERW, control.min, control.max, control.sliderValue(control.source.get())) {
	    public void changed() {
		control.applySlider(val);
	    }
	}, GAP + LABELW, y + UI.scale(2));
	return y + UI.scale(30);
    }

    public void tick(double dt) {
	super.tick(dt);
	gridInt.sync();
	gridReal.sync();
	angle.sync();
    }

    private static String format(double val, boolean whole) {
	if(whole)
	    return Integer.toString((int)Math.round(val));
	return String.format(Locale.ROOT, "%.1f", val);
    }

    private static int angleSliderValue(double gran) {
	int ret = 0;
	for(int i = 0; i < ANGLE_VALS.length; i++) {
	    if(Math.abs((gran * 2) - ANGLE_VALS[i]) < Math.abs((gran * 2) - ANGLE_VALS[ret]))
		ret = i;
	}
	return ret;
    }

    private static int angleDegrees(double gran) {
	return 360 / ANGLE_VALS[angleSliderValue(gran)];
    }

    private static int clamp(int val, int min, int max) {
	return Math.max(min, Math.min(max, val));
    }

    public void reqclose() {
	if(ConfigProfiles.active() == ConfigProfiles.Mode.BUILDER) {
	    show();
	    raise();
	} else {
	    CFG.SHOW_BUILDER_WINDOW.set(false);
	}
    }

    private interface Setter {
	void set(double val);
    }

    private interface Source {
	double get();
    }

    private static class Control {
	final String name;
	final int min, max;
	final double scale;
	final boolean whole;
	final boolean syncSlider;
	final Source source;
	final Setter setter;
	TextEntry entry;
	HSlider slider;

	Control(String name, int min, int max, double scale, boolean whole, boolean syncSlider, Source source, Setter setter) {
	    this.name = name;
	    this.min = min;
	    this.max = max;
	    this.scale = scale;
	    this.whole = whole;
	    this.syncSlider = syncSlider;
	    this.source = source;
	    this.setter = setter;
	}

	void apply(String text) {
	    try {
		apply(Double.parseDouble(text.trim()));
	    } catch(NumberFormatException ignored) {
		entry.settext(format(slider.val * scale, whole));
	    }
	}

	void apply(double val) {
	    if(whole)
		val = Math.round(val);
	    val = Math.max(min * scale, Math.min(max * scale, val));
	    slider.val = sliderValue(val);
	    entry.settext(format(val, whole));
	    setter.set(val);
	}

	void applySlider(int val) {
	    apply(val * scale);
	}

	void sync() {
	    sync(false);
	}

	void sync(boolean force) {
	    double val = source.get();
	    if(!syncSlider)
		return;
	    if(syncSlider) {
		int sval = sliderValue(val);
		if(slider.val != sval)
		    slider.val = sval;
	    }
	    if(force || !entry.hasfocus)
		entry.settext(format(val, whole));
	}

	int sliderValue(double val) {
	    return clamp((int)Math.round(val / scale), min, max);
	}
    }

    private static class AngleControl extends Control {
	AngleControl(String name, int min, int max) {
	    super(name, min, max, 1.0, true, true, () -> angleDegrees(MapView.plobagran), val -> MapView.setPlaceAngle(180.0 / val));
	}

	void apply(double val) {
	    val = Math.max(1, Math.min(90, Math.round(val)));
	    slider.val = sliderValue(val);
	    int deg = 360 / ANGLE_VALS[slider.val];
	    entry.settext(Integer.toString(deg));
	    setter.set(deg);
	}

	void applySlider(int val) {
	    slider.val = clamp(val, min, max);
	    int deg = 360 / ANGLE_VALS[slider.val];
	    entry.settext(Integer.toString(deg));
	    setter.set(deg);
	}

	int sliderValue(double val) {
	    double gran = 180.0 / Math.max(1, val);
	    return angleSliderValue(gran);
	}
    }
}
