/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public interface MapSource {
    public int gettile(Coord tc);
    public double getfz(Coord tc);
    default public double getfz2(Coord tc) {return getfz(tc);}
    public Tileset tileset(int t);
    public Tiler tiler(int t);

    static BufferedImage tileimg(MapSource m, BufferedImage[] texes, int t) {
	BufferedImage img = texes[t];
	if(img == null) {
	    Tileset set = m.tileset(t);
	    if(set == null)
		return(null);
	    Resource r = set.getres();
	    Resource.Image ir = r.layer(Resource.imgc);
	    if(ir == null)
		return(null);
	    img = ir.img;
	    texes[t] = img;
	}
	return(img);
    }
    
    public static int getColor(MapSource m, int index)
    {
	int result = 0;
	try
	{
	    int runSpd = 0;
	    String resname = m.tileset(index).getres().name;
	    if (Utils.PVP_MAP.containsKey(resname))
		runSpd = Utils.PVP_MAP.get(resname);
	    if (runSpd == 3)
		result = Color.decode("#9a6d0a").getRGB();
	    if (runSpd == 4)
		result = Color.decode("#44770b").getRGB();
	}
	catch (Exception x)
	{}
	return result;
    }

    public static BufferedImage drawmap(MapSource m, Area a) {
	Coord sz = a.sz();
	BufferedImage[] texes = new BufferedImage[256];
	int[] dColor = new int[256];
	Arrays.fill(dColor, -1);
	BufferedImage buf = TexI.mkbuf(sz);
	Coord c = new Coord();
	for(c.y = 0; c.y < sz.y; c.y++) {
	    for(c.x = 0; c.x < sz.x; c.x++) {
		int t = m.gettile(a.ul.add(c));
		if(t < 0) {
		    buf.setRGB(c.x, c.y, 0);
		    continue;
		}
		BufferedImage tex = tileimg(m, texes, t);
		int rgb = 0;
		if (CFG.PVP_MAP.get()) {
		    rgb = getColor(m, t);
		    if(tex != null && rgb == 0)
			rgb = getDominantColor(tex, dColor, t);
		}
		else
		{
		    if(tex != null)
			rgb = tex.getRGB(Utils.floormod(c.x + a.ul.x, tex.getWidth()),
			    Utils.floormod(c.y + a.ul.y, tex.getHeight()));
		}
		buf.setRGB(c.x, c.y, rgb);
	    }
	}
	for(c.y = 1; c.y < sz.y - 1; c.y++) {
	    for(c.x = 1; c.x < sz.x - 1; c.x++) {
		int t = m.gettile(a.ul.add(c));
		if(t < 0)
		    continue;
		try {
		    Tiler tl = m.tiler(t);
		    if(tl instanceof haven.resutil.Ridges.RidgeTile) {
			if(haven.resutil.Ridges.brokenp(m, a.ul.add(c))) {
			    for(int y = c.y - 1; y <= c.y + 1; y++) {
				for(int x = c.x - 1; x <= c.x + 1; x++) {
				    Color cc = new Color(buf.getRGB(x, y));
				    buf.setRGB(x, y, Utils.blendcol(cc, Color.BLACK, ((x == c.x) && (y == c.y))?1:0.1).getRGB());
				}
			    }
			}
		    }
		} catch(RuntimeException exc) {
		    /* XXX: Tileset resources loaded from cache can contain outdated
		     * and illegal references. Catching them and ignoring them here
		     * seems like an ugly hack, but what is the better alternative? */
		}
	    }
	}
	if (!CFG.PVP_MAP.get() && !CFG.REMOVE_BIOME_BORDER_FROM_MINIMAP.get())
	    for(c.y = 0; c.y < sz.y; c.y++) {
		for(c.x = 0; c.x < sz.x; c.x++) {
		    int t = m.gettile(a.ul.add(c));
		    if((m.gettile(a.ul.add(c).add(-1, 0)) > t) ||
		       (m.gettile(a.ul.add(c).add( 1, 0)) > t) ||
		       (m.gettile(a.ul.add(c).add(0, -1)) > t) ||
		       (m.gettile(a.ul.add(c).add(0,  1)) > t))
			buf.setRGB(c.x, c.y, Color.BLACK.getRGB());
		}
	    }
	return(buf);
    }
    
    public static int getDominantColor(BufferedImage image, int[] colors, int index)
    {
	if (colors[index] == -1)
	{
	    colors[index] = findDominantColor(image, false, -1).getRGB();
	}
	return colors[index];
    }
    
    public static Color findDominantColor(BufferedImage paramBufferedImage, boolean getBoundaryColor, int boundThickness) {
	int margin = boundThickness;
	int totRed = 0;
	int totGreen = 0;
	int totBlue = 0;
	int totAlpha = 0;
	int imgWt = paramBufferedImage.getWidth(), imgHt = paramBufferedImage.getHeight();
	Rectangle north = null, south = null, west = null, east = null;
	if (margin > 0) {
	    north = new Rectangle(0, 0, imgWt, margin);
	    west = new Rectangle(0, 0, margin, imgHt);
	    south = new Rectangle(0, imgHt - margin, imgWt, margin);
	    east = new Rectangle(imgWt - margin, 0, margin, imgHt);
	}
	for (int irow = 0; irow < imgHt; irow++) {
	    for (int icol = 0; icol < imgWt; icol++) {
		if (getBoundaryColor && margin > 0) {
		    boolean validPixels = false;
		    if (north.contains(icol, irow) || south.contains(icol, irow) || west.contains(icol, irow)
			|| east.contains(icol, irow)) {
			validPixels = true;
		    }
		    if (!validPixels)
			continue;
		}
		if (paramBufferedImage.getRGB(icol, irow) == 0)
		    totAlpha++;
		else {
		    Color pixelColor = new Color(paramBufferedImage.getRGB(icol, irow));
		    totRed += pixelColor.getRed();
		    totGreen += pixelColor.getGreen();
		    totBlue += pixelColor.getBlue();
		}
	    }
	}
	int totPixels = (imgHt * imgWt - totAlpha);
	if (getBoundaryColor)
	    totPixels = 2 * (imgHt * margin) + 2 * (imgWt * margin) - totAlpha;
	if (totPixels <= 0)
	    totPixels = 1;
	int red = totRed / totPixels, green = totGreen / totPixels, blue = totBlue / totPixels;
	
	Color localColor2 = new Color(red, green, blue);
	return localColor2;
    }
}
