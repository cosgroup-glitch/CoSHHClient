import java.awt.Color;
import java.awt.image.BufferedImage;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/** Exercises the actual packaged label method without constructing a client or game session. */
public final class GobInfoTextTest {
    public static void main(String[] args) throws Exception {
        if(args.length != 1) throw new IllegalArgumentException("Expected a new test fixture directory");
        Path root = Paths.get(args[0]).toRealPath();
        // Verify isolation before any client class is initialized by reflection.
        for(String value : new String[]{System.getenv("APPDATA"), System.getProperty("user.home"), System.getProperty("user.dir")})
            if(value == null || !Paths.get(value).toRealPath().startsWith(root)) throw new AssertionError("Test paths must stay inside fixture");
        if(!"true".equals(System.getProperty("java.awt.headless")) || !"workdir".equals(System.getProperty("config.homedir")))
            throw new AssertionError("Expected headless, fixture-only configuration");

        // Follow normal startup order; loading GeneralGobInfo first can initialize
        // Text during UI's scale initialization and create a zero-sized test font.
        Class.forName("haven.Config");
        Class.forName("haven.UI");
        Class<?> info = Class.forName("haven.GeneralGobInfo");
        Method render = info.getDeclaredMethod("text", String.class, Color.class);
        render.setAccessible(true);
        Class<?> text = Class.forName("haven.Text");
        Field image = text.getField("img"), label = text.getField("text");
        ConcurrentHashMap<String, Object> seen = new ConcurrentHashMap<>();
        ForkJoinPool pool = new ForkJoinPool(8);
        try {
            pool.submit(() -> IntStream.range(0, 24000).parallel().forEach(i -> {
                // Many cold keys and repeated shared keys exercise growth and publication.
                String value = i % 5 == 0 ? "Water" : "Barrel content " + (i % 1024);
                Color color = i % 2 == 0 ? new Color(252, 235, 255) : new Color(235, 252, 255);
                try {
                    Object line = render.invoke(null, value, color);
                    if(line == null) throw new AssertionError("Label lookup returned null");
                    BufferedImage img = (BufferedImage)image.get(line);
                    if(img == null || img.getWidth() < 1 || img.getHeight() < 1 || !value.equals(label.get(line)))
                        throw new AssertionError("Incomplete or incorrect rendered label");
                    String key = color.getRGB() + ":" + value;
                    Object first = seen.putIfAbsent(key, line);
                    if(first != null && first != line) throw new AssertionError("Concurrent callers received different entries for the same label");
                } catch(ReflectiveOperationException e) { throw new RuntimeException(e); }
            })).get(45, TimeUnit.SECONDS);
            Object empty = render.invoke(null, "", Color.WHITE);
            if(empty == null || image.get(empty) == null) throw new AssertionError("Empty text must still render safely");
            Object a = render.invoke(null, "Water", Color.WHITE), b = render.invoke(null, "Water", Color.RED);
            if(a == b) throw new AssertionError("Color must remain part of the label identity");
            System.out.println("PASS: 24,000 parallel label lookups; " + seen.size() + " shared keys; non-null images, atomic reuse, empty text, distinct colors.");
        } finally { pool.shutdownNow(); }
    }
}
