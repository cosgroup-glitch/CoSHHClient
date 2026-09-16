/* Preprocessed source code */
package haven.res.ui.music;

import haven.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import javax.sound.midi.*;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

/* >wdg: MusicWnd */
@haven.FromResource(name = "ui/music", version = 35)
public class MusicWnd extends Window {
    public Tex[] tips;
    public Map<Integer, Integer> keys;
    public int[] nti, shi;
    public int[] ntp, shp;
    public Tex[] ikeys;
    public boolean[] cur;
    public final int[] act;
    public final double start;
    public double latcomp = 0.15;
    public int actn;

    public boolean originalLayout;
    public double tempo = 1.0;
    public Label tempoLabel = new Label("Tempo Factor: " + tempo);
    public HafenMidiplayer hafenMidiplayer = null;
    private Thread midiThread;
    private String selectedMidiFile = Utils.getpref("instrument-midi-file", "midiFiles/example.mid");
    private Label midiFileLabel = null;
    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;

    public MusicWnd(String name, int maxpoly) {
	super(Coord.z, name, true);
	setMusicWndLayout(!CFG.IMPROVED_INSTRUMENT_MUSIC_WINDOW.get());
	this.act = new int[maxpoly];
	this.start = System.currentTimeMillis() / 1000.0;
    }

    public static Widget mkwidget(UI ui, Object[] args) {
	String nm = (String)args[0];
	int maxpoly = (Integer)args[1];
	return(new MusicWnd(nm, maxpoly));
    }

    protected void added() {
	super.added();
	ui.grabkeys(this);
    }

    public void cdraw(GOut g) {
	boolean[] cact = new boolean[cur.length];
	for(int i = 0; i < actn; i++) {
	    if(act[i] >= 0 && act[i] < cact.length)
		cact[act[i]] = true;
	}
	int base = originalLayout ? 12 : 0;
	if(originalLayout) {
	    if(ui.modshift) base += 12;
	    if(ui.modctrl)  base -= 12;
	}
	for(int i = 0; i < nti.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * ntp[i], 0);
	    boolean a = cact[nti[i] + base];
	    g.image(ikeys[a ? 1 : 0], c);
	    g.image(tips[nti[i]], c.add((ikeys[0].sz().x - tips[nti[i]].sz().x) / 2, ikeys[0].sz().y - tips[nti[i]].sz().y - (a ? UI.scale(9) : UI.scale(12))));
	}
	int sho = ikeys[0].sz().x - (ikeys[2].sz().x / 2);
	for(int i = 0; i < shi.length; i++) {
	    Coord c = new Coord(ikeys[0].sz().x * shp[i] + sho, 0);
	    boolean a = cact[shi[i] + base];
	    g.image(ikeys[a ? 3 : 2], c);
	    g.image(tips[shi[i]], c.add((ikeys[2].sz().x - tips[shi[i]].sz().x) / 2, ikeys[2].sz().y - tips[shi[i]].sz().y - (a ? UI.scale(9) : UI.scale(12))));
	}
    }

    public boolean keydown(KeyDownEvent ev) {
	double now = (ev.awt.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.awt.getKeyCode());
	if(keyp != null) {
	    int key = keyp;
	    if(originalLayout) {
		key += 12;
		if((ev.awt.getModifiersEx() & KeyMatch.S) != 0) key += 12;
		if((ev.awt.getModifiersEx() & KeyMatch.C) != 0) key -= 12;
	    }
	    playnote(now, key);
	    return(true);
	}
	super.keydown(ev);
	return(true);
    }

    private void playnote(double now, int key) {
	if((key < 0) || (key >= cur.length) || cur[key])
	    return;
	if(actn >= act.length) {
	    wdgmsg("stop", act[0], (float)(now - start));
	    for(int i = 1; i < actn; i++)
		act[i - 1] = act[i];
	    actn--;
	}
	wdgmsg("play", key, (float)(now - start));
	cur[key] = true;
	act[actn++] = key;
    }

    private void stopnote(double now, int key) {
	if((key >= 0) && (key < cur.length) && cur[key]) {
	    outer: for(int i = 0; i < actn; i++) {
		if(act[i] == key) {
		    wdgmsg("stop", key, (float)(now - start));
		    for(actn--; i < actn; i++)
			act[i] = act[i + 1];
		    break outer;
		}
	    }
	    cur[key] = false;
	}
    }

    public boolean keyup(KeyUpEvent ev) {
	double now = (ev.awt.getWhen() / 1000.0) + latcomp;
	Integer keyp = keys.get(ev.awt.getKeyCode());
	if(keyp != null) {
	    int key = keyp;
	    if(originalLayout) {
		stopnote(now, key);
		stopnote(now, key + 12);
		stopnote(now, key + 24);
	    } else {
		stopnote(now, key);
	    }
	    return(true);
	}
	return(super.keyup(ev));
    }

    public void setMusicWndLayout(boolean originalLayout) {
	if(originalLayout) {
	    nti = new int[] {0, 2, 4, 5, 7, 9, 11};
	    shi = new int[] {1, 3, 6, 8, 10};
	    ntp = new int[] {0, 1, 2, 3, 4, 5, 6};
	    shp = new int[] {0, 1, 3, 4, 5};
	    cur = new boolean[12 * 3];

	    Map<Integer, Integer> km = new HashMap<Integer, Integer>();
	    km.put(KeyEvent.VK_Z,  0);
	    km.put(KeyEvent.VK_S,  1);
	    km.put(KeyEvent.VK_X,  2);
	    km.put(KeyEvent.VK_D,  3);
	    km.put(KeyEvent.VK_C,  4);
	    km.put(KeyEvent.VK_V,  5);
	    km.put(KeyEvent.VK_G,  6);
	    km.put(KeyEvent.VK_B,  7);
	    km.put(KeyEvent.VK_H,  8);
	    km.put(KeyEvent.VK_N,  9);
	    km.put(KeyEvent.VK_J, 10);
	    km.put(KeyEvent.VK_M, 11);
	    Tex[] il = new Tex[4];
	    for(int i = 0; i < 4; i++)
		il[i] = Resource.classres(MusicWnd.class).layer(Resource.imgc, i).tex();
	    String tc = "ZSXDCVGBHNJM";
	    Text.Foundry fnd = new Text.Foundry(Text.fraktur.deriveFont(java.awt.Font.BOLD, 16)).aa(true);
	    Tex[] tl = new Tex[tc.length()];
	    for(int i = 0; i < nti.length; i++) {
		int ki = nti[i];
		tl[ki] = fnd.render(tc.substring(ki, ki + 1), Color.BLACK).tex();
	    }
	    for(int i = 0; i < shi.length; i++) {
		int ki = shi[i];
		tl[ki] = fnd.render(tc.substring(ki, ki + 1), Color.WHITE).tex();
	    }
	    keys = km;
	    ikeys = il;
	    tips = tl;
	    resize(ikeys[0].sz().mul(nti.length, 1));
	} else {
	    nti = new int[] {0, 2, 4, 5, 7, 9, 11, 12, 14, 16, 17, 19, 21, 23, 24, 26, 28, 29, 31, 33, 35};
	    shi = new int[] {1, 3, 6, 8, 10, 13, 15, 18, 20, 22, 25, 27, 30, 32, 34};
	    ntp = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
	    shp = new int[] {0, 1, 3, 4, 5, 7, 8, 10, 11, 12, 14, 15, 17, 18, 19};
	    cur = new boolean[36];

	    Map<Integer, Integer> km = new HashMap<Integer, Integer>();
	    String s = "1234567890QWERTYUIOPASDFGHJKLZXCVBNM";
	    int[] keycodes = {49, 50, 51, 52, 53, 54, 55, 56, 57, 48, 81, 87, 69, 82, 84, 89, 85, 73, 79, 80, 65, 83, 68, 70, 71, 72, 74, 75, 76, 90, 88, 67, 86, 66, 78, 77};
	    for(int i = 0; i < keycodes.length; i++)
		km.put(keycodes[i], i);
	    Tex[] il = new Tex[4];
	    for(int i = 0; i < 4; i++)
		il[i] = Resource.remote().loadwait("ui/music").layer(Resource.imgc, i).tex();
	    Text.Foundry fnd = new Text.Foundry(Text.fraktur.deriveFont(java.awt.Font.BOLD, 16)).aa(true);
	    Tex[] tl = new Tex[s.length()];
	    for(int i = 0; i < nti.length; i++) {
		int ki = nti[i];
		tl[ki] = fnd.render(s.substring(ki, ki + 1), Color.BLACK).tex();
	    }
	    for(int i = 0; i < shi.length; i++) {
		int ki = shi[i];
		tl[ki] = fnd.render(s.substring(ki, ki + 1), Color.WHITE).tex();
	    }
	    keys = km;
	    ikeys = il;
	    tips = tl;
	    resize(ikeys[0].sz().x * nti.length, UI.scale(200));
	    addMidiControls();
	}
	this.originalLayout = originalLayout;
    }

    private void addMidiControls() {
	hafenMidiplayer = new HafenMidiplayer();
	midiThread = new HackThread(hafenMidiplayer, "Hafen MIDI file player");
	midiThread.setDaemon(true);
	midiThread.start();

	midiFileLabel = add(new Label(midiFileDisplay()), UI.scale(10), sz.y - UI.scale(140));
	add(new Button(UI.scale(100), "Browse...") {
	    public void click() {
		File file = chooseMidiFile();
		if(file != null) {
		    selectedMidiFile = file.getAbsolutePath();
		    Utils.setpref("instrument-midi-file", selectedMidiFile);
		    midiFileLabel.settext(midiFileDisplay());
		}
		parent.setfocus(this);
	    }
	}, new Coord(UI.scale(220), sz.y - UI.scale(145)));

	add(new Button(UI.scale(100), "Play") {
	    public void click() {
		Utils.setpref("instrument-midi-file", selectedMidiFile);
		hafenMidiplayer.startPlaying(selectedMidiFile);
		parent.setfocus(this);
	    }
	}, new Coord(UI.scale(10), sz.y - UI.scale(120)));

	Button stopButton = add(new Button(UI.scale(100), "Stop") {
	    public void click() {
		hafenMidiplayer.stopPlaying();
		parent.setfocus(this);
	    }
	}, new Coord(UI.scale(115), sz.y - UI.scale(120)));
	setfocus(stopButton);

	add(new Button(UI.scale(120), "Party playing") {
	    public void click() {
		if((ui == null) || (ui.gui == null) || (ui.gui.chat == null))
		    return;
		Utils.setpref("instrument-midi-file", selectedMidiFile);
		for(Widget w = ui.gui.chat.lchild; w != null; w = w.prev) {
		    if(w instanceof ChatUI.MultiChat) {
			ChatUI.MultiChat chat = (ChatUI.MultiChat)w;
			if(chat.name().equals("Party")) {
			    chat.send("HFMPL@@@" + (System.currentTimeMillis() + 1000) + "|" + selectedMidiFile);
			    break;
			}
		    }
		}
	    }
	}, new Coord(UI.scale(430), sz.y - UI.scale(120)));

	HSlider tempoHSlider = new HSlider(UI.scale(180), 1, 20, (int)(tempo * 10)) {
	    public void changed() {
		tempo = val / 10.0;
		hafenMidiplayer.setTempo((float)tempo);
		tempoLabel.settext("Tempo Factor: " + tempo);
	    }
	};
	add(tempoLabel, new Coord(UI.scale(230), sz.y - UI.scale(130)));
	add(tempoHSlider, new Coord(UI.scale(230), sz.y - UI.scale(110)));
    }

    private String midiFileDisplay() {
	if((selectedMidiFile == null) || selectedMidiFile.trim().isEmpty())
	    return("No MIDI file selected");
	File file = midiFile(selectedMidiFile);
	String name = file.getName();
	if(name.length() > 38)
	    name = name.substring(0, 35) + "...";
	return(name);
    }

    private File chooseMidiFile() {
	JFileChooser chooser = new JFileChooser();
	chooser.setFileFilter(new FileNameExtensionFilter("MIDI files (*.mid, *.midi)", "mid", "midi"));
	File current = midiFile(selectedMidiFile);
	File dir = current.isDirectory() ? current : current.getParentFile();
	if((dir != null) && dir.exists())
	    chooser.setCurrentDirectory(dir);
	if(current.isFile())
	    chooser.setSelectedFile(current);
	int result = chooser.showOpenDialog(null);
	if(result == JFileChooser.APPROVE_OPTION)
	    return(chooser.getSelectedFile());
	return(null);
    }

    private File midiFile(String path) {
	File file = new File(path);
	return(file.isAbsolute() ? file : new File(Config.HOMEDIR, path));
    }

    public class CustomReceiver implements Receiver {
	public void send(MidiMessage message, long timeStamp) {
	    if(message instanceof ShortMessage) {
		ShortMessage sm = (ShortMessage)message;
		if((sm.getChannel() == 9) || (sm.getChannel() == 10) || (sm.getChannel() == 11))
		    return;
		if(sm.getCommand() == NOTE_ON) {
		    int velocity = sm.getData2();
		    if(velocity > 0)
			keydown2(getHafenKey(sm), System.currentTimeMillis());
		    else
			keyup2(getHafenKey(sm), System.currentTimeMillis());
		} else if(sm.getCommand() == NOTE_OFF) {
		    keyup2(getHafenKey(sm), System.currentTimeMillis());
		}
	    }
	}

	public int getHafenKey(ShortMessage sm) {
	    int key = sm.getData1();
	    int octave = (key / 12) - 1;
	    int note = key % 12;
	    while(octave > 5)
		octave--;
	    while(octave < 3)
		octave++;
	    return(note + (octave - 3) * 12);
	}

	public void close() {}
    }

    public class HafenMidiplayer implements Runnable {
	public Receiver synthRcvr = new CustomReceiver();
	private Transmitter seqTrans;
	private Sequencer sequencer;
	private volatile boolean active = true;
	private volatile boolean start = false;
	private volatile boolean stop = false;
	private volatile boolean changedTempo = false;
	private volatile boolean synchPlay = false;
	private volatile long timeToPlay = 0;
	private volatile String midiFile = "";
	private volatile float tempo = 1f;

	public void run() {
	    while(active) {
		try {
		    if(start) {
			start = false;
			openSequence();
			sequencer.start();
		    }
		    if(changedTempo && (sequencer != null)) {
			sequencer.setTempoFactor(tempo);
			changedTempo = false;
		    }
		    if(stop) {
			stop = false;
			if(sequencer != null)
			    sequencer.stop();
		    }
		    if(synchPlay) {
			synchPlay = false;
			long delay = timeToPlay - System.currentTimeMillis();
			if((delay < 100) || (delay > 1000)) {
			    if((ui != null) && (ui.gui != null))
				ui.gui.error("Your clock is out of sync; update Windows internet time.");
			    continue;
			}
			openSequence();
			Thread.sleep(delay);
			sequencer.start();
		    }
		    Thread.sleep(50);
		} catch(InterruptedException e) {
		    Thread.currentThread().interrupt();
		    active = false;
		} catch(Exception ignored) {
		}
	    }
	    closeSequencer();
	}

	private void openSequence() throws Exception {
	    closeSequencer();
	    Sequence sequence = MidiSystem.getSequence(midiFile(midiFile));
	    sequencer = MidiSystem.getSequencer(false);
	    seqTrans = sequencer.getTransmitter();
	    seqTrans.setReceiver(synthRcvr);
	    sequencer.open();
	    sequencer.setSequence(sequence);
	    sequencer.setTempoFactor(tempo);
	}

	private void closeSequencer() {
	    try {
		if(seqTrans != null)
		    seqTrans.close();
	    } catch(Exception ignored) {
	    } finally {
		seqTrans = null;
	    }
	    try {
		if(sequencer != null)
		    sequencer.close();
	    } catch(Exception ignored) {
	    } finally {
		sequencer = null;
	    }
	}

	public void startPlaying(String path) {
	    midiFile = path;
	    start = true;
	}

	public void stopPlaying() {
	    stop = true;
	}

	public void kill() {
	    active = false;
	    stop = true;
	    if(midiThread != null)
		midiThread.interrupt();
	}

	public void setTempo(float tempo) {
	    this.tempo = tempo;
	    changedTempo = true;
	}

	public void synchPlay(long timeToPlay, String track) {
	    midiFile = track;
	    this.timeToPlay = timeToPlay;
	    synchPlay = true;
	}
    }

    public boolean keydown2(int hafenkey, long time) {
	playnote((time / 1000.0) + latcomp, hafenkey);
	return(true);
    }

    public boolean keyup2(int hafenkey, long time) {
	stopnote((time / 1000.0) + latcomp, hafenkey);
	return(true);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
	if((sender == this) && "close".equals(msg)) {
	    if(hafenMidiplayer != null)
		hafenMidiplayer.kill();
	    reqdestroy();
	}
	super.wdgmsg(sender, msg, args);
    }
}
