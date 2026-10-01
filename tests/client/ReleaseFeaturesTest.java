package haven;

import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Runs actual packaged feature logic in newly allocated synthetic fixtures only. */
public class ReleaseFeaturesTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        if(!ok) throw new AssertionError(message);
        checks++;
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args[0]).toRealPath();
        for(String value : new String[]{System.getenv("APPDATA"), System.getProperty("user.home"), System.getProperty("user.dir")})
            check(value != null && Paths.get(value).toRealPath().startsWith(root), "fixture isolation");
        Class.forName("haven.Config");
        Class.forName("haven.UI");
        exploration(root);
        food();
        updater();
        retries();
        System.out.println("PASS: " + checks + " release feature checks; synthetic fixtures only.");
    }

    private static void exploration(Path root) throws Exception {
        CFG.MMAP_EXPLORED_SESSION_EXPIRE.set(false);
        AtomicLong time = new AtomicLong(1000000000L);
        Path file = root.resolve("exploration.json");
        ExploredArea area = new ExploredArea(file.toFile(), time::get);
        area.update(Coord.of(-1, -1), Coord.of(1, 1), 42);
        check(area.grids(42).size() == 4, "negative coordinates cross four grids");
        check(area.mask(42, Coord.of(-1, -1), false).get(9999), "negative local coordinate");
        check(area.mask(43, Coord.z, false) == null, "segments isolated");
        long version = area.version(42, Coord.z, false);
        area.update(Coord.of(-1, -1), Coord.of(1, 1), 42);
        check(area.version(42, Coord.z, false) == version, "unchanged footprints reuse texture");
        check(area.toggleSession(), "start session");
        area.update(Coord.z, Coord.of(1, 1), 42);
        long sessionVersion = area.version(42, Coord.z, true);
        check(!area.toggleSession(), "stop session");
        check(area.mask(42, Coord.z, true) == null, "stopped session cleared");
        area.toggleSession();
        area.update(Coord.of(2, 2), Coord.of(3, 3), 42);
        check(area.version(42, Coord.z, true) > sessionVersion, "restart invalidates old texture version");
        check(!area.mask(42, Coord.z, true).get(0) && area.mask(42, Coord.z, true).get(202), "new session shape");
        area.close();
        ExploredArea loaded = new ExploredArea(file.toFile(), time::get);
        check(loaded.sessionActive(), "session persistence");
        check(loaded.mask(42, Coord.z, true).equals(area.mask(42, Coord.z, true)), "mask round trip");
        CFG.MMAP_EXPLORED_SESSION_EXPIRE.set(true);
        CFG.MMAP_EXPLORED_SESSION_MINUTES.set(15);
        loaded.tick();
        time.addAndGet(16 * 60000L);
        loaded.tick();
        check(loaded.mask(42, Coord.z, true) == null, "old session tiles expire");
        check(loaded.mask(42, Coord.z, false) != null, "history does not expire");
        loaded.close();
        // Malformed fixture files must remain byte-for-byte intact after update/close.
        String[] invalid = {"null", "{}", "{", "{\"version\":2,\"explored\":[],\"session\":[]}",
            "{\"version\":1,\"explored\":[null],\"session\":[]}",
            "{\"version\":1,\"explored\":[{\"mask\":\"!\"}],\"session\":[]}"};
        for(int i = 0; i < invalid.length; i++) {
            Path bad = root.resolve("invalid-" + i + ".json");
            byte[] bytes = invalid[i].getBytes(StandardCharsets.UTF_8);
            Files.write(bad, bytes);
            ExploredArea broken = new ExploredArea(bad.toFile(), time::get);
            broken.update(Coord.z, Coord.of(1, 1), 42);
            time.addAndGet(6000);
            broken.tick();
            broken.close();
            check(Arrays.equals(bytes, Files.readAllBytes(bad)), "invalid file untouched " + i);
        }
        Path directory = root.resolve("unreadable-as-file");
        Files.createDirectory(directory);
        Path sentinel = directory.resolve("keep");
        Files.write(sentinel, new byte[]{7});
        ExploredArea unreadable = new ExploredArea(directory.toFile(), time::get);
        unreadable.update(Coord.z, Coord.of(1, 1), 42);
        unreadable.close();
        check(Files.readAllBytes(sentinel)[0] == 7, "failed read leaves contents alone");
    }

    private static void food() {
        FeastFoodCounter counter = new FeastFoodCounter();
        check(!counter.update(10), "initial food snapshot is baseline");
        check(!counter.update(10), "duplicate refresh excluded");
        check(counter.update(12), "food increase counted");
        check(!counter.update(0), "stat gain reset excluded");
        check(counter.update(3), "food after reset counted");
        check(!counter.update(Double.NaN), "invalid meter excluded");
        check(!counter.update(5), "new valid baseline after invalid data");
    }

    private static void updater() throws Exception {
        final int[] requests = {0};
        final boolean[] fail = {false};
        URL.setURLStreamHandlerFactory(protocol -> !protocol.equals("fixture") ? null : new URLStreamHandler() {
            protected URLConnection openConnection(URL url) {
                return new URLConnection(url) {
                    public void connect() {}
                    public InputStream getInputStream() throws IOException {
                        requests[0]++;
                        check(getConnectTimeout() == 5000 && getReadTimeout() == 15000, "updater timeouts");
                        if(fail[0]) throw new SocketTimeoutException("synthetic timeout");
                        String json = "{\"version\":\"" + Config.version + "\",\"url\":\"client.zip\"}";
                        return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                    }
                };
            }
        });
        ClientUpdater.manifestUri.set(URI.create("fixture://release/update.json"));
        ClientUpdater.checkStartup();
        check(requests[0] == 1, "startup check completes synchronously");
        ClientUpdater.checkStartup();
        check(requests[0] == 1, "startup check runs once");
        ClientUpdater.UpdateInfo info = ClientUpdater.check();
        check(!info.newer() && info.url.equals(URI.create("fixture://release/client.zip")), "current manifest resolved");
        Field checked = ClientUpdater.class.getDeclaredField("startupChecked");
        checked.setAccessible(true);
        checked.setBoolean(null, false);
        fail[0] = true;
        ClientUpdater.checkStartup();
        check(requests[0] == 3, "unavailable update server returns to startup");
    }

    private static void retries() throws Exception {
        ByteArrayOutputStream warnings = new ByteArrayOutputStream();
        PrintStream original = System.err;
        int[] tries = {0};
        try {
            System.setErr(new PrintStream(warnings));
            String value = Utils.ioretry(() -> {
                if(tries[0]++ == 0) throw new IOException("transient fixture");
                return "ok";
            });
            check(value.equals("ok") && tries[0] == 2, "transient failure recovers");
            check(warnings.size() == 0, "recovered I/O has no warning");
            tries[0] = 0;
            try {
                Utils.ioretry(() -> {
                    // Keep this test fast and verify interrupted status is restored.
                    Thread.currentThread().interrupt();
                    throw new IOException("persistent fixture " + (++tries[0]));
                });
                throw new AssertionError("Expected failure");
            } catch(IOException expected) {
                check(tries[0] == 6, "exhaust all retries");
                check(expected.getSuppressed().length == 1, "prior failure retained");
                check(Thread.interrupted(), "interrupt restored");
                check(warnings.toString("UTF-8").contains("I/O error after retries"), "terminal error reported");
            }
        } finally {
            System.setErr(original);
            Thread.interrupted();
        }
    }
}
