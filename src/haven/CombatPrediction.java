package haven;

import me.ender.Reflect;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class CombatPrediction {
    private static class Attack {
        final Set<String> openings;
	final double value;
	final boolean weapon;

	Attack(double value, boolean weapon, String... openings) {
	    this.openings = new HashSet<>(Arrays.asList(openings));
	    this.value = value;
	    this.weapon = weapon;
	}
    }

    private static final Map<String, Double> COOLDOWNS = new HashMap<>();
    private static final Map<String, Attack> ATTACKS = new HashMap<>();
    private static final Map<GameUI, WeaponStats> weaponStats = new WeakHashMap<>();

    private static class WeaponStats {
	long seq = -1;
	double cooldown = 1.0;
	int damage;
	double quality;
    }

    static {
	COOLDOWNS.put("flex", 30.0); COOLDOWNS.put("gojug", 40.0); COOLDOWNS.put("haymaker", 50.0);
	COOLDOWNS.put("kick", 45.0); COOLDOWNS.put("knockteeth", 35.0); COOLDOWNS.put("lefthook", 40.0);
	COOLDOWNS.put("lowblow", 50.0); COOLDOWNS.put("oppknock", 45.0); COOLDOWNS.put("pow", 30.0);
	COOLDOWNS.put("ripapart", 60.0); COOLDOWNS.put("stealthunder", 40.0); COOLDOWNS.put("takedown", 50.0);
	COOLDOWNS.put("uppercut", 30.0); COOLDOWNS.put("cleave", 80.0); COOLDOWNS.put("chop", 40.0);
	COOLDOWNS.put("barrage", 20.0); COOLDOWNS.put("ravenbite", 40.0); COOLDOWNS.put("sideswipe", 25.0);
	COOLDOWNS.put("sting", 50.0);

	attack("fullcircle", 1.0, true, Buff.OPEN_RED, Buff.OPEN_YELLOW);
	attack("knockteeth", 30, false, Buff.OPEN_RED);
	attack("cleave", 1.5, true, Buff.OPEN_RED, Buff.OPEN_BLUE);
	attack("gojug", 40, false, Buff.OPEN_RED, Buff.OPEN_GREEN);
	attack("chop", 1.0, true, Buff.OPEN_GREEN);
	attack("haymaker", 20, false, Buff.OPEN_YELLOW);
	attack("kick", 25, false, Buff.OPEN_YELLOW);
	attack("lefthook", 15, false, Buff.OPEN_BLUE);
	attack("lowblow", 20, false, Buff.OPEN_BLUE);
	attack("pow", 10, false, Buff.OPEN_GREEN);
	attack("punchboth", 10, false, Buff.OPEN_GREEN, Buff.OPEN_YELLOW);
	attack("barrage", 0.25, true, Buff.OPEN_RED);
	attack("ravenbite", 1.1, true, Buff.OPEN_GREEN, Buff.OPEN_YELLOW);
	attack("ripapart", 50, false, Buff.OPEN_GREEN, Buff.OPEN_YELLOW, Buff.OPEN_RED, Buff.OPEN_BLUE);
	attack("sideswipe", 0.75, true, Buff.OPEN_YELLOW);
	attack("sting", 1.25, true, Buff.OPEN_GREEN, Buff.OPEN_BLUE);
	attack("sos", 1.0, true, Buff.OPEN_YELLOW, Buff.OPEN_BLUE);
	attack("takedown", 40, false, Buff.OPEN_YELLOW, Buff.OPEN_RED);
	attack("uppercut", 30, false, Buff.OPEN_GREEN, Buff.OPEN_BLUE);
    }

    private static void attack(String name, double value, boolean weapon, String... openings) {
	ATTACKS.put(name, new Attack(value, weapon, openings));
    }

    public static void updateAgility(GameUI gui, Fightview.Relation target, Indir<Resource> action, double observed) {
	if(target == null || action == null)
	    return;
	try {
	    Double base = COOLDOWNS.get(action.get().basename());
	    if(base == null)
		return;
	    double effective = base * (isWeaponAttack(action.get().basename()) ? weaponCooldown(gui) : 1.0);
	    double minimumObserved = Math.round(effective * Math.pow(0.5, 1.0 / 7.0));
	    double min = (observed <= minimumObserved) ? 0 : Math.pow(Math.max(0, observed - 0.5) / effective, 7);
	    double max = Math.pow((observed + 0.5) / effective, 7);
	    double narrowedMin = Math.max(target.minAgi, Math.min(2, min));
	    double narrowedMax = Math.min(target.maxAgi, Math.min(2, max));
	    if(narrowedMin <= narrowedMax) {
		target.minAgi = narrowedMin;
		target.maxAgi = narrowedMax;
	    }
	} catch(Loading ignored) {
	}
    }

    public static String agility(Fightview.Relation target) {
	if(target == null)
	    return "Agi: unknown";
	double min = round3(target.minAgi), max = round3(target.maxAgi);
	if(min <= 0 && max >= 2)
	    return "Agi: unknown";
	if(min <= 0)
	    return String.format("Agi: <%.3fx", max);
	if(max >= 2)
	    return String.format("Agi: >%.3fx", min);
	return String.format("Agi: %.3fx - %.3fx", min, max);
    }

    public static Integer damage(GameUI gui, Fightview.Relation target, Indir<Resource> action) {
	if(gui == null || target == null || action == null)
	    return null;
	try {
	    Attack attack = ATTACKS.get(action.get().basename());
	    if(attack == null)
		return null;
	    double opening = combinedOpening(target, attack.openings);
	    double full;
	    if(attack.weapon) {
		Double weapon = weaponDamage(gui);
		if(weapon == null)
		    return null;
		full = weapon * attack.value;
	    } else {
		full = attack.value * Math.sqrt(gui.ui.sess.glob.getcattr("str").comp / 10.0);
	    }
	    return (int)Math.ceil(full * opening * opening);
	} catch(Loading ignored) {
	    return null;
	}
    }

    private static double combinedOpening(Fightview.Relation target, Set<String> names) {
	double closed = 1.0;
	for(Buff buff : target.buffs.children(Buff.class)) {
	    try {
		if(names.contains(buff.res.get().name))
		    closed *= 1.0 - (Math.max(0, buff.ameter()) / 100.0);
	    } catch(Loading ignored) {
	    }
	}
	return 1.0 - closed;
    }

    private static boolean isWeaponAttack(String name) {
	Attack attack = ATTACKS.get(name);
	return attack != null && attack.weapon;
    }

    private static double weaponCooldown(GameUI gui) {
	return weaponStats(gui).cooldown;
    }

    private static Double weaponDamage(GameUI gui) {
	WeaponStats stats = weaponStats(gui);
	if(stats.damage <= 0 || stats.quality <= 0)
	    return null;
	return stats.damage * Math.pow(gui.ui.sess.glob.getcattr("str").comp / stats.quality, 0.25);
    }

    private static WeaponStats weaponStats(GameUI gui) {
	WeaponStats stats = weaponStats.computeIfAbsent(gui, key -> new WeaponStats());
	if(gui.equipory == null || stats.seq == gui.equipory.seq)
	    return stats;
	WeaponStats fresh = new WeaponStats();
	WItem left = gui.equipory.slots[Equipory.SLOTS.HAND_LEFT.idx];
	WItem right = gui.equipory.slots[Equipory.SLOTS.HAND_RIGHT.idx];
	for(WItem item : Arrays.asList(left, right)) {
	    if(item == null)
		continue;
	    boolean weapon = false;
	    double cooldown = 1.0;
	    for(ItemInfo info : item.item.info()) {
		if(isDamageInfo(info)) {
		    fresh.damage = Reflect.getFieldValueInt(info, "dmg");
		    weapon = true;
		} else if(isInfo(info, "Coolmod") && Reflect.hasField(info, "mod")) {
		    Double mod = Reflect.getFieldValue(info, "mod", Double.class);
		    if(mod != null && mod > 0)
			cooldown = mod;
		}
	    }
	    if(weapon) {
		fresh.cooldown = cooldown;
		fresh.quality = item.quality();
		break;
	    }
	}
	fresh.seq = gui.equipory.seq;
	weaponStats.put(gui, fresh);
	return fresh;
    }

    private static boolean isDamageInfo(ItemInfo info) {
	return isInfo(info, "Damage") || Reflect.hasField(info, "dmg");
    }

    private static boolean isInfo(ItemInfo info, String name) {
	return info.getClass().getSimpleName().equals(name) || info.getClass().getName().equals(name);
    }

    private static double round3(double value) {
	return Math.round(value * 1000.0) / 1000.0;
    }
}
