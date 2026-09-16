package haven;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GuardedCombatSkills {
    private static final Text.Foundry FND = new Text.Foundry(Text.sans, 12).aa(true);
    private static final Guard[] GUARDS = {
	new MinEnemyOpening("Cleave", "paginae/atk/cleave", Buff.OPEN_RED, 35),
	new MinEnemyOpening("Full Circle", "paginae/atk/fullcircle", Buff.OPEN_RED, 35, false),
	new MinEnemyOpening("Sting", "paginae/atk/sting", Buff.OPEN_BLUE, 20),
	new EnemyBiggestAtLeast("Opp Knock", "paginae/atk/oppknock", "oppknock-min", 30),
	new EnemyBiggestAtMost("Opp Knock", "paginae/atk/oppknock", "oppknock-max", 94),
	new MinOwnOpening("Zig-Zag Ruse", "paginae/atk/zigzag", Buff.OPEN_RED, 10),
    };
    private static final Map<String, List<Guard>> byres = new HashMap<>();
    static {
	for(Guard guard : GUARDS)
	    byres.computeIfAbsent(guard.resname, res -> new LinkedList<>()).add(guard);
    }

    public static Set<String> defaultEnabled() {
	Set<String> ret = new HashSet<>();
	for(Guard guard : GUARDS) {
	    if(guard.defenabled)
		ret.add(guard.key);
	}
	return ret;
    }

    public static Map<String, Integer> defaultThresholds() {
	Map<String, Integer> ret = new HashMap<>();
	for(Guard guard : GUARDS)
	    ret.put(guard.key, guard.defthreshold);
	return ret;
    }

    public static boolean known(String resname) {
	return byres.containsKey(resname);
    }

    public static boolean enabled(String resname) {
	List<Guard> guards = byres.get(resname);
	if(guards == null)
	    return false;
	Set<String> enabled = CFG.GUARDED_COMBAT_SKILLS.get();
	for(Guard guard : guards) {
	    if(enabled.contains(guard.key) || enabled.contains(guard.resname))
		return true;
	}
	return false;
    }

    public static String label(String resname) {
	List<Guard> guards = byres.get(resname);
	Guard guard = (guards == null || guards.isEmpty()) ? null : guards.get(0);
	return guard == null ? resname : guard.name;
    }

    public static String reason(String resname, Fightsess fs, Fightview fv) {
	List<Guard> guards = byres.get(resname);
	if(guards == null)
	    return null;
	for(Guard guard : guards) {
	    if(ruleEnabled(guard) && !guard.canUse(fs, fv))
		return guard.reason(fs, fv);
	}
	return null;
    }

    public static boolean canUse(String resname, Fightsess fs, Fightview fv) {
	List<Guard> guards = byres.get(resname);
	if(guards == null)
	    return true;
	for(Guard guard : guards) {
	    if(ruleEnabled(guard) && !guard.canUse(fs, fv))
		return false;
	}
	return true;
    }

    public static void openSettings(UI ui) {
	if(ui == null || ui.gui == null)
	    return;
	ui.gui.add(new SettingsWindow(), ui.mc);
    }

    private static int threshold(Guard guard) {
	Map<String, Integer> vals = CFG.GUARDED_COMBAT_THRESHOLDS.get();
	Integer val = vals.get(guard.key);
	return Utils.clip(val == null ? guard.defthreshold : val, 0, 100);
    }

    private static void threshold(Guard guard, String text, TextEntry entry) {
	try {
	    int val = Utils.clip(Integer.parseInt(text.trim()), 0, 100);
	    Map<String, Integer> next = new HashMap<>(CFG.GUARDED_COMBAT_THRESHOLDS.get());
	    next.put(guard.key, val);
	    CFG.GUARDED_COMBAT_THRESHOLDS.set(next);
	    entry.settext(Integer.toString(val));
	    entry.commit();
	} catch(NumberFormatException e) {
	    entry.settext(Integer.toString(threshold(guard)));
	    entry.commit();
	}
    }

    private static abstract class Guard {
	final String name, resname, key;
	final int defthreshold;
	final boolean defenabled;

	Guard(String name, String resname, int defthreshold, boolean defenabled) {
	    this(name, resname, resname, defthreshold, defenabled);
	}

	Guard(String name, String resname, String key, int defthreshold, boolean defenabled) {
	    this.name = name;
	    this.resname = resname;
	    this.key = key;
	    this.defthreshold = defthreshold;
	    this.defenabled = defenabled;
	}

	abstract boolean canUse(Fightsess fs, Fightview fv);
	abstract String reason(Fightsess fs, Fightview fv);
	abstract String rulePrefix();
	abstract String ruleSuffix();
    }

    private static boolean ruleEnabled(Guard guard) {
	Set<String> enabled = CFG.GUARDED_COMBAT_SKILLS.get();
	return enabled.contains(guard.key) || enabled.contains(guard.resname);
    }

    private static class MinEnemyOpening extends Guard {
	final String opening;

	MinEnemyOpening(String name, String resname, String opening, int min) {
	    this(name, resname, opening, min, true);
	}

	MinEnemyOpening(String name, String resname, String opening, int min, boolean defenabled) {
	    super(name, resname, min, defenabled);
	    this.opening = opening;
	}

	boolean canUse(Fightsess fs, Fightview fv) {
	    return fs.enemyOpening(fv, opening) >= threshold(this);
	}

	String reason(Fightsess fs, Fightview fv) {
	    return String.format("%s waits for enemy %s to be at least %d%%", name, openingLabel(opening), threshold(this));
	}

	String rulePrefix() {return openingLabel(opening) + " at least ";}
	String ruleSuffix() {return "%";}
    }

    private static class MinOwnOpening extends Guard {
	final String opening;

	MinOwnOpening(String name, String resname, String opening, int min) {
	    super(name, resname, min, true);
	    this.opening = opening;
	}

	boolean canUse(Fightsess fs, Fightview fv) {
	    return fs.ownOpening(fv, opening) >= threshold(this);
	}

	String reason(Fightsess fs, Fightview fv) {
	    return String.format("%s waits for your %s to be at least %d%%", name, openingLabel(opening), threshold(this));
	}

	String rulePrefix() {return "your " + openingLabel(opening) + " at least ";}
	String ruleSuffix() {return "%";}
    }

    private static class EnemyBiggestAtLeast extends Guard {
	EnemyBiggestAtLeast(String name, String resname, String key, int min) {
	    super(name, resname, key, min, true);
	}

	boolean canUse(Fightsess fs, Fightview fv) {
	    return fs.enemyMaxOpening(fv) >= threshold(this);
	}

	String reason(Fightsess fs, Fightview fv) {
	    return String.format("%s waits for enemy biggest opening to be at least %d%%", name, threshold(this));
	}

	String rulePrefix() {return "biggest at least ";}
	String ruleSuffix() {return "%";}
    }

    private static class EnemyBiggestAtMost extends Guard {
	EnemyBiggestAtMost(String name, String resname, String key, int max) {
	    super(name, resname, key, max, true);
	}

	boolean canUse(Fightsess fs, Fightview fv) {
	    return fs.enemyMaxOpening(fv) <= threshold(this);
	}

	String reason(Fightsess fs, Fightview fv) {
	    return String.format("%s waits for enemy biggest opening to be at most %d%%", name, threshold(this));
	}

	String rulePrefix() {return "biggest at most ";}
	String ruleSuffix() {return "%";}
    }

    private static String openingLabel(String opening) {
	if(Buff.OPEN_RED.equals(opening)) return "red";
	if(Buff.OPEN_BLUE.equals(opening)) return "blue";
	if(Buff.OPEN_YELLOW.equals(opening)) return "yellow";
	if(Buff.OPEN_GREEN.equals(opening)) return "green";
	return opening;
    }

    private static class SettingsWindow extends Window {
	SettingsWindow() {
	    super(UI.scale(new Coord(320, 260)), "Guarded Skills");
	    justclose = true;
	    int y = UI.scale(8);
	    add(new Label("Queue these skills until their rule passes:"), UI.scale(8), y);
	    y += UI.scale(28);
	    for(Guard guard : GUARDS) {
		CheckBox box = new CheckBox(guard.name) {{
		    a = ruleEnabled(guard);
		    changed(val -> {
			Set<String> next = new HashSet<>(CFG.GUARDED_COMBAT_SKILLS.get());
			if(val)
			    next.add(guard.key);
			else {
			    next.remove(guard.key);
			    next.remove(guard.resname);
			}
			CFG.GUARDED_COMBAT_SKILLS.set(next);
		    });
		}};
		add(box, UI.scale(10), y);
		add(new Label(guard.rulePrefix(), FND), UI.scale(130), y + UI.scale(3));
		TextEntry threshold = add(new TextEntry(UI.scale(38), Integer.toString(threshold(guard))) {
		    public void activate(String text) {
			threshold(guard, text, this);
		    }
		}, UI.scale(220), y);
		add(new Label(guard.ruleSuffix(), FND), threshold.pos("ur").adds(4, 3));
		y += UI.scale(26);
	    }
	    add(new Label("Press Enter in a box to save.", FND), UI.scale(10), y + UI.scale(3));
	    y += UI.scale(25);
	    Button defaults = add(new Button(UI.scale(90), "Defaults", false), UI.scale(10), y);
	    defaults.action(() -> {
		CFG.GUARDED_COMBAT_SKILLS.set(defaultEnabled());
		CFG.GUARDED_COMBAT_THRESHOLDS.set(defaultThresholds());
		reqdestroy();
		openSettings(ui);
	    });
	    Button none = add(new Button(UI.scale(70), "None", false), defaults.pos("ur").adds(8, 0));
	    none.action(() -> {
		CFG.GUARDED_COMBAT_SKILLS.set(new HashSet<>());
		reqdestroy();
		openSettings(ui);
	    });
	    Button close = add(new Button(UI.scale(70), "Close", false), none.pos("ur").adds(8, 0));
	    close.action(this::reqdestroy);
	}
    }
}
