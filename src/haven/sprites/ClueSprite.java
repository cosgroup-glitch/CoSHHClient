package haven.sprites;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;

import java.awt.Color;

public class ClueSprite extends MapSprite {
    private static final Color COLOR = Color.WHITE;

    private final double a1;
    private final double a2;
    private final int width;
    private final int length;
    private double timetolive;

    public ClueSprite(Coord2d rc, double a1, double a2, int width, int length, int duration) {
	this.rc = rc;
	this.a1 = -a1;
	this.a2 = -a2;
	this.width = width;
	this.length = length;
	this.timetolive = duration;
    }

    public void draw(GOut g, Coord pos, double zoomlevel) {
	Coord arm1 = Coord.of((int)(Math.cos(a1) * length), (int)(Math.sin(a1) * length)).add(pos);
	Coord arm2 = Coord.of((int)(Math.cos(a2) * length), (int)(Math.sin(a2) * length)).add(pos);
	g.chcolor(COLOR);
	g.clippedLine(pos, arm1, width);
	g.clippedLine(pos, arm2, width);
	g.clippedLine(arm1, arm2, width);
	g.chcolor();
    }

    public boolean tick(double dt) {
	timetolive -= dt;
	return timetolive <= 0;
    }
}
