package haven;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MiningAutoDrop {
    private static final Set<String> STONES = new HashSet<>(Arrays.asList(
        "gneiss", "basalt", "cinnabar", "sunstone", "dolomite", "feldspar", "flint", "granite",
        "hornblende", "limestone", "marble", "porphyry", "quartz", "sandstone", "schist", "zincspar",
        "apatite", "sodalite", "fluorospar", "soapstone", "olivine", "gabbro", "alabaster", "microlite",
        "mica", "kyanite", "corund", "orthoclase", "breccia", "diabase", "arkose", "diorite", "slate",
        "jasper", "rhyolite", "pegmatite", "greenschist", "eclogite", "pumice", "serpentine", "chert",
        "graywacke", "halite"));
    private static final Set<String> COAL = new HashSet<>(Arrays.asList("blackcoal", "coal"));
    private static final Set<String> ORES = new HashSet<>(Arrays.asList(
        "cassiterite", "chalcopyrite", "malachite", "ilmenite", "limonite", "hematite", "magnetite",
        "peacockore", "leadglance", "cuprite"));
    private static final Set<String> PRECIOUS = new HashSet<>(Arrays.asList(
        "galena", "argentite", "hornsilver", "petzite", "sylvanite", "nagyagite"));
    private static final Set<String> CURIOS = new HashSet<>(Arrays.asList(
        "catgold", "petrifiedshell", "strangecrystal"));

    public static boolean shouldDrop(WItem item) {
        if(!CFG.MINING_AUTO_DROP_ENABLED.get() || item.ui == null || item.ui.gui == null)
            return false;
        if(CFG.MINING_AUTO_DROP_CURSOR_ONLY.get() && !item.ui.isCursor("gfx/hud/curs/mine"))
            return false;
        Inventory inv = item.getparent(Inventory.class);
        if(inv == null || (!CFG.MINING_AUTO_DROP_CONTAINERS.get() && inv != item.ui.gui.maininv))
            return false;
        if(item.item.contents != null)
            return false;
        try {
            double q = item.quality();
            if(q <= 0.1)
                return false;
            String name = item.item.resource().basename();
            return (CFG.MINING_AUTO_DROP_STONES.get() && q < CFG.MINING_AUTO_DROP_STONES_Q.get() && STONES.contains(name)) ||
                (CFG.MINING_AUTO_DROP_COAL.get() && q < CFG.MINING_AUTO_DROP_COAL_Q.get() && COAL.contains(name)) ||
                (CFG.MINING_AUTO_DROP_ORES.get() && q < CFG.MINING_AUTO_DROP_ORES_Q.get() && ORES.contains(name)) ||
                (CFG.MINING_AUTO_DROP_PRECIOUS.get() && q < CFG.MINING_AUTO_DROP_PRECIOUS_Q.get() && PRECIOUS.contains(name)) ||
                (CFG.MINING_AUTO_DROP_CURIOS.get() && q < CFG.MINING_AUTO_DROP_CURIOS_Q.get() && CURIOS.contains(name)) ||
                (CFG.MINING_AUTO_DROP_QUARRYARTZ.get() && q < CFG.MINING_AUTO_DROP_QUARRYARTZ_Q.get() && name.equals("quarryquartz"));
        } catch(Loading ignored) {
            return false;
        }
    }
}
