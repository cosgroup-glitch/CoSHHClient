package haven.sprites;

import haven.Coord;
import haven.Coord2d;
import haven.GOut;

import java.awt.Color;

public class PingSprite extends MapSprite {
    private static final double RADIUS = 14;
    private static final Color BACKGROUND = new Color(255, 255, 255, 210);

    private final Color col;
    private double timetolive;

    public PingSprite(Coord2d rc, Color col, int timetolive) {
	this.rc = rc;
	this.col = col;
	this.timetolive = timetolive;
    }

    public void draw(GOut g, Coord pos, double zoomlevel) {
	g.chcolor(BACKGROUND);
	g.fellipse(pos, Coord.of(UI_RADIUS(5)));
	g.chcolor(col);
	drawRing(g, pos, (9 * timetolive + 10) % RADIUS);
	drawRing(g, pos, (9 * timetolive + 5) % RADIUS);
	drawRing(g, pos, (9 * timetolive) % RADIUS);
	g.chcolor();
    }

    private static int UI_RADIUS(int r) {
	return haven.UI.scale(r);
    }

    private void drawRing(GOut g, Coord c, double radius) {
	int steps = 20;
	Coord prev = null;
	for(int i = 0; i <= steps; i++) {
	    double a = (Math.PI * 2 * i) / steps;
	    Coord next = Coord.of((int)Math.round(c.x + (Math.cos(a) * radius)),
		(int)Math.round(c.y + (Math.sin(a) * radius)));
	    if(prev != null)
		g.line(prev, next, haven.UI.scale(2));
	    prev = next;
	}
    }

    public boolean tick(double dt) {
	timetolive -= dt;
	return timetolive <= 0;
    }
}
