package haven;

import java.util.concurrent.atomic.AtomicInteger;

public class DrawableFallbackTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if(!value) throw new AssertionError(message);
    }
    private static void propagates(RuntimeException expected, Runnable task) {
        try {
            task.run();
            throw new AssertionError("Expected error to propagate");
        } catch(RuntimeException actual) {
            check(actual == expected, "Unexpected error was swallowed or replaced");
        }
    }
    public static void main(String[] args) {
        Gob gob = new Gob();
        Resource good = new Resource("gfx/terobjs/example");
        Indir<Resource> goodRef = good.indir();
        ResDrawable healthy = new ResDrawable(gob, goodRef, MessageBuf.nil);
        check(healthy.getres() == good && healthy.getires() == goodRef, "Healthy identity preserved");
        check(healthy.loadFailure == null && Sprite.creates == 1, "Healthy sprite factory used");

        Resource.LoadFailedException failure = new Resource.LoadFailedException(
                "gfx/terobjs/bumlings/leadglance2", 2);
        AtomicInteger requests = new AtomicInteger();
        OCache.resource = () -> { requests.incrementAndGet(); throw failure; };
        ResDrawable.$cres delta = new ResDrawable.$cres();
        delta.apply(gob, new OCache.AttrDelta((byte)1));
        ResDrawable missing = (ResDrawable)gob.drawable;
        check(missing.spr instanceof MissingResourceSprite, "Failed delta installs placeholder");
        check(missing.loadFailure == failure, "Original failure retained for diagnostics");
        check(missing.getres() == MissingResourceSprite.RESOURCE, "Safe local metadata");
        check(missing.getires().get() == missing.getres(), "Metadata access cannot rethrow failed lookup");
        check(missing.resId().equals("local/missing-world-object"), "Unavailable resource not treated as loaded");
        check(Sprite.creates == 1, "Fallback does not invoke downloaded sprite factory");
        delta.apply(gob, new OCache.AttrDelta((byte)2));
        check(gob.drawable == missing && missing.sdtnum() == 2, "State update retains marker and payload");
        check(requests.get() == 1, "State update does not retry failed resource");
        missing.ctick(0.1);
        missing.gtick(new haven.render.Render());
        missing.added(new haven.render.RenderTree.Slot());
        check(missing.eqpoint("none", MessageBuf.nil) == null, "Absent equipment metadata safe");
        missing.dispose();
        OCache.resource = goodRef;
        delta.apply(gob, new OCache.AttrDelta((byte)2));
        check(gob.drawable != missing && gob.drawable.getres() == good, "Later resource replacement works");
        check(gob.notifications == 3, "All server drawable updates complete");

        Resource.NoSuchResourceException absent = new Resource.NoSuchResourceException("absent", 1);
        check(new ResDrawable(gob, () -> { throw absent; }, MessageBuf.nil).loadFailure == absent,
                "Missing-resource subtype also falls back");
        Loading loading = new Loading();
        propagates(loading, () -> new ResDrawable(gob, () -> { throw loading; }, MessageBuf.nil));
        RuntimeException bug = new IllegalStateException("programming error");
        propagates(bug, () -> new ResDrawable(gob, () -> { throw bug; }, MessageBuf.nil));
        Sprite.createFailure = bug;
        propagates(bug, () -> new ResDrawable(gob, goodRef, MessageBuf.nil));
        Sprite.createFailure = null;
        String target = "gfx/terobjs/cupboard";
        Resource targetResource = new Resource(target);
        ResDrawable visual = new ResDrawable(gob, targetResource.indir(), MessageBuf.nil);
        if(target.equals(System.getProperty("kami.test.missing-world-resource", ""))) {
            check(visual.spr instanceof MissingResourceSprite, "Explicit test target uses fallback");
            check(visual.loadFailure.name.equals(target), "Diagnostic retains target name");
            check(visual.loadFailure.getCause().getMessage().contains("Simulated failure"),
                    "Injected failure is clearly identified");
            ResDrawable unaffected = new ResDrawable(gob, goodRef, MessageBuf.nil);
            check(unaffected.loadFailure == null, "Non-targeted objects remain normal");
        } else {
            check(visual.loadFailure == null && visual.getres() == targetResource,
                    "Normal launch restores original object without changing any data");
        }
        System.out.println("PASS: " + checks + " drawable fallback checks");
    }
}
