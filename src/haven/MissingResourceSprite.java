package haven;

import haven.render.*;
import java.util.LinkedHashSet;
import java.util.Set;

/** Built-in world-object marker; no resource downloads or cache access. */
public final class MissingResourceSprite extends Sprite implements Sprite.CUpd {
    static final Resource RESOURCE = new Resource.Virtual(null, "local/missing-world-object", 1);
    private static final Set<String> reported = new LinkedHashSet<>();

    public MissingResourceSprite(Owner owner, Resource.LoadFailedException failure) {
        super(owner, RESOURCE);
        report(failure);
    }

    private static void report(Resource.LoadFailedException failure) {
        String key = failure.name + " (v" + failure.ver + ")";
        synchronized(reported) {
            if(!reported.add(key))
                return;
            if(reported.size() > 128)
                reported.remove(reported.iterator().next());
        }
        new Warning(failure, "Using purple world-object placeholder for " + key).issue();
    }

    // Shared, fixed geometry is initialized only when a failed object is rendered.
    // Both shades remain visible without downloaded textures or lighting data.
    private static class Geometry {
        private static final float[] POINTS = {
            -5, -5, 0,   5, -5, 0,   5, 5, 0,   -5, 5, 0,
            -5, -5, 12,  5, -5, 12,  5, 5, 12,  -5, 5, 12
        };
        private static final VertexArray VERTICES = new VertexArray(
                new VertexArray.Layout(new VertexArray.Layout.Input(Homo3D.vertex,
                        new VectorFormat(3, NumberFormat.FLOAT32), 0, 0, 12)),
                new VertexArray.Buffer(POINTS.length * 4, DataBuffer.Usage.STATIC,
                        DataBuffer.Filler.of(POINTS)));
        private static RenderTree.Node faces(short[] indices, BaseColor color) {
            Model model = new Model(Model.Mode.TRIANGLES, VERTICES,
                    new Model.Indices(indices.length, NumberFormat.UINT16,
                            DataBuffer.Usage.STATIC, DataBuffer.Filler.of(indices)));
            return Pipe.Op.compose(color, new States.Facecull(States.Facecull.Mode.NONE),
                    MixColor.slot.nil).apply(model);
        }
        static final RenderTree.Node LIGHT = faces(new short[] {
            4,5,6, 4,6,7, 0,1,5, 0,5,4, 2,3,7, 2,7,6
        }, new BaseColor(210, 35, 255, 255));
        static final RenderTree.Node DARK = faces(new short[] {
            0,2,1, 0,3,2, 1,2,6, 1,6,5, 3,0,4, 3,4,7
        }, new BaseColor(100, 15, 150, 255));
    }

    @Override
    public void added(RenderTree.Slot slot) {
        // Inherit GobClick and the object's transform so the marker stays selectable.
        slot.add(Geometry.LIGHT);
        slot.add(Geometry.DARK);
    }

    @Override
    public void update(Message state) {
        // Keep the marker for state updates; do not repeatedly query a failed loader.
    }
}
