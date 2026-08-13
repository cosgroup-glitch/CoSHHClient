package me.ender;

import haven.Gob;
import haven.res.lib.vmat.AttrMats;

import java.util.stream.Collectors;

import static haven.CFG.*;

public class CustomizeVarMat {
    public static boolean NoMat(Gob gob) {
	if(gob == null) {return false;}
	String resId = gob.resid();
	if(ResName.CUPBOARD.equals(resId)) {
	    return DISPLAY_NO_MAT_CUPBOARDS.get();
	}
	return false;
    }
    
    public static String formatMaterials(Gob gob) {
	if(gob == null) {return null;}
	AttrMats mats = gob.getattr(AttrMats.class);
	if(mats == null || mats.res == null || mats.res.isEmpty()) {return null;}
	
	return String.format("Materials:\n- %s", mats.res.stream().map(ClientUtils::prettyResName).collect(Collectors.joining("\n- ")));
    }
}
