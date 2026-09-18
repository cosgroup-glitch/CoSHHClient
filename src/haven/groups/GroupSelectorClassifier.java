package haven.groups;

import haven.BuddyWnd;
import haven.MapWnd;
import haven.Polity;
import haven.Widget;

import java.util.WeakHashMap;

public class GroupSelectorClassifier {
    private static final String CLASS_VILLAGE = "haven.res.ui.vlg.Village";
    private static final String CLASS_LANDWINDOW = "haven.res.ui.land.Landwindow";
    private static final String CLASS_FIELDCAIRN = "haven.res.ui.sar.Administer";

    private static final WeakHashMap<BuddyWnd.GroupSelector, GroupSelectorCompanion> companions = new WeakHashMap<>();

    private static final class Target {
	final GroupLabels.Scope scope;
	final String owner;
	final int lo, hi;
	final boolean warnAboveQuick;

	Target(GroupLabels.Scope scope, String owner, int lo, int hi, boolean warnAboveQuick) {
	    this.scope = scope;
	    this.owner = owner;
	    this.lo = lo;
	    this.hi = hi;
	    this.warnAboveQuick = warnAboveQuick;
	}
    }

    public static void attached(BuddyWnd.GroupSelector selector) {
	synchronized(companions) {
	    if(companions.containsKey(selector))
		return;
	}
	Target target = classify(selector);
	if(target == null)
	    return;
	GroupSelectorCompanion companion = new GroupSelectorCompanion(selector, target.scope, target.owner,
		target.lo, target.hi, target.warnAboveQuick);
	synchronized(companions) {
	    companions.put(selector, companion);
	}
	selector.hide();
	selector.parent.add(companion, selector.c);
    }

    public static void detached(BuddyWnd.GroupSelector selector) {
	GroupSelectorCompanion companion;
	synchronized(companions) {
	    companion = companions.remove(selector);
	}
	if(companion != null)
	    companion.destroy();
    }

    private static String chrid(Widget widget) {
	haven.GameUI gui = widget.getparent(haven.GameUI.class);
	return((gui == null) ? "" : gui.chrid);
    }

    private static Target classify(BuddyWnd.GroupSelector selector) {
	if(selector.getparent(MapWnd.class) != null)
	    return(null);
	if(selector.getparent(BuddyWnd.BuddyInfo.class) != null)
	    return(new Target(GroupLabels.Scope.KIN, chrid(selector), 0, BuddyWnd.ncolors - 1, false));

	Polity polity = selector.getparent(Polity.class);
	if(polity != null) {
	    if(CLASS_VILLAGE.equals(polity.getClass().getName()))
		return(new Target(GroupLabels.Scope.VILLAGE, polity.name, 0, BuddyWnd.ncolors - 1, true));
	    return(null);
	}

	Widget parent = selector.parent;
	if(parent != null) {
	    String type = parent.getClass().getName();
	    if(CLASS_LANDWINDOW.equals(type))
		return(new Target(GroupLabels.Scope.KIN, chrid(selector), 0, BuddyWnd.nquick - 1, false));
	    if(CLASS_FIELDCAIRN.equals(type))
		return(new Target(GroupLabels.Scope.KIN, chrid(selector), 0, BuddyWnd.ncolors - 1, true));
	}
	return(null);
    }
}
