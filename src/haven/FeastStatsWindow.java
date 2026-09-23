package haven;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FeastStatsWindow extends WindowX {
    private static final Pattern LEVEL_SUFFIX = Pattern.compile("^(.*?)(?:\\s*\\+(\\d+))?$");
    private static final int WIDTH = 255;
    private final GameUI gui;
    private final Map<Indir<Resource>, Integer> levelEvents = new HashMap<>();
    private final List<Widget> rows = new ArrayList<>();
    private boolean dirty = true;

    public FeastStatsWindow(GameUI gui) {
	super(UI.scale(WIDTH, 45), "Feast Gains");
	this.gui = gui;
	justclose = true;
    }

    public void statGained(Indir<Resource> event) {
	levelEvents.merge(event, 1, Integer::sum);
	dirty = true;
    }
    private static class EventName {
	final String stat;
	final int levels;

	EventName(String stat, int levels) {
	    this.stat = stat;
	    this.levels = levels;
	}
    }

    private static EventName eventName(Indir<Resource> resource) {
	String name = resource.get().flayer(BAttrWnd.FoodMeter.Event.class).nm;
	Matcher match = LEVEL_SUFFIX.matcher(name);
	if(!match.matches())
	    return new EventName(name, 1);
	return new EventName(match.group(1).trim(),
		(match.group(2) == null) ? 1 : Integer.parseInt(match.group(2)));
    }

    private void rebuild() {
	Map<String, Integer> gains = new TreeMap<>();
	for(Map.Entry<Indir<Resource>, Integer> entry : levelEvents.entrySet()) {
	    EventName event = eventName(entry.getKey());
	    gains.merge(event.stat, event.levels * entry.getValue(), Integer::sum);
	}

	rows.forEach(Widget::reqdestroy);
	rows.clear();
	int y = UI.scale(3);
	if(gains.isEmpty()) {
	    rows.add(add(new Label("No gains yet"), UI.scale(5), y));
	    y += UI.scale(22);
	} else {
	    Label stat = add(new Label("Stat"), UI.scale(5), y);
	    Label levels = add(new Label("Gained"), UI.scale(190), y);
	    stat.setcolor(Color.LIGHT_GRAY);
	    levels.setcolor(Color.LIGHT_GRAY);
	    rows.add(stat);
	    rows.add(levels);
	    y += UI.scale(20);
	    for(Map.Entry<String, Integer> entry : gains.entrySet()) {
		rows.add(add(new Label(entry.getKey()), UI.scale(5), y));
		rows.add(add(new Label("+" + entry.getValue()), UI.scale(190), y));
		y += UI.scale(18);
	    }
	}
	resize(UI.scale(WIDTH), Math.max(UI.scale(45), y + UI.scale(3)));
	dirty = false;
    }

    public void tick(double dt) {
	super.tick(dt);
	if(dirty) {
	    try {
		rebuild();
	    } catch(Loading ignored) {
	    }
	}
    }

    public void destroy() {
	Utils.setprefc("wndc-feast-gains", c);
	if(gui.feastStatsWindow == this)
	    gui.feastStatsWindow = null;
	super.destroy();
    }
}
