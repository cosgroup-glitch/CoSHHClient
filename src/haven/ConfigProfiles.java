package haven;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigProfiles {
    public enum Mode {
	COMBAT("combat", "Combat mode"),
	CAMERA("camera", "Camera Mode"),
	BUILDER("builder", "Builder Mode"),
	DEFAULT("default", "Default");

	public final String id;
	public final String label;

	Mode(String id, String label) {
	    this.id = id;
	    this.label = label;
	}

	public static Mode byId(String id) {
	    for(Mode mode : values()) {
		if(mode.id.equals(id))
		    return mode;
	    }
	    return DEFAULT;
	}
    }

    private static final List<CFG<?>> profileCfgs = Arrays.asList(
	CFG.HIDE_PLAYER_NAMES,
	CFG.DISPLAY_KINSFX,
	CFG.DISPLAY_FLAVOR,
	CFG.DISPLAY_GOB_INFO,
	CFG.DISPLAY_GOB_INFO_DISABLED_PARTS,
	CFG.DISPLAY_GOB_INFO_TREE_ENABLED_PARTS,
	CFG.DISPLAY_GOB_INFO_TREE_HIDE_GROWING_PARTS,
	CFG.DISPLAY_GOB_INFO_TREE_SHOW_BIG,
	CFG.DISPLAY_GOB_INFO_TREE_SHOW_BIG_THRESHOLD,
	CFG.DISPLAY_GOB_INFO_SHORT,
	CFG.DISPLAY_GOB_SPEED,
	CFG.DISPLAY_GOB_HITBOX_FILLED,
	CFG.DISPLAY_GOB_HITBOX,
	CFG.DISPLAY_GOB_HITBOX_TOP,
	CFG.COLOR_HBOX_FILLED,
	CFG.COLOR_HBOX_SOLID,
	CFG.COLOR_HBOX_PASSABLE,
	CFG.SHOW_WORLD_GRID,
	CFG.DISPLAY_GOB_PATHS,
	CFG.DISPLAY_GOB_PATHS_FOR,
	CFG.QUEUE_PATHS,
	CFG.HIDE_TREES,
	CFG.SKIP_HIDING_RADAR_TREES,
	CFG.SHOW_GOB_RADIUS,
	CFG.SHOW_MINESWEEPER_OVERLAY,
	CFG.SHOW_CONTAINER_FULLNESS,
	CFG.SHOW_PROGRESS_COLOR,
	CFG.SIMPLE_CROPS,
	CFG.NO_TILE_TRANSITION,
	CFG.FLAT_TERRAIN,
	CFG.FLAT_CAVE_WALLS,
	CFG.DISPLAY_RIDGE_BOX,
	CFG.COLORIZE_DEEP_WATER,
	CFG.DISPLAY_SCALE_CUPBOARDS,
	CFG.MINE_SUPPORT_DANGER_THRESHOLD,
	CFG.DISPLAY_SCALE_WALLS,
	CFG.DISPLAY_DECALS_ON_TOP,
	CFG.DISPLAY_NO_MAT_CUPBOARDS,
	CFG.DISPLAY_AURA_SPEED_BUFF,
	CFG.DISPLAY_AURA_RABBIT,
	CFG.DISPLAY_AURA_CRITTERS,
	CFG.SHOW_TOOLBELT_0,
	CFG.SHOW_TOOLBELT_1,
	CFG.DISABLE_UI_HIDING,
	CFG.UI_DISABLE_CONTAINER_POS,
	CFG.GUI_LOCK,
	CFG.GUI_EDIT_GRID,
	CFG.GUI_EDIT_GRID_SIZE,
	CFG.UI_SHOW_EQPROXY_HAND,
	CFG.UI_SHOW_EQPROXY_POUCH,
	CFG.SHOW_BUILDER_WINDOW,
	CFG.ALT_COMBAT_UI,
	CFG.SIMPLE_COMBAT_OPENINGS,
	CFG.ALWAYS_MARK_COMBAT_TARGET,
	CFG.HIGHLIGHT_PARTY_IN_COMBAT,
	CFG.HIGHLIGHT_SELF_IN_COMBAT,
	CFG.HIGHLIGHT_ENEMY_IN_COMBAT,
	CFG.MARK_PARTY_IN_COMBAT,
	CFG.MARK_SELF_IN_COMBAT,
	CFG.MARK_ENEMY_IN_COMBAT,
	CFG.SHOW_COMBAT_INFO,
	CFG.SHOW_COMBAT_INFO_HEIGHT,
	CFG.SHOW_FLOATING_STAT_WDGS,
	CFG.SHOW_FLOATING_STATS_COMBAT,
	CFG.LOCK_FLOATING_STAT_WDGS,
	CFG.DRAG_COMBAT_UI,
	CFG.KEEP_COMBAT_UI_AFTER_COMBAT,
	CFG.COMBAT_UI_INACTIVE_SCALE,
	CFG.COMBAT_UI_OPENING_DECAY,
	CFG.SHOW_COMBAT_DMG,
	CFG.CLEAR_PLAYER_DMG_AFTER_COMBAT,
	CFG.CLEAR_ALL_DMG_AFTER_COMBAT,
	CFG.SHOW_COMBAT_KEYS,
	CFG.DISABLE_MENU_KEYS,
	CFG.DISABLE_MENU_KEYS_IN_COMBAT,
	CFG.COMBAT_AUTO_PEACE,
	CFG.COMBAT_RE_AGGRO,
	CFG.MMAP_LIST,
	CFG.MMAP_VIEW,
	CFG.MMAP_GRID,
	CFG.MMAP_POINTER,
	CFG.MMAP_CLAIM,
	CFG.MMAP_VILLAGE,
	CFG.MMAP_SHOW_BIOMES,
	CFG.MMAP_SHOW_PATH,
	CFG.MMAP_SHOW_MARKER_NAMES,
	CFG.MMAP_SHOW_PARTY_NAMES,
	CFG.MMAP_SHOW_PARTY_NAMES_STYLE,
	CFG.SHOW_TIME,
	CFG.SHOW_STATS,
	CFG.PVP_MAP,
	CFG.MOVE_COMBAT_UI,
	CFG.DISABLE_WINDOW_ANIMATION,
	CFG.REMOVE_BIOME_BORDER_FROM_MINIMAP,
	CFG.DRAW_OPENINGS_OVER_GOBS,
	CFG.SHOW_MINIMAP_ON_START,
	CFG.DISPLAY_SCALE_TREES,
	CFG.DISPLAY_SCALE_BUSHES,
	CFG.EXTEND_ZOOM_ON_ORTHO,
	CFG.EXTENDED_ORTHO_VIEW,
	CFG.CAMERA_SMOOTH_JITTER,
	CFG.CAMERA_SMOOTH_STRENGTH,
	CFG.CAMERA_ROTATION_SMOOTHING_MS,
	CFG.ANIM_FRAME_SKIP,
	CFG.GOB_INFO_TICK_INTERVAL,
	CFG.FREEZE_DOMESTIC_ANIM,
	CFG.HIDE_DOMESTIC_ANIMALS,
	CFG.PARALLEL_TICK,
	CFG.GL_DISPOSE_PER_FRAME,
	CFG.DISABLE_YULELIGHTS_FX,
	CFG.HIDE_GAMEUI_PORTRAIT,
	CFG.MAP_COMPACT_LOCKED,
	CFG.HIDE_ANIMAL_WARNING_IN_COMBAT,
	CFG.BLOCK_ATTACK_TAMED_HORSE
    );

    private static boolean initialized = false;
    private static boolean applying = false;

    public static synchronized void init() {
	if(initialized)
	    return;
	initialized = true;
	for(CFG<?> cfg : profileCfgs)
	    observe(cfg);
	ensureProfile(Mode.byId(CFG.ACTIVE_CONFIG_PROFILE.get()));
    }

    public static Mode active() {
	init();
	return Mode.byId(CFG.ACTIVE_CONFIG_PROFILE.get());
    }

    public static String activeLabel() {
	return active().label;
    }

    public static synchronized void activate(Mode mode) {
	init();
	ensureProfile(mode);
	CFG.ACTIVE_CONFIG_PROFILE.set(mode.id);
	Map<String, Object> profile = profiles().get(mode.id);
	applying = true;
	try {
	    for(CFG<?> cfg : profileCfgs) {
		if(profile.containsKey(cfg.path()))
		    CFG.setObject(cfg, profile.get(cfg.path()), true);
	    }
	    if(mode == Mode.BUILDER)
		CFG.SHOW_BUILDER_WINDOW.set(true);
	} finally {
	    applying = false;
	}
    }

    private static <T> void observe(CFG<T> cfg) {
	cfg.observe(changed -> save(changed, changed.get()));
    }

    private static synchronized void save(CFG<?> cfg, Object value) {
	if(applying)
	    return;
	Mode mode = Mode.byId(CFG.ACTIVE_CONFIG_PROFILE.get());
	ensureProfile(mode);
	Map<String, Object> profile = profiles().get(mode.id);
	profile.put(cfg.path(), value);
	CFG.CONFIG_PROFILES.set(profiles());
    }

    private static void ensureProfile(Mode mode) {
	Map<String, Map<String, Object>> profiles = profiles();
	if(!profiles.containsKey(mode.id)) {
	    profiles.put(mode.id, snapshot());
	    CFG.CONFIG_PROFILES.set(profiles);
	}
    }

    private static Map<String, Object> snapshot() {
	Map<String, Object> snapshot = new HashMap<>();
	for(CFG<?> cfg : profileCfgs)
	    snapshot.put(cfg.path(), cfg.get());
	return snapshot;
    }

    private static Map<String, Map<String, Object>> profiles() {
	return CFG.CONFIG_PROFILES.get();
    }
}
