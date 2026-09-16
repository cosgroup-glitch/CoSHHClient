package haven;

import haven.res.ui.tt.wpn.info.WeaponInfo;
import me.ender.Reflect;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CombatWeaponRange {
    private static final Pattern NUMBER = Pattern.compile("(-?\\d+(?:\\.\\d+)?)");

    public static double equippedRange(GameUI gui) {
	if(gui == null || gui.equipory == null)
	    return Double.NaN;
	double range = Double.NaN;
	range = Math.max(rangeOrZero(range), rangeOrZero(slotRange(gui.equipory, Equipory.SLOTS.HAND_LEFT)));
	range = Math.max(rangeOrZero(range), rangeOrZero(slotRange(gui.equipory, Equipory.SLOTS.HAND_RIGHT)));
	return range > 0 ? range : Double.NaN;
    }

    public static String equippedName(GameUI gui) {
	if(gui == null || gui.equipory == null)
	    return null;
	String left = slotName(gui.equipory, Equipory.SLOTS.HAND_LEFT);
	String right = slotName(gui.equipory, Equipory.SLOTS.HAND_RIGHT);
	if(left != null && right != null)
	    return left.equals(right) ? left : left + " / " + right;
	return left != null ? left : right;
    }

    public static double displayDistance(double rawDistance, double weaponRange) {
	return usesMapUnits(weaponRange) ? rawDistance / 11.0 : rawDistance;
    }

    public static boolean withinRange(double rawDistance, double weaponRange) {
	if(Double.isNaN(rawDistance) || Double.isNaN(weaponRange) || weaponRange <= 0)
	    return false;
	return displayDistance(rawDistance, weaponRange) <= weaponRange + 0.1;
    }

    public static boolean usesMapUnits(double weaponRange) {
	return !Double.isNaN(weaponRange) && weaponRange <= 20.0;
    }

    public static double gobRange(Gob gob) {
	if(gob == null)
	    return Double.NaN;
	GameUI gui = gob.context(GameUI.class);
	if(Boolean.TRUE.equals(gob.isMe())) {
	    double range = equippedRange(gui);
	    if(!Double.isNaN(range))
		return range;
	}
	return guessedRange(gob.equippedOverlayResNames());
    }

    public static double rawGobRange(Gob gob) {
	double range = gobRange(gob);
	if(Double.isNaN(range))
	    return Double.NaN;
	return usesMapUnits(range) ? range * 11.0 : range;
    }

    private static double guessedRange(List<String> resnames) {
	double best = Double.NaN;
	for(String resname : resnames) {
	    double range = guessedRange(resname);
	    if(range > rangeOrZero(best))
		best = range;
	}
	return best;
    }

    private static double guessedRange(String resname) {
	if(resname == null)
	    return Double.NaN;
	String name = resname.toLowerCase(Locale.ROOT);
	if(name.contains("boarspear") || name.contains("spear"))
	    return 4.5;
	if(name.contains("b12axe"))
	    return 3.0;
	if(name.contains("dagger"))
	    return 2.0;
	if(name.contains("bronzesword") || name.contains("fyrdsword") || name.contains("hirdsword") ||
	    name.contains("sword") || name.contains("cutblade"))
	    return 3.0;
	return Double.NaN;
    }

    private static double slotRange(Equipory equipory, Equipory.SLOTS slot) {
	WItem w = equipory.slots[slot.idx];
	if(w == null || w.item == null)
	    return Double.NaN;
	try {
	    return itemRange(w.item.info());
	} catch(Loading ignored) {
	    return Double.NaN;
	}
    }

    private static String slotName(Equipory equipory, Equipory.SLOTS slot) {
	WItem w = equipory.slots[slot.idx];
	if(w == null || w.item == null)
	    return null;
	try {
	    String name = w.item.name.get(null);
	    return (name == null || name.isEmpty()) ? prettyResName(w.item.resname()) : name;
	} catch(Loading ignored) {
	    return null;
	}
    }

    private static double itemRange(List<ItemInfo> infos) {
	for(ItemInfo info : infos) {
	    if(!isRangeInfo(info))
		continue;
	    double range = numericRange(info);
	    if(range > 0)
		return normalizeRange(range);
	    if(info instanceof WeaponInfo) {
		range = parseRange(((WeaponInfo)info).wpntips());
		if(range > 0)
		    return normalizeRange(range);
	    }
	}
	return Double.NaN;
    }

    private static double normalizeRange(double range) {
	/* Weapon tooltips report range as percent; 120% is about 1.5 map tiles. */
	return range > 20.0 ? range / 80.0 : range;
    }

    private static boolean isRangeInfo(ItemInfo info) {
	return Reflect.is(info, "Range") || Reflect.like(info, ".Range") || info.getClass().getSimpleName().equals("Range");
    }

    private static double numericRange(Object info) {
	Class<?> cls = info.getClass();
	while(cls != null) {
	    for(Field field : cls.getDeclaredFields()) {
		Class<?> type = field.getType();
		if(!(type == int.class || type == long.class || type == float.class || type == double.class ||
		    Number.class.isAssignableFrom(type)))
		    continue;
		try {
		    field.setAccessible(true);
		    Object value = field.get(info);
		    if(value instanceof Number) {
			double range = ((Number)value).doubleValue();
			if(range > 0)
			    return range;
		    }
		} catch(IllegalAccessException ignored) {}
	    }
	    cls = cls.getSuperclass();
	}
	return Double.NaN;
    }

    private static double parseRange(String text) {
	if(text == null)
	    return Double.NaN;
	Matcher matcher = NUMBER.matcher(text);
	return matcher.find() ? Utils.dv(matcher.group(1)) : Double.NaN;
    }

    private static double rangeOrZero(double range) {
	return Double.isNaN(range) ? 0 : range;
    }

    private static String prettyResName(String resname) {
	if(resname == null || resname.isEmpty())
	    return null;
	int idx = resname.lastIndexOf('/');
	String name = (idx >= 0) ? resname.substring(idx + 1) : resname;
	return name.isEmpty() ? null : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }
}
