package haven;

import me.ender.CFGColorBtn;
import me.ender.ui.CFGBox;
import me.ender.ui.CFGSlider;

public class ExploredAreaOptions extends WindowX {
    public static void open(GameUI gui) {
	if(gui.exploredAreaOptions != null) {
	    gui.exploredAreaOptions.show();
	    gui.exploredAreaOptions.raise();
	    gui.setfocus(gui.exploredAreaOptions);
	    return;
	}
	ExploredAreaOptions wnd = new ExploredAreaOptions();
	gui.exploredAreaOptions = wnd;
	wnd.reqclose(() -> {
	    wnd.reqdestroy();
	    if(gui.exploredAreaOptions == wnd)
		gui.exploredAreaOptions = null;
	});
	gui.add(wnd, Utils.getprefc("wndc-explored-area-options", UI.scale(260, 180)));
	gui.setfocus(wnd);
    }

    public ExploredAreaOptions() {
	super(Coord.z, "Explored Area Options");
	int y = UI.scale(8);
	add(new CFGColorBtn(CFG.MMAP_EXPLORED_COLOR, "Explored history color", false), UI.scale(10), y);
	y += UI.scale(30);
	Label exploredOpacity = add(new Label(""), UI.scale(10), y);
	y += UI.scale(18);
	add(new CFGSlider(UI.scale(220), 0, 100, CFG.MMAP_EXPLORED_OPACITY,
		exploredOpacity, "Explored history opacity: %d%%"), UI.scale(10), y);
	y += UI.scale(32);

	add(new CFGColorBtn(CFG.MMAP_EXPLORED_SESSION_COLOR, "Session color", false), UI.scale(10), y);
	y += UI.scale(30);
	Label sessionOpacity = add(new Label(""), UI.scale(10), y);
	y += UI.scale(18);
	add(new CFGSlider(UI.scale(220), 0, 100, CFG.MMAP_EXPLORED_SESSION_OPACITY,
		sessionOpacity, "Session opacity: %d%%"), UI.scale(10), y);
	y += UI.scale(38);

	add(new CFGBox("Expire green areas after they are last seen", CFG.MMAP_EXPLORED_SESSION_EXPIRE), UI.scale(10), y);
	y += UI.scale(28);
	Label expiry = add(new Label(""), UI.scale(10), y);
	y += UI.scale(18);
	add(new CFGSlider(UI.scale(220), 15, 30, CFG.MMAP_EXPLORED_SESSION_MINUTES,
		expiry, "Green area timeout: %d minutes"), UI.scale(10), y);
	y += UI.scale(30);
	resize(UI.scale(350), y);
    }

    public void destroy() {
	if((ui != null) && (ui.gui != null))
	    Utils.setprefc("wndc-explored-area-options", c);
	super.destroy();
    }
}
