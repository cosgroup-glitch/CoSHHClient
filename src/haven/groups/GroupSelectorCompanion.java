package haven.groups;

import haven.BuddyWnd;
import haven.Coord;
import haven.Dropbox;
import haven.GOut;
import haven.GameUI;
import haven.Text;
import haven.UI;
import haven.Widget;

import java.awt.Color;

public class GroupSelectorCompanion extends Widget {
    private static final int rowh = UI.scale(20);
    private static final int gap = UI.scale(2);
    private static final int editw = rowh;
    private static final int totalw = BuddyWnd.nquick * UI.scale(20);
    private static final int dropw = totalw - gap - editw;

    final BuddyWnd.GroupSelector real;
    final GroupLabels.Scope scope;
    final String owner;
    final int lo, hi;
    final boolean warnAboveQuick;
    private final GroupDropbox dropdown;
    private GroupLabelPopup labelPopup;

    GroupSelectorCompanion(BuddyWnd.GroupSelector real, GroupLabels.Scope scope, String owner,
			   int lo, int hi, boolean warnAboveQuick) {
	super(new Coord(dropw + gap + editw, rowh));
	this.real = real;
	this.scope = scope;
	this.owner = owner;
	this.lo = lo;
	this.hi = hi;
	this.warnAboveQuick = warnAboveQuick;
	dropdown = add(new GroupDropbox(displayGroup()), 0, 0);
	add(new EditButton(), dropw + gap, 0);
    }

    private int displayGroup() {
	return(Math.max(0, real.group));
    }

    public void tick(double dt) {
	super.tick(dt);
	int group = displayGroup();
	if((dropdown.sel == null) || (dropdown.sel != group))
	    dropdown.sel = group;
    }

    void setLabelPopup(GroupLabelPopup popup) {
	labelPopup = popup;
    }

    void clearLabelPopup(GroupLabelPopup popup) {
	if(labelPopup == popup)
	    labelPopup = null;
    }

    void closeLabelPopup() {
	GroupLabelPopup popup = labelPopup;
	labelPopup = null;
	if(popup != null)
	    popup.destroy();
    }

    public void destroy() {
	closeLabelPopup();
	super.destroy();
    }

    private void warnIfRisky(int group) {
	if(warnAboveQuick && (group >= BuddyWnd.nquick)) {
	    GameUI gui = getparent(GameUI.class);
	    if(gui != null)
		gui.error("Groups above " + (BuddyWnd.nquick - 1) + " may not render correctly on unmodified clients.");
	}
    }

    private String labelText(int group) {
	String label = GroupLabels.get(scope, owner, group);
	return(label.isEmpty() ? Integer.toString(group) : (group + " - " + label));
    }

    private class GroupDropbox extends Dropbox<Integer> {
	GroupDropbox(int group) {
	    super(dropw, 10, rowh);
	    sel = group;
	}

	protected Integer listitem(int i) {
	    return(lo + i);
	}

	protected int listitems() {
	    return(hi - lo + 1);
	}

	protected void drawitem(GOut g, Integer item, int i) {
	    int sw = itemh - UI.scale(4);
	    g.chcolor(BuddyWnd.gcolor(item));
	    g.frect(new Coord(UI.scale(2), (itemh - sw) / 2), new Coord(sw, sw));
	    g.chcolor(Color.WHITE);
	    g.text(labelText(item), new Coord(sw + UI.scale(6), (itemh - Text.std.height()) / 2));
	    g.chcolor();
	}

	public void change(Integer item) {
	    super.change(item);
	    if(item != null) {
		real.selectExtended(item);
		warnIfRisky(item);
	    }
	}
    }

    private class EditButton extends Widget {
	EditButton() {
	    super(new Coord(editw, rowh));
	}

	public void draw(GOut g) {
	    g.chcolor(new Color(60, 60, 60));
	    g.frect(Coord.z, sz);
	    g.chcolor(Color.WHITE);
	    g.atext("...", sz.div(2), 0.5, 0.5);
	    g.chcolor();
	}

	public boolean mousedown(MouseDownEvent ev) {
	    GroupLabelPopup.open(GroupSelectorCompanion.this, scope, owner, dropdown.sel);
	    return(true);
	}
    }
}
