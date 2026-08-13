package haven;

import haven.MenuGrid.Pagina;
import haven.res.ui.tt.alch.effect.Effect;
import haven.res.ui.tt.level.Level;
import haven.resutil.FoodInfo;

import java.awt.*;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static haven.BAttrWnd.Constipations.*;

public class ItemData {
    public static boolean DBG = Config.get().getprop("ender.debug.items", "off").equals("on");
    public static final String WATER = "Water";
    public static final String TEA = "Tea";
    
    private ItemData() {}
    
    public static Tex longtip(Pagina pagina, boolean widePagina) {
	return longtip(pagina, widePagina, 0, 0);
    }
    
    public static Tex longtip(Pagina pagina, boolean widePagina, int titleSize, int titleSpace) {
	List<ItemInfo> infos = pagina.button().info();
	if(infos == null || infos.isEmpty()) {
	    return null;
	}
	return longtip(pagina.res(), infos, widePagina, titleSize, titleSpace);
    }
    
    private static Tex longtip(Resource res, List<ItemInfo> infos, boolean widePagina, int titleSize, int titleSpace) {
	Resource.AButton ad = res.layer(Resource.action);
	Resource.Pagina pg = res.layer(Resource.pagina);
	Resource.Tooltip tip = res.layer(Resource.tooltip);
	String spacing = new String(new char[titleSpace]).replace("\0", " ");
	String tt = String.format("$b{%s%s}", spacing, ad != null ? ad.name : (tip != null) ? tip.t : res.name);
	if(titleSize > 0) {
	    tt = String.format("$size[%d]{%s}", titleSize, tt);
	}
	if(pg == null) {widePagina = false;}
	
	if(widePagina) {
	    tt += "\n\n" + pg.text;
	    infos = infos.stream()
		.filter(i -> !(i instanceof ItemInfo.Pagina))
		.collect(Collectors.toList());
	}
	
	BufferedImage img = MenuGrid.ttfnd.render(tt, UI.scale(300)).img;
	
	if(!infos.isEmpty()) {
	    img = ItemInfo.catimgs(UI.scale(20), img, ItemInfo.longtip(infos));
	}
	return new TexI(img);
    }
    
    public static boolean hasFoodInfo(GItem item) {
	try {
	    return item.info().stream().anyMatch(i -> i instanceof FoodInfo);
	} catch (Loading ignored) {}
	return false;
    }
    
    public static boolean isSalted(GItem item) {
	try {
	    return item.info().stream().anyMatch(i -> i instanceof ItemInfo.AdHoc
		&& "Salted".equals(((ItemInfo.AdHoc) i).str.text));
	} catch (Loading ignored) {}
	return false;
    }
    
    public static boolean hasIngredientInfo(GItem item) {
	try {
	    return item.info().stream().anyMatch(i -> i instanceof Effect);
	} catch (Loading ignored) {}
	return false;
    }
    
    public static void modifyFoodTooltip(ItemInfo.Owner owner, CompImage cmp, int[] types, double glut, double fepSum) {
	cmp.add(RichText.render(String.format("Base FEP: $col[128,255,0]{%s}, FEP/Hunger: $col[128,255,0]{%s}", Utils.odformat2(fepSum, 2), FEPPerHunger(glut, fepSum)), 0).img, FoodInfo.PAD);
	
	//this is not real item, don't add extra info
	if(!(owner instanceof GItem)) {return;}
	CharacterInfo character = null;
	CharacterInfo.Constipation constipation = null;
	try {
	    character = owner.context(Session.class).character;
	    constipation = character.constipation;
	} catch (NullPointerException | OwnerContext.NoContext ignore) {}
	
	if(character == null) {return;}
	
	List<FEPMod> mods = new ArrayList<>();
	boolean showCategories = CFG.DISPLAY_FOOD_CATEGORIES.get();
	
	character.getEnergyFEPMod().ifPresent(mods::add);
	
	//satiation
	if(types.length > 0) {
	    //TODO: find a way to get actual categories like meat, dairy, offal etc.
	    if(showCategories) {cmp.add(Text.render("Categories:").img, FoodInfo.PAD);}
	    
	    double satiation = 1;
	    for (int type : types) {
		CharacterInfo.Constipation.Data c = constipation.get(type);
		if(c != null) {
		    if(showCategories) {cmp.add(constipation.render(FoodInfo.class, c), FoodInfo.PAD);}
		    satiation = Math.min(satiation, c.value);
		}
	    }
	    if(satiation != 1) {mods.add(new FEPMod(satiation, "satiation"));}
	}
	
	//hunger
	if(Math.abs(character.gluttony - 1) > 0.005d) {
	    mods.add(new FEPMod(character.gluttony, "hunger"));
	}
	
	//TODO: add table bonuses
	
	//account
	character.getAccountFEPBonus().ifPresent(mods::add);
	
	if(mods.isEmpty()) {return;}
	double fullMult = 1;
	
	cmp.add(RichText.render("Effectiveness:").img, FoodInfo.PAD);
	for (FEPMod mod : mods) {
	    cmp.add(mod.img(), FoodInfo.PAD);
	    fullMult *= mod.val;
	}
	double adjustedFEP = fepSum * fullMult;
	cmp.add(RichText.render(String.format("Adjusted FEP: %s, FEP/Hunger: $col[200,150,255]{%s}", RichText.color(Utils.odformat2(adjustedFEP, 2), color(fullMult)), FEPPerHunger(glut, adjustedFEP)), 0).img, FoodInfo.PAD);
    }
    
    private static String FEPPerHunger(double glut, double fepSum) {
	return glut != 0
	    ? Utils.odformat2(fepSum / (1000d * glut), 3)
	    : fepSum == 0 ? "0" : "∞";
    }
    
    public static class FEPMod {
	public final double val;
	public final String text;
	private BufferedImage img;
	
	public FEPMod(double val, String text) {
	    this.val = val;
	    this.text = text;
	}
	
	public BufferedImage img() {
	    if(img == null) {
		img = RichText.render(String.format("     ×%s %s", RichText.color(String.format("%.2f", val), color(val)), text), 0).img;
	    }
	    return img;
	}
    }
    
    public interface ITipData {
	ItemInfo create(Session sess);
    }
    
    public static float getMaxCapacity(WItem item) {
	Level level = item.fullness.get();
	if(level != null) {return (float) level.max;}
	
	//TODO: find a better way - maybe config file?
	String name = item.item.resname();
	if(name.contains("/bucket")) {return 1000f;}
	if(name.endsWith("/glassjug")) {return 500f;}
	if(name.endsWith("/waterskin")) {return 300f;}
	if(name.endsWith("/waterflask")) {return 200f;}
	if(name.contains("/kuksa")) {return 80f;}
	
	return 0f;
    }
    
    public static class Content {
	private static final Pattern PARSE = Pattern.compile("([\\d.]*) ([\\w]+) of (.*)");
	public final String name;
	public final String unit;
	public final float count;
	public final QualityList q;
	
	public Content(String name, String unit, float count) {
	    this(name, unit, count, QualityList.make(Collections.emptyList()));
	}
	
	public Content(String name, String unit, float count, QualityList q) {
	    this.name = name;
	    this.unit = unit;
	    this.count = count;
	    this.q = q;
	}
	
	public static Content parse(String name) {
	    return parse(name, QualityList.make(Collections.emptyList()));
	}
	
	public static Content parse(String name, QualityList q) {
	    Matcher m = PARSE.matcher(name);
	    if(m.find()) {
		float count = 0;
		try {
		    count = Float.parseFloat(m.group(1));
		} catch (Exception ignored) {}
		return new Content(m.group(3), m.group(2), count, q);
	    }
	    return new Content(name, "", 1, q);
	}
	
	public String name() {
	    if(unit != null && unit.startsWith("seed")) {
		return String.format("Seeds of %s", name);
	    }
	    return name;
	}
	
	public boolean is(String what) {
	    if(name == null || what == null) {
		return false;
	    }
	    return name.contains(what);
	}
	
	public boolean empty() {return count == 0 || name == null;}
	
	public static final Content EMPTY = new Content(null, null, 0);
    }
    
    public static class DebugInfo extends ItemInfo.Tip {
	public static RichText.Foundry fnd = new RichText.Foundry(
	    TextAttribute.FAMILY, "monospaced",
	    TextAttribute.SIZE, 10,
	    TextAttribute.FOREGROUND, Color.LIGHT_GRAY
	);
	public final BufferedImage img;
	
	public DebugInfo(GItem item) {
	    super(item);
	    this.img = fnd.render(item.resname()).img;
	}
	
	@Override
	public int order() {return 20000;}
	
	public BufferedImage tipimg() {
	    return (img);
	}
    }
}
