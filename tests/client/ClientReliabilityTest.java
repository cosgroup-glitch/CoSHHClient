package haven;

import java.io.IOException;
import java.lang.ref.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ClientReliabilityTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if(!value) throw new AssertionError(message);
    }

    private static void rejected(Path archive, String expected) throws Exception {
        byte[] before = Files.readAllBytes(archive);
        try {
            ClientIntegrity.verify(archive);
            throw new AssertionError("Accepted damaged archive: " + archive);
        } catch(IOException e) {
            check(e.getMessage().contains(expected), "Helpful diagnostic: " + e);
        }
        check(Arrays.equals(before, Files.readAllBytes(archive)), "Verification changed archive");
    }

    private static void archiveTests(Path dir) throws Exception {
        Path archive = dir.resolve("valid.jar");
        byte[] payload = "unique-test-class-payload".getBytes("UTF-8");
        try(ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(archive))) {
            ZipEntry entry = new ZipEntry("haven/Client.class");
            CRC32 crc = new CRC32();
            crc.update(payload);
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(payload.length);
            entry.setCrc(crc.getValue());
            out.putNextEntry(entry);
            out.write(payload);
            out.closeEntry();
            out.putNextEntry(new ZipEntry("deflated-resource"));
            out.write(new byte[100000]);
            out.closeEntry();
        }
        byte[] original = Files.readAllBytes(archive);
        ClientIntegrity.verify(archive);
        check(Arrays.equals(original, Files.readAllBytes(archive)), "Valid archive remains unchanged");
        byte[] corrupt = original.clone();
        int offset = 30 + "haven/Client.class".length();
        check(corrupt[offset] == payload[0], "Fixture payload offset");
        corrupt[offset] ^= 1;
        Path damaged = dir.resolve("corrupt.jar");
        Files.write(damaged, corrupt);
        rejected(damaged, "haven/Client.class");
        Path truncated = dir.resolve("truncated.jar");
        Files.write(truncated, Arrays.copyOf(original, original.length / 2));
        rejected(truncated, "truncated.jar");
        Path empty = dir.resolve("empty.jar");
        try(ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(empty))) {}
        rejected(empty, "Missing haven/Client.class");
        Path missing = dir.resolve("missing.jar");
        try {
            ClientIntegrity.verify(missing);
            throw new AssertionError("Accepted missing archive");
        } catch(IOException expected) {
            check(!Files.exists(missing), "Missing archive was not created");
        }
        ClientIntegrity.verifyOwnArchive(); // Compiled-class launches remain supported.
    }

    private static void finalizerTests() throws Exception {
        AtomicReference<Thread> worker = new AtomicReference<>();
        Finalizer finalizer = new Finalizer(task -> {
            Thread thread = new Thread(task, "Finalizer regression test");
            worker.set(thread);
            return thread;
        });
        Object retained = new Object();
        AtomicInteger count = new AtomicInteger();
        CountDownLatch cleaned = new CountDownLatch(1);
        Runnable reference = finalizer.add(retained, () -> { count.incrementAndGet(); cleaned.countDown(); });
        worker.get().interrupt();
        // The interrupt can arrive before or during the blocking queue wait.
        check(Warning.issued.poll(200, TimeUnit.MILLISECONDS) == null, "Interrupt must not issue warning");
        ((Reference<?>)reference).enqueue();
        check(cleaned.await(5, TimeUnit.SECONDS), "Cleanup continues after interrupt");
        worker.get().join(5000);
        check(!worker.get().isAlive(), "Worker exits when references are drained");
        reference.run();
        check(count.get() == 1, "Cleanup runs exactly once");

        IllegalStateException failure = new IllegalStateException("intentional cleaner failure");
        Runnable broken = finalizer.add(retained, () -> { throw failure; });
        CountDownLatch nextCleaned = new CountDownLatch(1);
        Runnable next = finalizer.add(retained, nextCleaned::countDown);
        ((Reference<?>)broken).enqueue();
        check(Warning.issued.poll(5, TimeUnit.SECONDS) == failure, "Real cleaner errors stay visible");
        ((Reference<?>)next).enqueue();
        check(nextCleaned.await(5, TimeUnit.SECONDS), "Cleanup survives a failing cleaner");
        worker.get().join(5000);
        check(!worker.get().isAlive(), "Replacement worker drained");
        check(Warning.issued.isEmpty(), "No spurious warnings");
        check(retained.hashCode() == retained.hashCode(), "Keep referent alive through test");
    }

    public static void main(String[] args) throws Exception {
        archiveTests(Files.createTempDirectory(Paths.get(args[0]), "archives-"));
        finalizerTests();
        System.out.println("PASS: " + checks + " client reliability checks");
    }
}
