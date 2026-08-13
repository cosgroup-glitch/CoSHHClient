package me.ender.gob;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import haven.Config;
import haven.Gob;
import haven.GobTag;

import java.util.HashMap;
import java.util.Map;

public class GobContents {
    private static final Map<String, Map<String, String>> DATA = new HashMap<>();
    private static final String SMELTER = "tag/smelter";

    
    public static final String SEED = "Seed";
    public static final String LEAF = "Leaf";
    public static final String BARK = "Bark";
    public static final String BOUGH = "Bough";
    
    public static final String READY = "Ready";
    public static final String COLD = "Cold";
    
    private static final String ANIMAL = "tag/animal";
    public static final String FLEECE = "Fleece";
    
    public static final String WATER = "Water";
    public static final String FOOD = "Food";
    public static final String FLOWER = "Flower";
    public static final String NOT_PLANTED = "NotPlanted";
    public static final String SOIL = "Soil";
    
    static {
	final Gson gson = new Gson();
	try {
	    final Map<String, Map<String, String>> cfg = gson.fromJson(Config.loadJarFile("gob_contents.json5"), new TypeToken<Map<String, Map<String, String>>>() {
	    }.getType());
	    DATA.putAll(cfg);
	} catch (Exception ignored) {
	}
    }
    
    public static Map<String, String> getData(Gob gob) {
	if(gob.is(GobTag.SMELTER)) {return DATA.get(SMELTER);}
	if(gob.is(GobTag.ANIMAL)) {return DATA.get(ANIMAL);}
	String resid = gob.resid();
	if(resid == null) {return null;}
	return DATA.get(resid);
    }
}
