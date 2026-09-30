package haven;

import haven.render.*;
import java.util.*;
import java.util.function.Supplier;

// Isolated collaborators for the actual ResDrawable source. No game, network,
// graphics context, settings, or disk cache classes are initialized in this test.
interface Indir<T> { T get(); }
class Loading extends RuntimeException {}
class Resource {
    final String name;
    final int ver = 1;
    Resource(String name) { this.name = name; }
    Indir<Resource> indir() { return () -> this; }
    <T> T layer(Class<T> type, String name) { return null; }
    static class LoadException extends RuntimeException {
        LoadException(String message, Resource resource) { super(message); }
    }
    static class LoadFailedException extends RuntimeException {
        final String name; final int ver;
        LoadFailedException(String name, int ver) { this.name = name; this.ver = ver; }
        LoadFailedException(String name, int ver, LoadException cause) {
            super(cause); this.name = name; this.ver = ver;
        }
    }
    static class NoSuchResourceException extends LoadFailedException {
        NoSuchResourceException(String name, int ver) { super(name, ver); }
    }
}
class Message {
    byte[] data;
    Message(byte... data) { this.data = data; }
    boolean eom() { return data.length == 0; }
    public Message clone() { return new Message(data.clone()); }
}
class MessageBuf extends Message {
    static final MessageBuf nil = new MessageBuf(new byte[0]);
    MessageBuf(byte[] data) { super(data.clone()); }
    MessageBuf(Message source) { this(source.data); }
    public MessageBuf clone() { return new MessageBuf(data); }
    public boolean equals(Object other) {
        return other instanceof MessageBuf && Arrays.equals(data, ((MessageBuf)other).data);
    }
    public int hashCode() { return Arrays.hashCode(data); }
}
class Gob {
    Drawable drawable;
    int notifications;
    void setattr(Drawable value) { drawable = value; }
    <T> T getattr(Class<T> type) { return type.cast(drawable); }
    void drawableUpdated() { notifications++; }
    void idUpdated() {}
    Random mkrandoom() { return new Random(1); }
    interface Placing { Placer placer(); }
    static class Placer {}
}
abstract class Drawable {
    final Gob gob;
    Drawable(Gob gob) { this.gob = gob; }
    public void added(RenderTree.Slot slot) {}
    public abstract Resource getres();
    public abstract Indir<Resource> getires();
    public String resId() { return getres().name; }
    public Gob.Placer placer() { return new Gob.Placer(); }
}
interface EquipTarget { Supplier<? extends Pipe.Op> eqpoint(String name, Message data); }
class Skeleton {
    static class BoneOffset { Supplier<? extends Pipe.Op> from(Object value) { return null; } }
}
class ClassResolver<T> {
    <R> ClassResolver<T> add(Class<R> type, java.util.function.Function<T, R> fn) { return this; }
    <R> R context(Class<R> type, T value, boolean fail) { return null; }
}
class OwnerContext {
    static <T> T orparent(Class<T> type, T value, Gob gob) { return value; }
}
class Sprite {
    interface Owner {}
    interface CUpd { void update(Message data); }
    static int creates;
    static RuntimeException createFailure;
    static Sprite create(Owner owner, Resource resource, Message state) {
        creates++;
        if(createFailure != null) throw createFailure;
        return new Sprite();
    }
    static int decnum(Message state) { return state.eom() ? 0 : state.data[0]; }
    void age() {}
    void tick(double dt) {}
    void gtick(Render render) {}
    void dispose() {}
}
class MissingResourceSprite extends Sprite implements Sprite.CUpd {
    static final Resource RESOURCE = new Resource("local/missing-world-object");
    MissingResourceSprite(Owner owner, Resource.LoadFailedException failure) {}
    public void update(Message state) {}
}
class OCache {
    static final int OD_RES = 1;
    @interface DeltaType { int value(); }
    interface Delta {
        void apply(Gob gob, AttrDelta message);
        static Indir<Resource> getres(Gob gob, int id) { return resource; }
    }
    static Indir<Resource> resource;
    static class AttrDelta {
        boolean old;
        byte state;
        AttrDelta(byte state) { this.state = state; }
        int uint16() { return 0x8001; }
        int uint8() { return 1; }
        byte[] bytes(int length) { return new byte[] {state}; }
    }
}
