package haven;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Read-only program archive checks. Never accesses game resources or caches. */
public final class ClientIntegrity {
    private ClientIntegrity() {}

    public static void verifyOwnArchive() {
        CodeSource source = ClientIntegrity.class.getProtectionDomain().getCodeSource();
        if(source == null)
            return; // A custom class loader may not expose a program archive.
        try {
            if(!"file".equals(source.getLocation().getProtocol()))
                return;
            Path archive = Paths.get(source.getLocation().toURI());
            if(Files.isDirectory(archive))
                return; // Development launch from compiled classes.
            verify(archive);
        } catch(IOException | URISyntaxException e) {
            throw new IllegalStateException("Client program files are damaged or unreadable. " +
                    "Close the client and reinstall its program files from a trusted release. " +
                    "Do not clear the game cache. " + e.getMessage(), e);
        }
    }

    public static void verify(Path archive) throws IOException {
        try(ZipFile zip = new ZipFile(archive.toFile())) {
            byte[] buffer = new byte[32768];
            Set<String> names = new HashSet<>();
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while(entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if(!names.add(entry.getName()))
                    throw new IOException("Duplicate entry: " + entry.getName());
                CRC32 crc = new CRC32();
                long size = 0;
                try(InputStream in = zip.getInputStream(entry)) {
                    for(int n; (n = in.read(buffer)) != -1;) {
                        crc.update(buffer, 0, n);
                        size += n;
                    }
                } catch(IOException e) {
                    throw new IOException("Cannot read entry: " + entry.getName(), e);
                }
                // ZipFile streams do not reliably enforce CRC checks themselves.
                if((size != entry.getSize()) || (crc.getValue() != entry.getCrc()))
                    throw new IOException("Checksum or size mismatch: " + entry.getName());
            }
            if(!names.contains("haven/Client.class"))
                throw new IOException("Missing haven/Client.class");
        } catch(IOException e) {
            throw new IOException(archive.toAbsolutePath() + ": " + e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws IOException {
        if(args.length != 1)
            throw new IllegalArgumentException("Usage: haven.ClientIntegrity <hafen.jar>");
        verify(Paths.get(args[0]));
        System.out.println("Client archive integrity verified: " + args[0]);
    }
}
