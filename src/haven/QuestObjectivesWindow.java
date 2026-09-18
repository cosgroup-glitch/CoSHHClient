package haven;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static haven.CharWnd.attrf;
import static haven.CharWnd.iconfilter;
import static haven.CharWnd.ifnd;
import static haven.CharWnd.resdoc;
import static haven.PUtils.convolvedown;

public class QuestObjectivesWindow extends GameUI.Hidewnd {
    private static final Coord PAD = UI.scale(1, 1);
    private static final Coord FRAME_PULL = UI.scale(-2, -2);
    private static final Coord FRAME_OUTSET = UI.scale(3, 0);
    private static final Coord MIN = UI.scale(220, 130);
    private static final int BUTTON_GAP = UI.scale(6);
    private static final int BUTTON_ROW = UI.scale(42);
    private static final int CREDO_BUTTON_SIZE = UI.scale(34);
    private final Objectives body = add(new Objectives(), PAD);
    private final Frame bodyFrame = add(new Frame(Coord.z, false), PAD);
    private final QuestTabButton questButton = add(new QuestTabButton("quest", "Quest Log") {
	public void click() {
	    openQuestLog();
	}
    });
    private final QuestTabButton credoButton = add(new QuestTabButton("skill", "Credos") {
	public void click() {
	    openCredos();
	}
    });
    private final CurrentCredoButton currentCredoButton = add(new CurrentCredoButton());
    private GameUI gui;
    private int questid = -1;
    private QuestLogWindow questLog;
    private CredoLogWindow credoLog;

    public QuestObjectivesWindow(GameUI gui, Coord sz) {
	super(sz.max(MIN), "Quest Objectives");
	this.gui = gui;
	layout();
	hide();
    }

    protected Deco makedeco() {
	return(new DecoX(true).dragsize(true));
    }

    public void showQuest(QuestWnd.Quest.Box quest) {
	try {
	    questid = quest.questid();
	    body.update(questid, quest.title(), quest.conds());
	    resize(csz().max(fitSize()).max(MIN));
	    show();
	    raise();
	} catch(Loading ignored) {
	}
    }

    public void updateQuest(QuestWnd.Quest.Box quest) {
	if(quest.questid() == questid)
	    showQuest(quest);
    }

    public void clearQuest(int id) {
	if(id == questid) {
	    questid = -1;
	    body.clear();
	}
    }

    private Coord fitSize() {
	Coord bsz = buttonSize();
	Coord nsz = body.naturalSize().add(PAD.mul(2)).addy(BUTTON_ROW);
	return(Coord.of(Math.max(nsz.x, bsz.x), nsz.y));
    }

    private void layout() {
	Coord csz = csz();
	Coord fc = PAD.add(FRAME_PULL);
	Coord barea = Coord.of(Math.max(1, csz.x - fc.x - PAD.x + FRAME_OUTSET.x), Math.max(1, csz.y - fc.y - PAD.y - BUTTON_ROW + FRAME_OUTSET.y));
	bodyFrame.move(fc);
	bodyFrame.resize(barea);
	body.move(fc.add(bodyFrame.box.btloff()));
	body.resize(barea.sub(bodyFrame.box.bisz()).max(Coord.of(1, 1)));
	int by = Math.max(PAD.y, csz.y - UI.scale(2) - questButton.sz.y);
	int bx = Math.max(PAD.x, fc.x + bodyFrame.box.btloff().x + UI.scale(3));
	questButton.move(Coord.of(bx, by));
	credoButton.move(Coord.of(bx + questButton.sz.x + BUTTON_GAP, by));
	currentCredoButton.move(Coord.of(credoButton.c.x + credoButton.sz.x + BUTTON_GAP, by));
    }

    public void resize(Coord sz) {
	super.resize(sz.max(MIN));
	layout();
    }

    private Coord buttonSize() {
	int w = questButton.sz.x + BUTTON_GAP + credoButton.sz.x + BUTTON_GAP + currentCredoButton.sz.x;
	int h = Math.max(Math.max(questButton.sz.y, credoButton.sz.y), currentCredoButton.sz.y);
	return(Coord.of(w, h));
    }

    private void openQuestLog() {
	if((questLog == null) || (questLog.parent == null)) {
	    questLog = gui.add(new QuestLogWindow(gui), Utils.getprefc("wndc-kami-quest-log", UI.scale(new Coord(460, 260))));
	    questLog.hide();
	}
	questLog.show();
	questLog.raise();
    }

    private void openCredos() {
	if((credoLog == null) || (credoLog.parent == null)) {
	    credoLog = gui.add(new CredoLogWindow(gui), Utils.getprefc("wndc-kami-credo-log", UI.scale(new Coord(500, 300))));
	    credoLog.hide();
	}
	credoLog.show();
	credoLog.raise();
    }

    private SkillWnd.CredoGrid currentCredos() {
	return((gui != null) && (gui.chrwdg != null) && (gui.chrwdg.skill != null)) ? gui.chrwdg.skill.credos : null;
    }

    private void loadCurrentCredoQuest() {
	SkillWnd.CredoGrid grid = currentCredos();
	if((grid == null) || (grid.pcr == null) || (grid.pqid < 0) || (gui == null) || (gui.chrwdg == null))
	    return;
	gui.chrwdg.wdgmsg("qsel", grid.pqid);
    }

    private String currentCredoProgress(int questid) {
	SkillWnd.CredoGrid grid = currentCredos();
	if((grid == null) || (grid.pcr == null) || (grid.pqid != questid))
	    return null;
	return String.format("Lv. %d/%d    Qt. %d/%d", grid.pcl, grid.pclt, grid.pcql, grid.pcqlt);
    }

    public void destroy() {
	if(questLog != null)
	    Utils.setprefc("wndc-kami-quest-log", questLog.c);
	if(credoLog != null)
	    Utils.setprefc("wndc-kami-credo-log", credoLog.c);
	super.destroy();
    }

    private static abstract class QuestTabButton extends IButton {
	QuestTabButton(String name, String tip) {
	    super("gfx/hud/chr/" + name, "u", "d", null);
	    settip(tip);
	}

	protected void depress() {
	    ui.sfx(Button.clbtdown.stream());
	}

	protected void unpress() {
	    ui.sfx(Button.clbtup.stream());
	}
    }

    private class CurrentCredoButton extends Widget {
	private SkillWnd.Credo last;
	private Tex icon, upFrame, downFrame, hoverFrame;
	private boolean down = false, hover = false;
	private UI.Grab grab;

	CurrentCredoButton() {
	    super(Coord.of(CREDO_BUTTON_SIZE));
	    settip("Load current credo quest objective");
	}

	private SkillWnd.CredoGrid grid() {
	    return currentCredos();
	}

	private boolean active() {
	    SkillWnd.CredoGrid grid = grid();
	    return((grid != null) && (grid.pcr != null) && (grid.pqid >= 0));
	}

	private void update() {
	    SkillWnd.CredoGrid grid = grid();
	    SkillWnd.Credo cr = (grid == null) ? null : grid.pcr;
	    if(cr != last) {
		if(icon != null)
		    icon.dispose();
		icon = null;
		last = cr;
	    }
	    if((icon == null) && (cr != null)) {
		try {
		    icon = new TexI(cr.res.get().flayer(Resource.imgc).img);
		} catch(Loading ignored) {
		}
	    }
	}

	public void draw(GOut g) {
	    update();
	    Area cell = Area.sized(Coord.z, sz);
	    drawCell(g, cell);
	    Area clip = inner(cell);
	    if(icon != null) {
		Coord isz = icon.sz();
		int w = clip.sz().x;
		int h = Math.max(1, (isz.y * w) / Math.max(1, isz.x));
		Coord dsz = Coord.of(w, h);
		Coord ic = clip.ul.add(0, (clip.sz().y - h) / 2);
		g.chcolor(255, 255, 255, active() ? 255 : 120);
		g.image(icon, ic, clip.ul, clip.br, dsz);
		g.chcolor();
	    }
	}

	private Area inner(Area area) {
	    int b = UI.scale(4);
	    return(Area.corn(area.ul.add(b, b), area.br.sub(b, b)));
	}

	private void drawCell(GOut g, Area area) {
	    Area in = inner(area);
	    g.image(Window.bg, in.ul, in.sz());
	    g.chcolor(0, 0, 0, 35);
	    g.frect(in.ul, in.sz());
	    g.chcolor();
	    drawButtonBorder(g, area, buttonTex());
	}

	private Tex buttonTex() {
	    if(upFrame == null)
		upFrame = new TexI(questButton.up);
	    if(downFrame == null)
		downFrame = new TexI(questButton.down);
	    if(hoverFrame == null)
		hoverFrame = new TexI(questButton.hover);
	    if(down && hover)
		return(downFrame);
	    if(hover || (grab != null))
		return(hoverFrame);
	    return(upFrame);
	}

	private void drawButtonBorder(GOut g, Area area, Tex tex) {
	    int b = UI.scale(4);
	    Coord tsz = tex.sz();
	    Coord dsz = area.sz();
	    drawSlice(g, tex, area.ul, Coord.z, Coord.of(b, b), Coord.of(b, b));
	    drawSlice(g, tex, area.ul.add(b, 0), Coord.of(b, 0), Coord.of(tsz.x - b, b), Coord.of(Math.max(1, dsz.x - (b * 2)), b));
	    drawSlice(g, tex, area.ul.add(dsz.x - b, 0), Coord.of(tsz.x - b, 0), Coord.of(tsz.x, b), Coord.of(b, b));
	    drawSlice(g, tex, area.ul.add(0, b), Coord.of(0, b), Coord.of(b, tsz.y - b), Coord.of(b, Math.max(1, dsz.y - (b * 2))));
	    drawSlice(g, tex, area.ul.add(dsz.x - b, b), Coord.of(tsz.x - b, b), Coord.of(tsz.x, tsz.y - b), Coord.of(b, Math.max(1, dsz.y - (b * 2))));
	    drawSlice(g, tex, area.ul.add(0, dsz.y - b), Coord.of(0, tsz.y - b), Coord.of(b, tsz.y), Coord.of(b, b));
	    drawSlice(g, tex, area.ul.add(b, dsz.y - b), Coord.of(b, tsz.y - b), Coord.of(tsz.x - b, tsz.y), Coord.of(Math.max(1, dsz.x - (b * 2)), b));
	    drawSlice(g, tex, area.ul.add(dsz.x - b, dsz.y - b), Coord.of(tsz.x - b, tsz.y - b), tsz, Coord.of(b, b));
	}

	private void drawSlice(GOut g, Tex tex, Coord dc, Coord sul, Coord sbr, Coord dsz) {
	    g.image(new TexSI(tex, sul, sbr), dc, dsz);
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if(ev.b == 1) {
		down = true;
		hover = ev.c.isect(Coord.z, sz);
		grab = ui.grabmouse(this);
		ui.sfx(Button.clbtdown.stream());
		return true;
	    }
	    return false;
	}

	public boolean mouseup(MouseUpEvent ev) {
	    if((grab != null) && (ev.b == 1)) {
		grab.remove();
		grab = null;
		down = false;
		hover = ev.c.isect(Coord.z, sz);
		ui.sfx(Button.clbtup.stream());
		if(hover)
		    loadCurrentCredoQuest();
		return true;
	    }
	    return false;
	}

	public void mousemove(MouseMoveEvent ev) {
	    hover = ev.c.isect(Coord.z, sz);
	}

	public void destroy() {
	    if(icon != null)
		icon.dispose();
	    if(upFrame != null)
		upFrame.dispose();
	    if(downFrame != null)
		downFrame.dispose();
	    if(hoverFrame != null)
		hoverFrame.dispose();
	    super.destroy();
	}
    }

    private class Objectives extends Widget {
	private final Text.Foundry titlef = new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 16).aa(true);
	private final Text.Foundry condf = new Text.Foundry(Text.sans, 12).aa(true);
	private int questid = -1;
	private String title = "";
	private QuestWnd.Quest.Condition[] conditions = {};
	private Tex rtitle;
	private Tex[] rcond = {};
	private Tex rcredo;
	private String credoText;
	private int rwidth = -1;

	public Objectives() {
	    super(Coord.z);
	}

	public void clear() {
	    questid = -1;
	    title = "";
	    conditions = new QuestWnd.Quest.Condition[0];
	    disposeTex();
	    resize(Coord.z);
	}

	public void update(int questid, String title, QuestWnd.Quest.Condition[] conditions) {
	    this.questid = questid;
	    this.title = title;
	    this.conditions = Arrays.copyOf(conditions, conditions.length);
	    rwidth = -1;
	    render();
	}

	public Coord naturalSize() {
	    render();
	    int w = (rtitle == null) ? 0 : rtitle.sz().x, h = (rtitle == null) ? 0 : rtitle.sz().y + UI.scale(3);
	    for(Tex tex : rcond) {
		w = Math.max(w, tex.sz().x);
		h += tex.sz().y;
	    }
	    if(rcredo != null) {
		w = Math.max(w, rcredo.sz().x);
		h += UI.scale(4) + rcredo.sz().y;
	    }
	    return(Coord.of(w + UI.scale(2), h));
	}

	private void disposeTex() {
	    if(rtitle != null) {
		rtitle.dispose();
		rtitle = null;
	    }
	    for(Tex tex : rcond) {
		if(tex != null)
		    tex.dispose();
	    }
	    rcond = new Tex[0];
	    if(rcredo != null) {
		rcredo.dispose();
		rcredo = null;
	    }
	}

	private void render() {
	    int width = Math.max(UI.scale(60), sz.x - UI.scale(2));
	    String nextCredoText = currentCredoProgress(questid);
	    if((rtitle != null) && (rwidth == width) && Utils.eq(credoText, nextCredoText))
		return;
	    disposeTex();
	    rwidth = width;
	    credoText = nextCredoText;
	    rtitle = titlef.renderwrap(title, width).tex();
	    rcond = new Tex[conditions.length];
	    for(int i = 0; i < conditions.length; i++) {
		QuestWnd.Quest.Condition cond = conditions[i];
		Color color = QuestWnd.Quest.stcol[Math.min(cond.done, QuestWnd.Quest.stcol.length - 1)];
		String text = String.format("%c %s%s",
		    QuestWnd.Quest.stsym[Math.min(cond.done, QuestWnd.Quest.stsym.length - 1)],
		    cond.desc,
		    (cond.status == null) ? "" : (" " + cond.status));
		rcond[i] = condf.renderwrap(text, color, width).tex();
	    }
	    if(credoText != null)
		rcredo = Text.renderstroked(credoText, Color.WHITE, Color.BLACK, QuestWnd.Quest.QView.qcfnd).tex();
	}

	public void resize(Coord sz) {
	    super.resize(sz);
	    if(rwidth != Math.max(UI.scale(60), sz.x - UI.scale(2)))
		render();
	}

	public boolean mousedown(MouseDownEvent ev) {
	    if((rtitle != null) && ev.c.isect(Coord.z, rtitle.sz())) {
		QuestWnd qw = questWnd();
		if(qw != null) {
		    qw.wdgmsg("qsel", questid);
		    openQuestLog();
		    return true;
		}
	    }
	    return super.mousedown(ev);
	}

	public void draw(GOut g) {
	    render();
	    int y = 0;
	    if(rtitle != null) {
		g.image(rtitle, Coord.of(UI.scale(1), y));
		y += rtitle.sz().y + UI.scale(3);
	    }
	    for(Tex tex : rcond) {
		g.image(tex, Coord.of(UI.scale(1), y));
		y += tex.sz().y;
	    }
	    if(rcredo != null) {
		y += UI.scale(4);
		g.image(rcredo, Coord.of(UI.scale(1), y));
	    }
	}

	public void destroy() {
	    disposeTex();
	    super.destroy();
	}
    }

    private QuestWnd questWnd() {
	return((gui != null) && (gui.chrwdg != null)) ? gui.chrwdg.quest : null;
    }

    private static class QuestLogWindow extends GameUI.Hidewnd {
	private static final Coord MIN = UI.scale(420, 240);
	private static final Coord PAD = UI.scale(10, 10);
	private static final int GAP = UI.scale(8);
	private static final int TAB_ROW = UI.scale(36);
	private final GameUI gui;
	private final Tabs tabs;
	private final Tabs.Tab currentTab;
	private final Tabs.Tab completedTab;
	private final QuestListBox current;
	private final QuestListBox completed;
	private final QuestDetail detail;
	private final Frame detailFrame;
	private final Frame listFrame;
	private final Button currentButton;
	private final Button completedButton;

	QuestLogWindow(GameUI gui) {
	    super(UI.scale(new Coord(620, 360)), "Quest Log");
	    this.gui = gui;
	    detail = add(new QuestDetail(), PAD);
	    tabs = new Tabs(Coord.z, Coord.z, this);
	    currentTab = tabs.add();
	    current = currentTab.add(new QuestListBox(this, true), Coord.z);
	    completedTab = tabs.add();
	    completed = completedTab.add(new QuestListBox(this, false), Coord.z);
	    detailFrame = add(new Frame(Coord.z, false), PAD);
	    listFrame = add(new Frame(Coord.z, false), PAD);
	    currentButton = add(tabs.new TabButton(UI.scale(115), "Current", currentTab));
	    completedButton = add(tabs.new TabButton(UI.scale(115), "Completed", completedTab));
	    layout();
	}

	protected Deco makedeco() {
	    return(new DecoX(true).dragsize(true));
	}

	private QuestWnd questWnd() {
	    return((gui != null) && (gui.chrwdg != null)) ? gui.chrwdg.quest : null;
	}

	private QuestWnd.Quest.Box selectedBox() {
	    QuestWnd qw = questWnd();
	    return((qw != null) && (qw.quest instanceof QuestWnd.Quest.Box)) ? (QuestWnd.Quest.Box)qw.quest : null;
	}

	void select(QuestWnd.Quest q) {
	    QuestWnd qw = questWnd();
	    if(qw != null)
		qw.wdgmsg("qsel", q.id);
	}

	public void resize(Coord sz) {
	    super.resize(sz.max(MIN));
	    layout();
	}

	private void layout() {
	    Coord csz = csz();
	    int listw = Math.max(UI.scale(250), (csz.x - (PAD.x * 2) - GAP) / 3);
	    int h = Math.max(1, csz.y - (PAD.y * 2));
	    int listh = Math.max(1, h - TAB_ROW);
	    Coord detailsz = Coord.of(Math.max(1, csz.x - (PAD.x * 2) - GAP - listw), h);
	    detailFrame.move(PAD);
	    detailFrame.resize(detailsz);
	    detail.move(PAD.add(detailFrame.box.btloff()));
	    detail.resize(detailsz.sub(detailFrame.box.bisz()).max(Coord.of(1, 1)));
	    Coord listc = PAD.addx(detailsz.x + GAP);
	    Coord listsz = Coord.of(listw, listh);
	    Coord listin = listsz.sub(listFrame.box.bisz()).max(Coord.of(1, 1));
	    listFrame.move(listc);
	    listFrame.resize(listsz);
	    currentTab.move(listc.add(listFrame.box.btloff()));
	    completedTab.move(listc.add(listFrame.box.btloff()));
	    tabs.resize(listin);
	    current.resize(listin);
	    completed.resize(listin);
	    int bw = currentButton.sz.x + UI.scale(5) + completedButton.sz.x;
	    int bx = listc.x + Math.max(0, (listw - bw) / 2);
	    int by = listc.y + listh + UI.scale(5);
	    currentButton.move(Coord.of(bx, by));
	    completedButton.move(Coord.of(bx + currentButton.sz.x + UI.scale(5), by));
	}

	public void tick(double dt) {
	    super.tick(dt);
	    current.refresh();
	    completed.refresh();
	    detail.update(selectedBox());
	}

	private static class QuestListBox extends SListBox<QuestWnd.Quest, Widget> {
	    private final QuestLogWindow owner;
	    private final boolean pending;
	    private List<QuestWnd.Quest> items = Collections.emptyList();

	    QuestListBox(QuestLogWindow owner, boolean pending) {
		super(Coord.z, attrf.height() + UI.scale(2));
		this.owner = owner;
		this.pending = pending;
	    }

	    protected List<QuestWnd.Quest> items() {
		return(items);
	    }

	    void refresh() {
		QuestWnd qw = owner.questWnd();
		if(qw == null) {
		    items = Collections.emptyList();
		    return;
		}
		List<QuestWnd.Quest> src = pending ? qw.cqst.quests : qw.dqst.quests;
		items = new ArrayList<>(src);
		Collections.sort(items, Comparator.comparingInt((QuestWnd.Quest q) -> q.mtime).reversed());
	    }

	    protected Widget makeitem(QuestWnd.Quest q, int idx, Coord sz) {
		return(new Item(sz, q));
	    }

	    protected void drawslot(GOut g, QuestWnd.Quest q, int idx, Area area) {
		super.drawslot(g, q, idx, area);
		QuestWnd.Quest.Box box = owner.selectedBox();
		if((box != null) && (box.questid() == q.id))
		    drawsel(g, q, idx, area);
	    }

	    private class Item extends Widget {
		private final QuestWnd.Quest q;
		private final IconText nm;
		private Object dres, dtit;

		Item(Coord sz, QuestWnd.Quest q) {
		    super(sz);
		    this.q = q;
		    this.nm = new IconText(sz) {
			protected BufferedImage img() {return(q.res.get().flayer(Resource.imgc).img);}
			protected String text() {return(q.title());}

			protected void drawtext(GOut g) {
			    if(q.done == QuestWnd.Quest.QST_DISABLED)
				g.chcolor(255, 128, 0, 255);
			    super.drawtext(g);
			    g.chcolor();
			}
		    };
		    add(this.nm, Coord.z);
		}

		public void draw(GOut g) {
		    if((q.res != dres) || (q.title != dtit)) {
			nm.invalidate();
			dres = q.res;
			dtit = q.title;
		    }
		    super.draw(g);
		}

		public boolean mousedown(MouseDownEvent ev) {
		    if(ev.b == 1) {
			owner.select(q);
			return(true);
		    }
		    return(super.mousedown(ev));
		}

	    }
	}

	private static class QuestDetail extends Widget {
	    private final RichTextBox text = add(new RichTextBox(Coord.z, ifnd, null), Coord.z);
	    private final List<Button> options = new ArrayList<>();
	    private int id = -1;
	    private String sig = "";
	    private QuestWnd.Quest.Box current;
	    private int optw = -1;

	    QuestDetail() {
		super(Coord.z);
		text.bg = new Color(0, 0, 0, 128);
	    }

	    void update(QuestWnd.Quest.Box box) {
	    int nid = (box == null) ? -1 : box.questid();
	    String nsig = questSignature(box) + optionSignature(box);
	    if((nid == id) && Utils.eq(sig, nsig))
		return;
	    id = nid;
	    sig = nsig;
	    current = box;
	    if(box == null) {
		text.set((RichText.Document)null);
	    } else {
		text.set(() -> resdoc(box.res.get(), questText(box)));
	    }
	    rebuildOptions();
	    layout();
	    }

	    private String questText(QuestWnd.Quest.Box box) {
		StringBuilder buf = new StringBuilder(box.rendertext());
		QuestWnd.Quest.Condition[] conds = box.conds();
		if(conds.length > 0)
		    buf.append("\n\n");
		for(QuestWnd.Quest.Condition cond : conds) {
		    int st = Math.min(cond.done, QuestWnd.Quest.stcol.length - 1);
		    buf.append(RichText.Parser.col2a(QuestWnd.Quest.stcol[st]));
		    buf.append('{').append(QuestWnd.Quest.stsym[st]).append(' ').append(cond.desc);
		    if(cond.status != null)
			buf.append(' ').append(cond.status);
		    buf.append("}\n");
		}
		return(buf.toString());
	    }

	    private String questSignature(QuestWnd.Quest.Box box) {
		if(box == null)
		    return("");
		StringBuilder buf = new StringBuilder();
		for(QuestWnd.Quest.Condition cond : box.conds()) {
		    buf.append(cond.done).append('\n').append(cond.desc).append('\n').append(cond.status).append('\n');
		}
		return(buf.toString());
	    }

	    private String optionSignature(QuestWnd.Quest.Box box) {
		if(!(box instanceof QuestWnd.Quest.DefaultBox))
		    return("");
		StringBuilder buf = new StringBuilder();
		for(Pair<String, String> opt : ((QuestWnd.Quest.DefaultBox)box).options)
		    buf.append(opt.a).append('\n').append(opt.b).append('\n');
		return(buf.toString());
	    }

	    private void clearOptions() {
		for(Button btn : options)
		    btn.destroy();
		options.clear();
	    }

	    private void rebuildOptions() {
		clearOptions();
		optw = Math.max(1, sz.x - UI.scale(20));
		if(!(current instanceof QuestWnd.Quest.DefaultBox))
		    return;
		for(Pair<String, String> opt : ((QuestWnd.Quest.DefaultBox)current).options) {
		    options.add(add(new Button(optw, opt.b, false) {
			    public void click() {
				current.wdgmsg("opt", opt.a);
			    }
			}, Coord.z));
		}
	    }

	    private void layout() {
		if((current != null) && (optw != Math.max(1, sz.x - UI.scale(20))))
		    rebuildOptions();
		int gap = UI.scale(5);
		int h = options.isEmpty() ? 0 : UI.scale(10);
		for(Button btn : options)
		    h += btn.sz.y + gap;
		text.resize(Coord.of(sz.x, Math.max(1, sz.y - h)));
		int y = text.sz.y + UI.scale(5);
		for(Button btn : options) {
		    btn.move(Coord.of(UI.scale(10), y));
		    y += btn.sz.y + gap;
		}
	    }

	    public void resize(Coord sz) {
		super.resize(sz);
		layout();
	    }

	    public void destroy() {
		clearOptions();
		super.destroy();
	    }
	}
    }

    private static class CredoLogWindow extends GameUI.Hidewnd {
	private static final Coord MIN = UI.scale(420, 260);
	private static final Coord PAD = UI.scale(10, 10);
	private static final int GAP = UI.scale(8);
	private static final int ACTIONH = UI.scale(34);
	private final GameUI gui;
	private final CredoGridView list;
	private final CredoInfo info;
	private final Frame listFrame;
	private final Frame infoFrame;
	private final Button pursue;
	private final Label cost;
	private CredoEntry selected;

	CredoLogWindow(GameUI gui) {
	    super(UI.scale(new Coord(620, 380)), "Credos");
	    this.gui = gui;
	    list = add(new CredoGridView(this), PAD);
	    info = add(new CredoInfo(), PAD);
	    listFrame = add(new Frame(Coord.z, false), PAD);
	    infoFrame = add(new Frame(Coord.z, false), PAD);
	    pursue = add(new Button(UI.scale(100), "Pursue", false) {
		    public void click() {
			pursueSelected();
		    }
		});
	    cost = add(new Label(""));
	    layout();
	}

	protected Deco makedeco() {
	    return(new DecoX(true).dragsize(true));
	}

	private SkillWnd.CredoGrid credos() {
	    return((gui != null) && (gui.chrwdg != null) && (gui.chrwdg.skill != null)) ? gui.chrwdg.skill.credos : null;
	}

	private SkillWnd skills() {
	    return((gui != null) && (gui.chrwdg != null)) ? gui.chrwdg.skill : null;
	}

	void select(CredoEntry entry) {
	    selected = entry;
	    SkillWnd.CredoGrid grid = credos();
	    if(entry == null) {
		info.clear();
	    } else if((entry.live != null) && (grid != null)) {
		info.setLive(() -> entry.live.rendertext(grid));
	    } else {
		info.setCatalog(entry);
	    }
	    updateActions();
	}

	public void resize(Coord sz) {
	    super.resize(sz.max(MIN));
	    layout();
	}

	private void layout() {
	    Coord csz = csz();
	    int lw = Math.max(UI.scale(230), (csz.x - (PAD.x * 2) - GAP) / 2);
	    int h = Math.max(1, csz.y - (PAD.y * 2) - ACTIONH);
	    Coord listsz = Coord.of(lw, h);
	    listFrame.move(PAD);
	    listFrame.resize(listsz);
	    list.move(PAD.add(listFrame.box.btloff()));
	    list.resize(listsz.sub(listFrame.box.bisz()).max(Coord.of(1, 1)));
	    Coord infoc = PAD.addx(lw + GAP);
	    Coord infosz = Coord.of(Math.max(1, csz.x - infoc.x - PAD.x), h);
	    infoFrame.move(infoc);
	    infoFrame.resize(infosz);
	    info.move(infoc.add(infoFrame.box.btloff()));
	    info.resize(infosz.sub(infoFrame.box.bisz()).max(Coord.of(1, 1)));
	    int ay = PAD.y + h + UI.scale(6);
	    pursue.move(Coord.of(PAD.x, ay));
	    cost.move(Coord.of(PAD.x + pursue.sz.x + UI.scale(10), ay + ((pursue.sz.y - cost.sz.y) / 2)));
	    updateActions();
	}

	public void tick(double dt) {
	    super.tick(dt);
	    list.refresh();
	    updateActions();
	}

	private void updateActions() {
	    SkillWnd.CredoGrid grid = credos();
	    boolean canStart = (grid != null) && (grid.pcr == null) && !grid.ncr.isEmpty();
	    pursue.visible = canStart;
	    if(grid == null) {
		cost.settext("");
	    } else if(grid.pcr != null) {
		cost.settext(String.format("Pursuing: %s  Level %d/%d  Quest %d/%d",
			displayName(grid.pcr), grid.pcl, grid.pclt, grid.pcql, grid.pcqlt));
	    } else if(grid.cost > 0) {
		cost.settext(String.format("Cost: %,d LP", grid.cost));
	    } else {
		cost.settext("");
	    }
	    cost.visible = !cost.texts.equals("");
	    cost.move(Coord.of(PAD.x + pursue.sz.x + UI.scale(10), pursue.c.y + ((pursue.sz.y - cost.sz.y) / 2)));
	}

	private void pursueSelected() {
	    SkillWnd.CredoGrid grid = credos();
	    SkillWnd skill = skills();
	    if((grid == null) || (skill == null) || (selected == null) || (selected.live == null) || (grid.pcr != null))
		return;
	    if(!grid.ncr.contains(selected.live))
		return;
	    skill.pursueCredo(selected.live);
	}

	private static class CredoEntry {
	    final String name;
	    final String[] requires;
	    final String description;
	    final String[] bonuses;
	    final String image;
	    final SkillWnd.Credo live;
	    final boolean unavailable;

	    CredoEntry(SkillWnd.Credo live) {
		this.name = displayName(live);
		CatalogCredo catalog = catalog(name);
		this.requires = (catalog == null) ? new String[0] : catalog.requires;
		this.description = (catalog == null) ? "" : catalog.description;
		this.bonuses = (catalog == null) ? new String[0] : catalog.bonuses;
		this.image = (catalog == null) ? null : catalog.image;
		this.live = live;
		this.unavailable = false;
	    }

	    CredoEntry(CatalogCredo catalog) {
		this.name = catalog.name;
		this.requires = catalog.requires;
		this.description = catalog.description;
		this.bonuses = catalog.bonuses;
		this.image = catalog.image;
		this.live = null;
		this.unavailable = true;
	    }

	    RichText.Document catalogText() {
		StringBuilder buf = new StringBuilder();
		buf.append("$b{$font[serif,16]{").append(name).append("}}\n\n");
		buf.append("$col[180,180,180]{Unavailable}\n\n");
		if(!description.equals(""))
		    buf.append(description).append("\n\n");
		if(requires.length == 0) {
		    buf.append("Requirements: None known.\n\n");
		} else {
		    buf.append("Requires:\n");
		    for(String req : requires)
			buf.append("  \u2022 ").append(req).append('\n');
		    buf.append('\n');
		}
		if(bonuses.length > 0) {
		    buf.append("$col[255,255,64]{Bonuses:}\n");
		    for(String bonus : bonuses)
			buf.append("$col[255,255,64]{  \u2022 ").append(bonus).append("}\n");
		}
		return(new RichText.Document(buf.toString()));
	    }

	    Tex catalogImage() {
		if(image == null)
		    return(null);
		try(InputStream in = QuestObjectivesWindow.class.getResourceAsStream("/haven/credo/" + image)) {
		    if(in == null)
			return(null);
		    return(new TexI(javax.imageio.ImageIO.read(in)));
		} catch(Exception e) {
		    return(null);
		}
	    }
	}

	private static class CatalogCredo {
	    final String name;
	    final String[] requires;
	    final String description;
	    final String[] bonuses;
	    final String image;

	    CatalogCredo(String name, String image, String description, String[] bonuses, String... requires) {
		this.name = name;
		this.image = image;
		this.description = description;
		this.bonuses = bonuses;
		this.requires = requires;
	    }
	}

	private static final CatalogCredo[] CREDO_CATALOG = {
            new CatalogCredo("Forager", "forager.png",
                "Beneath far-stretched canopies all around you sing the abundance of the Hearth wilding, and, as if in a dream, you have sensed its vastness in life and form, yearning now also to learn its names, and ways. It is said that the Old Hearthlings drank the light of the sun, and that it was in reflection thereof that the gods made root and leaf to cover that first antiquity which saw them set loose. There is much wisdom in the sagas, and a scent on the wind invites you to remember more of the things which grow and crawl. Wandering the Forager's Path promises...",
                new String[] {"Exploration +10", "Perception +10", "Speed bonus when chasing small animals", "Increased baseline quality of herbs picked. (+20%)", "A chance to double herbs picked."}),
            new CatalogCredo("Fisherman", "fisherman.png",
                "Far beneath the waves gleams a treasure of wisdom, and on the breath of a fish you have heard told the riddles of life and death. Since first your feet touched water, a siren's song has called you deeper, and deeper, and farther down, down into the yonders blue, to see the spired groves of kelp, where Roach and Pike play hide and seek, and the crashing waters where Salmon dance. A ripple on calm waters calls you to learn the names of the things which swim and dive. Towing the Fisherman's Line promises...",
                new String[] {"Reduced risk for loss of fishing equipment.", "Better trash recovery when fishing.", "Increased chance of a rare catch.", "Reduced cost of \"Fisher's Request for a Catch\".", "Increased baseline quality of fish caught. (+10%)"}),
            new CatalogCredo("Hunter", "hunter.png",
                "In depths of woodland realms unchartered, the tracks too easily criss-cross back and over themselves, losing all meaning under shades of bush and fern, and only those who studiously read those glyphs of hoof and paw, may then follow them down wilder, freer, paths. For some time you have yearned to speak this strange and peculiar tongue of beast, blood and soil, and a beying echo from far away in the greenery, promises to teach you. Tracking the Hunter's Trail promises...",
                new String[] {"Marksmanship +20", "Fleeing animals take more damage.", "Reduced cost of 'Quell the Beast'.", "Gain more meat from butchering. (does not apply to tamed animals)", "Ranged weapons deal slightly more damage."}),
            new CatalogCredo("Farmer", "farmer.png",
                "In golden rows, ever chasing a blue horizon, lie planted the myths of a people tied intimately to the soil they till, and to the grain they mill. Of such things you desire to learn more, for you are becoming a Farmer, and within you now shoots leaves of wisdom, of the seasons, and of the plants, and of all the things which grow. In the creaking of sprouting roots you have heard stories of how the world seed was once planted by the old gods, long before a moon was yet hung over the Hearth to light it. In the pod of a pea, you have seen strange and ancient runes, telling of the world's coming end and ruin. A wind that shakes the barley calls you onward to gather further insight. Plowing the Farmer's Furrow promises...",
                new String[] {"Farming +15", "Quality bonus when milking. (+20%)", "Faster crop growth. (+10%)", "Increased crop yield. (+20%)", "Farming +50"}, "Forager"),
            new CatalogCredo("Lumberjack", "lumberjack.png",
                "For some time you have wandered among the great spires of woodlands free and wild, with a splinter in your soul tracing the contour of your ever increasing desire to prune and guide them to more proper forms. In the splitting of logs you have heard the cries of new shapes dreaming to be hewn free, and in the gleam of axe and fire you have seen a wholesome light with which to burn and chase away the shadows of an untended wilderness. You have heard told that, when the World-Tree first sprouted, the Old Gods placed the first Lumberjack on guard over it for all nights and days, until the end of time, when he alone will know the proper time to fell the great tree, and sunder the Hearth. To ply the Lumberjack's trade is to learn the shapes and courses of the various woods and trees, their marblings and colorations, kinds and seasons, and how to best put those to use. Cutting the Lumberjack's Timberline promises...",
                new String[] {"Lore +15", "Increased cutting speed of trees and logs.", "Carpentry +20.", "Increased board and block yield from logs. (+30%)", "Strength +25"}, "Forager", "Hunter"),
            new CatalogCredo("Mystic", "mystic.png",
                "You have stared for long longingly into depths of ocean and forest, and much contemplation of the things high and low have led you down darker, more brooding paths, and your thoughts have for some time carried an otherworldly air. You walk now always in a shadowland, somewhere in between dream and awakening, where the currents of Hearth magick run close to the ground, and with your third eye always open to the vast, universal distances around you. The Mystic seeks wisdom and understanding, and in thought and deed affinity, and communion, with the woodland spirits, and with all the presences and potentialities of nature. The sagas speak of three Norns, old and wise, who weave the fate silk of all hearthlings, and who know where each and every thread runs out. Dreaming the Mystic's Dream promises...",
                new String[] {"Will +15", "Lore +20", "+5% study speed.", "+10% experience gain.", "Hearth Magic cost reduced by 10%"}, "Forager", "Fisherman"),
            new CatalogCredo("Quarryman", "quarryman.png",
                "You have sat with the mountain until only the mountain remained, and for some time fissure has grown down the straight and narrow of your worldview, in challenge, threatening to rend it in twain with the terrible blow of a new experience suffered. Your dreams have grown rigid, and angular, and you see the world around you as hewn in stone, and seek now to remake it again in that image. The Quarryman knows the taste and turns of the mineral world, and the names of all the stones and rocks which weave it. From the depths of the mountain an ominous note calls you to think and act like quartz, like granite, and like the blackest, most cunning, obsidian. Some say that the roots of the mountain are older than even the Old Gods themselves. Cracking the Quarryman's Crevice promises...",
                new String[] {"Masonry +20", "Reduced cost of 'Mine Song'", "Increased stone chipping speed and mining strength.", "Strength +20", "Chance to dig out Quarryartz Stone"}, "Forager"),
            new CatalogCredo("Tailor", "tailor.png",
                "The thin of your thread has passed through the eye of a needle, and remade you entirely in new whole cloth. For some time your thoughts have wandered to softer places, and stranger fabrics, and increasingly you have felt the call of needlework, and strange artifice. The Tailor recalls all the fibers and threads of the Hearth, and weaves them continously into the tapestry of his life, in service to both himself, others, and ever the quest for the perfect garment, for the perfect occasion. For Peasant and King alike, only the Tailor stands between him, the cold of the seasons, and the harshness of unforgiving elements, and such responsibilities call forth only the best in those whose task it is to wear that spiritual finery. Treading the Tailor's Thread promises...",
                new String[] {"Sewing +15", "Chance to fail gildings reduced by 5%.", "Double the chance to reslot gildings when recycling artifacts.", "Sewing +25", "The first gilding of a new artifact is always successful."}, "Farmer"),
            new CatalogCredo("Nomad", "nomad.png",
                "You have grown weary of familiar surroundings, or perhaps your feet never stopped to rest? Either way you find yourself now a wanderer in tought and the vastness of open space, ever seeking out new and farther places, and always striving toward the next horizon. Seven times you have walked the Hearth over wide, and three over tall, and many are the strange and wondrous sights you have seen in it, yet there remains always an immensity of the unseen, beckoning you onward to take that next step. The Nomad calls no place home, yet is also always at home in his solitudes of nature and travel, accompanied perhaps only by wild horses and beasts of burden. It is said that when the Old Gods were young, they too walked distances beyond reckoning. Embarking on the Nomad's Journey promises...",
                new String[] {"Exploration +15", "Survival +25", "One extra column of inventory space.", "Pony Power of Horses lasts longer.", "One extra row of inventory space."}, "Fisherman", "Forager", "Hunter"),
            new CatalogCredo("Miner", "miner.png",
                "Your soul has moved forever deep into the eternal night -- lit only by flashes of roaring, red fire -- of the mine, and through your veins courses a blood stained by heavy metal. Day and night have begun losing their meanings to you, and your senses have grown acutely tuned to a life subterranean. You live every moment to go deeper and further down, down toward deep and deeper veins, where the roots of the mountain claw their way into the Underworld, and where the caverns echo with trollish laughter. With every snapping of the tendons, and every strain of muscles aching, you drive your pick through the eternal cavern, ripping the treasures of the deep to that surface far above which you so seldom see, and hoping to strike for the Mother Lode. The mountain whispers to you of a time before the Old Gods. Descending the Miner's Shaft promises...",
                new String[] {"Strength +15 & Masonry +15", "Significant chance to localize caveins.", "Ore mined smelts faster. (+25%)", "Chance to pulverize tiles when mining.", "Ability to sense ore ahead when mining."}, "Quarryman"),
            new CatalogCredo("Strider", "strider.png",
                "You feel the vibrance of the seasons, and under the soles of your feet you can sense the tremor of a single leaf falling, or of your quarry, quaking in the undergrowth at your passing. You have long wandered the Woodland Hearth, and your trade has grown to become that of a frontiersman in it, making a living in forestry, fishing, and hunting, and thus you have become a part of the wilds -- and they of you -- to the point that you now move nigh effortlessly in them, over root and crag, flowing almost like a river. They say that the first Strider was the Moon itself, racing the Sun across the firmament. Taking the Strider's Stride promises...",
                new String[] {"+50% Swimming.", "Chance of getting additional hides when flaying animals.", "Reduced satiation from Game (2% instead 5%).", "Not interrupted by combat when raiding insect nests.", "+15% damage with ranged weapons."}, "Lumberjack", "Fisherman"),
            new CatalogCredo("Cave Hermit", "cave-hermit.png",
                "The surface of the Hearth has grown but a distant memory, and the waves of history may wash whichever ways they please, far above in that raging, tumultous world of light and day. Your world has become another, calmer, darker, deeper, and more ancient one, of peace and meditation, far below in the cool and pitch of the deepest caverns, where Hearthling sight fades, and other senses grow instead to terrible powers. You spend your nights eternal far down, down near the roots of the Mountain, in the musk of cavebulbs, with a touch of slime, and the haunting symphonies of a thousand bats screeching in awful symphony. Your ways are those of stone, and of thought, and of communion, ultimately, with the dark of the cavernous Underworld. Staring into the Cave Hermit's Abyss promises...",
                new String[] {"Significantly increased risk of mining out trolls.", "Ability to eat Stalagooms & Cave Slime.", "+1.5% study speed per cave level you descend.", "Deal 5% extra damage per cave level you descend.", "Trolls no longer attack you."}, "Miner", "Mystic"),
            new CatalogCredo("Gardener", "gardener.png",
                "Leaves tremble at your touch, as if by the first beams of radiant sunlight of spring, reflecting some of their own green back again unto you, and unto your fingers. Wherever you wander, seeds appear to sprout from under your feet, and the touch of your naked soles restores life to dying grass. The secrets of all the things which grow are known to you, and you trace the threads of life and death in the contours and growth lines of grass and leaves. Your dream, as a Gardener, is to prune and guide the exuberant abundance of wild life into the pleasant and considered archetypes of a well kept garden. The myths speak of how all the Hearth once poured forth from a single seed. Climbing the Gardener's Stalk promises...",
                new String[] {"Can plant Blood Sterns, Farming +5", "Can plant Cavebulbs, Farming +5", "Halved soil and water required for gardening pots.", "Can plant Chiming Bluebells, Farming +10", "Doubled yield from gardening pots."}, "Farmer"),
            new CatalogCredo("Blacksmith", "blacksmith.png",
                "Metal chimes in pure notes at the thunder of your hammer, and your muscles have grown to terrible dimensions enough to bend steel. Your life is lived and regulated by the steady, pulsating rhythm of pounding hammers, and heaving bellows, drawing the heavy sighs of hot, soot-filled, air, rushing again away to feed ever hungry, ever burning, furnaces, mirrored in reflective pools of molten metal. You know the stuff which makes up all the Hearth -- earth and wood -- and how to slake their thirsts for metallic forms in the glowing fury of white-hot flames. The Firmament of the Hearth was pounded thin as a flake, and hung high over the Hearth, by the Creator, from a single silver nugget. Pounding the Blacksmith's Anvil promises...",
                new String[] {"Strength +15 & Smithing +15", "More irrlights when smithing.", "Reduced smithing time.", "Smelters built have larger inventories. (6x6)", "Small chance to double output when smithing."}, "Miner", "Lumberjack"),
            new CatalogCredo("Potter", "potter.png",
                "The dead matter of clay springs to life in your hands like in no others, coaxing secret forms to dance into existence from the perpetual void of the primordial before. In the roaring fire of kilns, and in the slow, revolving sound of your wheel, you have heard the calling of such forms, yearning to be made in full, and yours are the hands that shape them, and move the bellows that heave with life and fire. In the crack of a single shard, you can recall the errors of all hearthkin. The world ocean rests in an earthenware dish, fired in the sun by the Old Gods on the first day. Spinning the Potter's Wheel promises...",
                new String[] {"Masonry +15", "Small quality bonus when digging ball clay. (+10%)", "Large quality bonus when digging acre clay. (+25%)", "Chance to harvest more cave, gray, and pit clays.", "Ability to make \"Potter's Clay\"."}, "Farmer"),
            new CatalogCredo("Gem Hunter", "gem-hunter.png",
                "From hidden rocks and deep streams rings out a call of pure notes, calling you to their sources to hew them free, the endless facets of a thousand gems gleaming in the endless dark. Your heart has for some time burned with secret passion for the nobler salts of the earth, and your hunt leaves no stone unturned, as every strike of your pick strikes sparks to reflect and play in the lustre and shine of pure jewels, and light up the darkness of the mine with splendid color. All the Hearth was once a perfect diamond, ground to rocks and dusts by Time itself. Tracing the Gem Hunter's Seam promises...",
                new String[] {"Masonry +15", "Higher chance of mining out gems.", "Higher chance of mining out larger gems.", "Can mine out pear cut gems.", "Small chance of doubling a gem mined out."}, "Quarryman"),
            new CatalogCredo("Pearl Diver", "pearl-diver.png",
                "There is a certain quiet, and profound stillness, that one can sense only in a mine, or near the bottom of a cool lake. You have plunged farther on down than most, driven always by the call of those rare, precious, lights, which illuminate the everdarks of the Hearth, but lately visions of especially rare baubles have begun ocurring to you -- gnawing at you, like grains of sand lodged in the iridescence of your mind -- and you have begun testing the waters to find the secret places of which the mussels whisper. The night sky was cut, by the Gods, from nacre grown in the river of time. Drawing the Pearl Diver's Breath promises...",
                new String[] {"Constitution +10", "Halved asphyxiation damage when drowning.", "Significant quality bonus when picking mussels and oysters. (+50%, +80% in combination with the Forage bonus)", "Reduce satiations from Seafood.", "Significantly increased chances of finding pearls. (Effect applied upon picking mussels/oysters, rough 0.5-0.75% fixed chance in addition to regular pool of foraging)"}, "Fisherman", "Gem Hunter"),
            new CatalogCredo("Cook", "cook.png",
                "They say a watched pot never boils, and without you ever really noticing it, up until now, the pot of your mind unwatched has slowly stirred up into a hearthy broth of new ideas and impulses, threatening now in stormy boiling to blow the lid clean off entirely. You take no greater pleasure in life than from cooking; from combining the myriad ingredients of game and fowl and fish alike, into new creations, the one more dazzling and fantastical on the tongue than the next, and you have perhaps even begun developing a bit of a reputation for your work with knife and ladle. The blood of the first Hearthling was made from red clay, boiled in water, and seasoned with the breath of life, by the Old Gods. Stirring the Cook's Pot promises...",
                new String[] {"Increased speed when cooking.", "Constitution +15", "All foods cooked give less satiations. (4% instead 5%, food marked as 'well prepared')", "Cooking +30", "Chance to double the output when cooking."}, "Hunter", "Farmer"),
            new CatalogCredo("Scholar", "scholar.png",
                "Through ardent studies of worlds natural and hidden, you have grown into an accomplished knower of things seen and unseen, and yet your mind unsatiated reaches further still, into vast troves of wisdom yet unsounded. You have, lately, in love of knowledge, felt the beckon of a new calling, and wisdom becomes every day more and more an end in itself to you, apart from pratical application. You are a mentor to the young, a wise elder, or a recluse in exile among tomes and parchments, and only seldom one finds your fingers without a trace of ink, or the dust of old manuscripts. The old texts speak of a time before the Hearth was lit, when it burned only as a spark in the eyes of the old gods. From the first curiosity all knowledge took flame. Learning the Scholar's Wisdom promises...",
                new String[] {"Intelligence +15, Lore +15", "Small chance to avoid consuming studied curiosities.", "'Contemplation & Meditation' cost reduced by half.", "Extra row and column on your Study Desk.", "Ability to create Scholarly Accounts."}, "Gardener", "Quarryman"),
            new CatalogCredo("Herder", "herder.png",
                "You have traveled far and wide, and your herds have grown plentiful in your footsteps. You have become a Herder, always working diligently for the health and bounty of your animals and herds, grazing upon the lush grasslands of the hearth. You spend your days on horseback, tending to the needs of your flocks, keeping beasts of prey away, and following the seasonal migrations in search of greener pastures. The sagas say that the Old Gods once drove a herd of cattle across the firmament, and that the stars sprung to light where their hooves trod. Undertaking the Herders's Trek promises...",
                new String[] {"Wild animals always accept clover.", "Your branded animals eats 20% less.", "Pony Power of horses lasts longer.", "Your branded animals have shorter gestation period.", "Sizeable quality bonus to all domestic meats. (10%)"}, "Farmer", "Nomad"),
            new CatalogCredo("Wandering Sage", "wandering-sage.png",
                "You have wandered the Hearth, deep in mystical contemplation on the signs and omens, and sought to commune with far flung Rowan trees, dark streams, and stands of Alder. Your learned wisdom exceeds most, and yet new depths of ignorance reveal themselves behind each new revelation of spirited nature. You can hear the spirits of things vast and faint whisper to you from the shadowy world beyond the veil of the Hearth, and they trace your footsteps wherever your wanderlust aching takes you. Meditation, and the journey as a goal in itself, are your lodestars. You have seen, in terrifying visions, how the Old Gods rode in chariots of Ash and Bronze, thundering over the ancient steppes in ages beyond reckoning, and your feet long always to trace those furrows. Embarking on the Wandering Sage's Pilgrimage promises...",
                new String[] {"Gain less Travel Weariness (-20%).", "+5% experience gain.", "Reduced stamina drain from walking.", "Frequent experience gain when seeing Natural Wonders (trigger random +250 exp without lore event).", "+15% bonus to quest rewards."}, "Mystic", "Nomad")

	};

	private static class CredoInfo extends Widget {
	    private static final int IMG_H = UI.scale(150);
	    private final RichTextBox text = add(new RichTextBox(Coord.z, ifnd, null), Coord.z);
	    private Tex image;
	    private boolean catalog;

	    CredoInfo() {
		super(Coord.z);
		text.bg = new Color(0, 0, 0, 128);
	    }

	    void clear() {
		clearImage();
		catalog = false;
		text.set((RichText.Document)null);
		layout();
	    }

	    void setLive(Indir<? extends RichText.Document> doc) {
		clearImage();
		catalog = false;
		text.set(doc);
		layout();
	    }

	    void setCatalog(CredoEntry entry) {
		clearImage();
		catalog = true;
		image = entry.catalogImage();
		text.set(entry.catalogText());
		layout();
	    }

	    private void clearImage() {
		if(image != null) {
		    image.dispose();
		    image = null;
		}
	    }

	    private void layout() {
		int top = (catalog && (image != null)) ? IMG_H : 0;
		text.move(Coord.of(0, top));
		text.resize(Coord.of(sz.x, Math.max(1, sz.y - top)));
	    }

	    public void resize(Coord sz) {
		super.resize(sz);
		layout();
	    }

	    public void draw(GOut g) {
		g.chcolor(0, 0, 0, 128);
		g.frect(Coord.z, sz);
		g.chcolor();
		if(catalog && (image != null)) {
		    int h = Math.min(IMG_H - UI.scale(16), image.sz().y);
		    int w = (image.sz().x * h) / image.sz().y;
		    g.aimage(image, Coord.of(UI.scale(10), UI.scale(10)), 0, 0, Coord.of(w, h));
		}
		super.draw(g);
	    }

	    public void destroy() {
		clearImage();
		super.destroy();
	    }
	}

	private static String displayName(SkillWnd.Credo cr) {
	    try {
		return(cr.res.get().flayer(Resource.tooltip).t);
	    } catch(Loading l) {
		return(cr.nm);
	    }
	}

	private static String[] reqs(String name) {
	    CatalogCredo cr = catalog(name);
	    return((cr == null) ? new String[0] : cr.requires);
	}

	private static CatalogCredo catalog(String name) {
	    for(CatalogCredo cr : CREDO_CATALOG) {
		if(cr.name.equals(name))
		    return(cr);
	    }
	    return(null);
	}

	private static String key(String name) {
	    return(name.toLowerCase().replaceAll("[^a-z0-9]", ""));
	}

	private static class CredoGridView extends Scrollport {
	    private final CredoLogWindow owner;
	    private final Coord crsz = UI.scale(44, 56);
	    private final int m = UI.scale(6);
	    private String sig = "";

	    CredoGridView(CredoLogWindow owner) {
		super(Coord.z);
		this.owner = owner;
	    }

	    void refresh() {
		SkillWnd.CredoGrid grid = owner.credos();
		String nsig = signature(grid);
		if(Utils.eq(sig, nsig))
		    return;
		sig = nsig;
		for(Widget ch = cont.child; ch != null; ch = cont.child)
		    ch.destroy();
		int y = 0;
		if(grid == null) {
		    cont.add(new Label("Credos are not loaded yet."), m, y);
		    y += UI.scale(24);
		    y = section("Unavailable", unavailable(Collections.emptyList()), y);
		    cont.update();
		    return;
		}
		if(grid.pcr != null)
		    y = section("Pursuing", entries(Collections.singletonList(grid.pcr)), y);
		if(!grid.ncr.isEmpty())
		    y = section("Available", entries(grid.ncr), y);
		if(!grid.ccr.isEmpty())
		    y = section("Acquired", entries(grid.ccr), y);
		List<CredoEntry> unavailable = unavailable(grid);
		if(!unavailable.isEmpty())
		    y = section("Unavailable", unavailable, y);
		cont.update();
	    }

	    private String signature(SkillWnd.CredoGrid grid) {
		if(grid == null)
		    return("none");
		StringBuilder buf = new StringBuilder();
		append(buf, grid.pcr);
		buf.append(':').append(grid.pcl).append('/').append(grid.pclt).append(':').append(grid.pcql).append('/').append(grid.pcqlt).append('\n');
		for(SkillWnd.Credo cr : grid.ncr)
		    append(buf, cr);
		buf.append('|');
		for(SkillWnd.Credo cr : grid.ccr)
		    append(buf, cr);
		buf.append('|').append(sz.x);
		return(buf.toString());
	    }

	    private void append(StringBuilder buf, SkillWnd.Credo cr) {
		if(cr == null) {
		    buf.append("null\n");
		} else {
		    buf.append(cr.nm).append(':').append(cr.has).append('\n');
		}
	    }

	    private List<CredoEntry> entries(List<SkillWnd.Credo> credos) {
		List<CredoEntry> ret = new ArrayList<>();
		for(SkillWnd.Credo cr : credos)
		    ret.add(new CredoEntry(cr));
		return(ret);
	    }

	    private List<CredoEntry> unavailable(SkillWnd.CredoGrid grid) {
		List<SkillWnd.Credo> live = new ArrayList<>();
		if(grid != null) {
		    if(grid.pcr != null)
			live.add(grid.pcr);
		    live.addAll(grid.ncr);
		    live.addAll(grid.ccr);
		}
		return(unavailable(live));
	    }

	    private List<CredoEntry> unavailable(List<SkillWnd.Credo> live) {
		Set<String> seen = new HashSet<>();
		for(SkillWnd.Credo cr : live)
		    seen.add(key(displayName(cr)));
		List<CredoEntry> ret = new ArrayList<>();
		for(CatalogCredo cr : CREDO_CATALOG) {
		    if(!seen.contains(key(cr.name)))
			ret.add(new CredoEntry(cr));
		}
		return(ret);
	    }

	    private int section(String title, List<CredoEntry> credos, int y) {
		cont.add(new Label(title, new Text.Foundry(Text.serif.deriveFont(java.awt.Font.BOLD), 15).aa(true)), m, y);
		y += UI.scale(22);
		int col = 0;
		int cols = Math.max(1, (Math.max(1, sz.x - UI.scale(16))) / (crsz.x + m));
		List<CredoEntry> sorted = new ArrayList<>(credos);
		Collections.sort(sorted, Comparator.comparing(cr -> cr.name));
		for(CredoEntry cr : sorted) {
		    cont.add(new CredoIcon(cr), m + (col * (crsz.x + m)), y);
		    if(++col >= cols) {
			col = 0;
			y += crsz.y + m;
		    }
		}
		if(col != 0)
		    y += crsz.y + m;
		return(y + UI.scale(8));
	    }

	    private class CredoIcon extends Widget {
		private final CredoEntry cr;
		private Tex img;
		private Tex initials;

		CredoIcon(CredoEntry cr) {
		    super(crsz);
		    this.cr = cr;
		    this.tooltip = Text.render(cr.name);
		}

		private Tex img() {
		    if(img == null) {
			BufferedImage base = null;
			if(cr.live != null) {
			    base = cr.live.res.get().flayer(Resource.imgc).img;
			} else {
			    Tex catalog = cr.catalogImage();
			    if(catalog instanceof TexI)
				base = ((TexI)catalog).back;
			    if(catalog != null)
				catalog.dispose();
			}
			if(base != null) {
			    img = new TexI(convolvedown(base, crsz, iconfilter));
			} else {
			    BufferedImage ph = TexI.mkbuf(crsz);
			    java.awt.Graphics2D g = ph.createGraphics();
			    g.setColor(new Color(45, 45, 45, 180));
			    g.fillRect(0, 0, crsz.x, crsz.y);
			    g.setColor(new Color(140, 140, 140));
			    g.drawRect(0, 0, crsz.x - 1, crsz.y - 1);
			    g.dispose();
			    img = new TexI(ph);
			}
		    }
		    return(img);
		}

		public void draw(GOut g) {
		    g.image(img(), Coord.z);
		    if(cr.unavailable) {
			g.chcolor(0, 0, 0, 120);
			g.frect(Coord.z, sz);
			if(cr.image == null) {
			    g.chcolor(185, 185, 185, 220);
			    g.aimage(initials(), sz.div(2), 0.5, 0.5);
			}
			g.chcolor();
		    }
		    if((owner.selected != null) && key(cr.name).equals(key(owner.selected.name))) {
			g.chcolor(255, 255, 0, 120);
			g.frect(Coord.z, sz);
			g.chcolor();
		    }
		}

		public boolean mousedown(MouseDownEvent ev) {
		    if(ev.b == 1) {
			owner.select(cr);
			return(true);
		    }
		    return(super.mousedown(ev));
		}

		public void destroy() {
		    if(img != null)
			img.dispose();
		    if(initials != null)
			initials.dispose();
		    super.destroy();
		}

		private Tex initials() {
		    if(initials == null) {
			String[] parts = cr.name.split(" ");
			String label = parts.length > 1 ? (parts[0].substring(0, 1) + parts[1].substring(0, 1)) : cr.name.substring(0, Math.min(2, cr.name.length()));
			initials = Text.render(label).tex();
		    }
		    return(initials);
		}
	    }
	}
    }
}
