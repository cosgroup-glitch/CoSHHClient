package haven;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Tracks map tiles which have entered the player's loaded view. */
public class ExploredArea {
    private static final int GRID_SIZE = MCache.cmaps.x;
    private static final int MASK_SIZE = GRID_SIZE * MCache.cmaps.y;
    private static final long SAVE_DELAY = 5000;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static class GridKey {
	final long segment;
	final Coord grid;

	GridKey(long segment, Coord grid) {
	    this.segment = segment;
	    this.grid = grid;
	}

	public boolean equals(Object o) {
	    if(!(o instanceof GridKey))
		return(false);
	    GridKey that = (GridKey)o;
	    return((segment == that.segment) && grid.equals(that.grid));
	}

	public int hashCode() {
	    return(Objects.hash(segment, grid));
	}
    }

    private static class Mask {
	final BitSet bits = new BitSet(MASK_SIZE);
	long version;
    }

    private static class SavedGrid {
	long segment;
	int x, y;
	String mask;
	String seen;
    }

    private static class SavedData {
	int version;
	boolean sessionActive;
	List<SavedGrid> explored;
	List<SavedGrid> session;
    }

    private final File file;
    private final Map<GridKey, Mask> explored = new HashMap<>();
    private final Map<GridKey, Mask> session = new HashMap<>();
    private final Map<GridKey, int[]> sessionSeen = new HashMap<>();
    private boolean sessionActive;
    private boolean expiryEnabled;
    private boolean loaded, loadFailed, dirty;
    private long lastSave;
    private long revision;
    private final java.util.function.LongSupplier clock;
    private int lastTimedMinute = Integer.MIN_VALUE;
    private int lastExpiryMinute = Integer.MIN_VALUE;
    private Coord lastUL, lastBR;
    private long lastSegment = Long.MIN_VALUE;

    public ExploredArea(String genus) {
	String path = Config.genusFile("explored-area.json", (genus == null) ? "" : genus);
	this.file = Config.getFile(path);
	this.clock = System::currentTimeMillis;
    }

    ExploredArea(File file, java.util.function.LongSupplier clock) {
	this.file = file;
	this.clock = clock;
    }

    public synchronized void ensureLoaded() {
	if(loaded)
	    return;
	loaded = true;
	lastSave = clock.getAsLong();
	if(Files.notExists(file.toPath()))
	    return;
	try {
	    String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	    SavedData data = gson.fromJson(json, SavedData.class);
	    if((data == null) || (data.version != 1) || (data.explored == null) || (data.session == null))
		throw(new IOException("empty explored-area data"));
	    decode(data.explored, explored, false);
	    decode(data.session, session, true);
	    sessionActive = data.sessionActive;
	    expiryEnabled = CFG.MMAP_EXPLORED_SESSION_EXPIRE.get();
	    if(expiryEnabled)
		initializeMissingSeen(currentMinute());
	} catch(Exception e) {
	    loadFailed = true;
	    explored.clear();
	    session.clear();
	    sessionSeen.clear();
	    new Warning(e, "could not read explored-area data; leaving it untouched").issue();
	}
    }

    private void decode(List<SavedGrid> source, Map<GridKey, Mask> target, boolean sessionLayer) throws IOException {
	if(source == null)
	    return;
	for(SavedGrid saved : source) {
	    if((saved == null) || (saved.mask == null))
		throw(new IOException("missing explored-area mask"));
	    BitSet bits = BitSet.valueOf(Base64.getDecoder().decode(saved.mask));
	    if(bits.length() > MASK_SIZE)
		throw(new IOException("oversized explored-area mask"));
	    if(!bits.isEmpty()) {
		Mask mask = new Mask();
		mask.bits.or(bits);
		mask.version = ++revision;
		GridKey key = new GridKey(saved.segment, Coord.of(saved.x, saved.y));
		if(target.put(key, mask) != null)
		    throw(new IOException("duplicate explored-area grid"));
		if(sessionLayer && (saved.seen != null))
		    sessionSeen.put(key, decodeSeen(saved.seen));
	    }
	}
    }

    public synchronized void update(Coord ul, Coord br, long segment) {
	ensureLoaded();
	syncExpirySetting();
	int minute = currentMinute();
	boolean timedRefresh = expiryEnabled && sessionActive && (minute != lastTimedMinute);
	if(!timedRefresh && Objects.equals(ul, lastUL) && Objects.equals(br, lastBR) && (segment == lastSegment))
	    return;
	lastUL = ul;
	lastBR = br;
	lastSegment = segment;
	boolean changed = mark(explored, ul, br, segment);
	if(sessionActive) {
	    changed |= mark(session, ul, br, segment);
	    if(expiryEnabled) {
		markSeen(ul, br, segment, minute);
		lastTimedMinute = minute;
		dirty = true;
	    }
	}
	if(changed)
	    dirty = true;
    }

    private void markSeen(Coord ul, Coord br, long segment, int minute) {
	Coord gul = ul.div(GRID_SIZE);
	Coord gbr = br.sub(1, 1).div(GRID_SIZE);
	for(int gy = gul.y; gy <= gbr.y; gy++) {
	    for(int gx = gul.x; gx <= gbr.x; gx++) {
		Coord gc = Coord.of(gx, gy);
		GridKey key = new GridKey(segment, gc);
		int[] seen = sessionSeen.computeIfAbsent(key, ignored -> new int[MASK_SIZE]);
		Coord origin = gc.mul(GRID_SIZE);
		int x0 = Math.max(0, ul.x - origin.x);
		int y0 = Math.max(0, ul.y - origin.y);
		int x1 = Math.min(GRID_SIZE, br.x - origin.x);
		int y1 = Math.min(MCache.cmaps.y, br.y - origin.y);
		for(int y = y0; y < y1; y++)
		    java.util.Arrays.fill(seen, (y * GRID_SIZE) + x0, (y * GRID_SIZE) + x1, minute);
	    }
	}
    }

    private boolean mark(Map<GridKey, Mask> target, Coord ul, Coord br, long segment) {
	Coord gul = ul.div(GRID_SIZE);
	Coord gbr = br.sub(1, 1).div(GRID_SIZE);
	boolean changed = false;
	for(int gy = gul.y; gy <= gbr.y; gy++) {
	    for(int gx = gul.x; gx <= gbr.x; gx++) {
		Coord gc = Coord.of(gx, gy);
		Mask mask = target.computeIfAbsent(new GridKey(segment, gc), key -> new Mask());
		Coord origin = gc.mul(GRID_SIZE);
		int x0 = Math.max(0, ul.x - origin.x);
		int y0 = Math.max(0, ul.y - origin.y);
		int x1 = Math.min(GRID_SIZE, br.x - origin.x);
		int y1 = Math.min(MCache.cmaps.y, br.y - origin.y);
		int before = mask.bits.cardinality();
		for(int y = y0; y < y1; y++)
		    mask.bits.set((y * GRID_SIZE) + x0, (y * GRID_SIZE) + x1);
		if(mask.bits.cardinality() != before) {
		    mask.version = ++revision;
		    changed = true;
		}
	    }
	}
	return(changed);
    }

    /** Iterate recorded grids, never all possible grids at a distant zoom level. */
    public synchronized List<Coord> grids(long segment) {
	ensureLoaded();
	List<Coord> result = new ArrayList<>();
	for(GridKey key : explored.keySet()) {
	    if(key.segment == segment)
		result.add(key.grid);
	}
	return(result);
    }

    public synchronized long version(long segment, Coord grid, boolean sessionLayer) {
	ensureLoaded();
	Mask mask = (sessionLayer ? session : explored).get(new GridKey(segment, grid));
	return((mask == null) ? -1 : mask.version);
    }

    public synchronized BitSet mask(long segment, Coord grid, boolean sessionLayer) {
	ensureLoaded();
	Mask mask = (sessionLayer ? session : explored).get(new GridKey(segment, grid));
	return((mask == null) ? null : (BitSet)mask.bits.clone());
    }

    public synchronized boolean sessionActive() {
	ensureLoaded();
	return(sessionActive);
    }

    public synchronized boolean toggleSession() {
	ensureLoaded();
	if(sessionActive) {
	    sessionActive = false;
	    session.clear();
	    sessionSeen.clear();
	} else {
	    session.clear();
	    sessionSeen.clear();
	    sessionActive = true;
	    lastUL = lastBR = null;
	    lastSegment = Long.MIN_VALUE;
	}
	dirty = true;
	return(sessionActive);
    }

    public synchronized void tick() {
	ensureLoaded();
	syncExpirySetting();
	if(expiryEnabled && sessionActive)
	    expireOldTiles();
	if(dirty && !loadFailed && ((clock.getAsLong() - lastSave) >= SAVE_DELAY))
	    save();
    }

    private void syncExpirySetting() {
	boolean enabled = CFG.MMAP_EXPLORED_SESSION_EXPIRE.get();
	if(enabled == expiryEnabled)
	    return;
	expiryEnabled = enabled;
	if(enabled) {
	    initializeMissingSeen(currentMinute());
	    lastTimedMinute = Integer.MIN_VALUE;
	} else {
	    sessionSeen.clear();
	}
	dirty = true;
    }

    private void initializeMissingSeen(int minute) {
	for(Map.Entry<GridKey, Mask> entry : session.entrySet()) {
	    int[] seen = sessionSeen.computeIfAbsent(entry.getKey(), ignored -> new int[MASK_SIZE]);
	    for(int bit = entry.getValue().bits.nextSetBit(0); bit >= 0; bit = entry.getValue().bits.nextSetBit(bit + 1)) {
		if(seen[bit] == 0)
		    seen[bit] = minute;
	    }
	}
    }

    private void expireOldTiles() {
	int minute = currentMinute();
	if(minute == lastExpiryMinute)
	    return;
	lastExpiryMinute = minute;
	int cutoff = minute - Utils.clip(CFG.MMAP_EXPLORED_SESSION_MINUTES.get(), 15, 30);
	Iterator<Map.Entry<GridKey, Mask>> entries = session.entrySet().iterator();
	while(entries.hasNext()) {
	    Map.Entry<GridKey, Mask> entry = entries.next();
	    int[] seen = sessionSeen.get(entry.getKey());
	    if(seen == null)
		continue;
	    Mask mask = entry.getValue();
	    boolean changed = false;
	    for(int bit = mask.bits.nextSetBit(0); bit >= 0; ) {
		int next = mask.bits.nextSetBit(bit + 1);
		if((seen[bit] > 0) && (seen[bit] <= cutoff)) {
		    mask.bits.clear(bit);
		    seen[bit] = 0;
		    changed = true;
		}
		bit = next;
	    }
	    if(changed) {
		mask.version = ++revision;
		dirty = true;
	    }
	    if(mask.bits.isEmpty()) {
		entries.remove();
		sessionSeen.remove(entry.getKey());
	    }
	}
    }

    private int currentMinute() {
	return((int)(clock.getAsLong() / 60000L));
    }

    public synchronized void close() {
	if(dirty)
	    save();
    }

    private void save() {
	if(loadFailed)
	    return;
	SavedData data = new SavedData();
	data.version = 1;
	data.explored = new ArrayList<>();
	data.session = new ArrayList<>();
	data.sessionActive = sessionActive;
	encode(explored, data.explored, false);
	encode(session, data.session, true);
	Path target = file.toPath().toAbsolutePath();
	try {
	    Path parent = target.getParent();
	    if(parent != null)
		Files.createDirectories(parent);
	    Path temp = Files.createTempFile(parent, target.getFileName() + "-", ".tmp");
	    Files.write(temp, gson.toJson(data).getBytes(StandardCharsets.UTF_8));
	    try {
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
	    } catch(java.nio.file.AtomicMoveNotSupportedException e) {
		Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
	    }
	    dirty = false;
	    lastSave = clock.getAsLong();
	} catch(IOException e) {
	    new Warning(e, "could not save explored-area data").issue();
	}
    }

    private void encode(Map<GridKey, Mask> source, List<SavedGrid> target, boolean sessionLayer) {
	for(Map.Entry<GridKey, Mask> entry : source.entrySet()) {
	    if(entry.getValue().bits.isEmpty())
		continue;
	    SavedGrid saved = new SavedGrid();
	    saved.segment = entry.getKey().segment;
	    saved.x = entry.getKey().grid.x;
	    saved.y = entry.getKey().grid.y;
	    saved.mask = Base64.getEncoder().encodeToString(entry.getValue().bits.toByteArray());
	    if(sessionLayer && expiryEnabled) {
		int[] seen = sessionSeen.get(entry.getKey());
		if(seen != null)
		    saved.seen = encodeSeen(seen);
	    }
	    target.add(saved);
	}
    }

    private static String encodeSeen(int[] seen) {
	try {
	    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	    try(DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
		for(int value : seen)
		    out.writeInt(value);
	    }
	    return(Base64.getEncoder().encodeToString(bytes.toByteArray()));
	} catch(IOException e) {
	    throw(new RuntimeException(e));
	}
    }

    private static int[] decodeSeen(String encoded) throws IOException {
	int[] seen = new int[MASK_SIZE];
	byte[] compressed = Base64.getDecoder().decode(encoded);
	try(DataInputStream in = new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(compressed)))) {
	    for(int i = 0; i < seen.length; i++)
		seen[i] = in.readInt();
	    if(in.read() != -1)
		throw(new IOException("oversized explored-area timestamps"));
	}
	return(seen);
    }
}
