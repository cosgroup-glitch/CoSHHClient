package haven.sprites;

import haven.*;
import haven.render.RenderTree;

import java.awt.Color;

public class AttackRangeSprite extends Sprite {
    private final ColoredRadius radius;

    public AttackRangeSprite(Gob owner, double range) {
	super(owner, null);
	radius = new ColoredRadius(owner, (float)range, new Color(255, 255, 255, 24), new Color(255, 255, 255, 210));
    }

    public void added(RenderTree.Slot slot) {
	slot.add(radius);
    }
}
