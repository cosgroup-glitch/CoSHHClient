package haven;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

public class ClientUpdater {
    private static final Gson gson = new Gson();
    public static final Config.Variable<URI> manifestUri = Config.Variable.propu("kami.update.manifest", "");
    private static boolean startupChecked = false;

    public static class UpdateInfo {
	public final String version;
	public final URI url;
	public final String notes;
	public final String sha256;

	private UpdateInfo(String version, URI url, String notes, String sha256) {
	    this.version = version;
	    this.url = url;
	    this.notes = notes;
	    this.sha256 = sha256;
	}

	public boolean newer() {
	    return (version != null) && !version.equals(Config.version);
	}
    }

    public static boolean configured() {
	return manifestUri.get() != null;
    }

    public static UpdateInfo check() throws IOException {
	URI manifest = manifestUri.get();
	if(manifest == null)
	    throw(new IOException("No update manifest configured."));
	String json;
	try(InputStream in = fetchExternal(manifest.toURL())) {
	    json = read(in);
	}
	JsonObject obj = gson.fromJson(json, JsonObject.class);
	if(obj == null)
	    throw(new IOException("Update manifest is empty."));
	String version = str(obj, "version");
	String url = str(obj, "url");
	if((version == null) || (url == null))
	    throw(new IOException("Update manifest must contain version and url."));
	URI download = manifest.resolve(url);
	return new UpdateInfo(version, download, str(obj, "notes"), str(obj, "sha256"));
    }

    public static void install(UpdateInfo update) throws IOException {
	Path tmp = Files.createTempDirectory("kami-update-");
	Path zip = tmp.resolve("KamisLabyrinthClient-" + sanitize(update.version) + ".zip");
	download(update.url.toURL(), zip);
	if((update.sha256 != null) && !update.sha256.equalsIgnoreCase(sha256(zip)))
	    throw(new IOException("Downloaded update failed checksum verification."));
	Path script = tmp.resolve("install-kami-update.ps1");
	writeInstaller(script, zip, appdir(), tmp.resolve("update.log"));
	new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
			   "-File", script.toString(),
			   Long.toString(currentPid()),
			   zip.toString(),
			   appdir().toString(),
			   tmp.resolve("update.log").toString())
	    .start();
	System.exit(0);
    }

    public static synchronized void checkStartup() {
	if(startupChecked || !configured())
	    return;
	startupChecked = true;
	new HackThread(() -> {
	    try {
		UpdateInfo update = check();
		if(!update.newer())
		    return;
		install(update);
	    } catch(Exception e) {
		e.printStackTrace(Debug.log);
	    }
	}, "kami's labyrinth Client update check").start();
    }

    private static Path appdir() throws IOException {
	Path jar = Utils.srcpath(ClientUpdater.class);
	Path dir = jar.getParent();
	if(dir == null)
	    throw(new IOException("Could not find client directory."));
	if(dir.getFileName().toString().equalsIgnoreCase("bin"))
	    dir = dir.getParent();
	if(dir == null)
	    throw(new IOException("Could not find client directory."));
	return dir.toAbsolutePath();
    }

    private static void download(URL url, Path dest) throws IOException {
	URLConnection conn = openExternal(url);
	try(InputStream in = conn.getInputStream();
	    OutputStream out = Files.newOutputStream(dest)) {
	    byte[] buf = new byte[1024 * 128];
	    int n;
	    while((n = in.read(buf)) >= 0)
		out.write(buf, 0, n);
	}
    }

    private static URLConnection openExternal(URL url) throws IOException {
	URLConnection conn = url.openConnection();
	conn.addRequestProperty("User-Agent", Http.USER_AGENT);
	return(conn);
    }

    private static InputStream fetchExternal(URL url) throws IOException {
	URLConnection conn = openExternal(url);
	return(conn.getInputStream());
    }

    private static void writeInstaller(Path script, Path zip, Path appdir, Path log) throws IOException {
	try(BufferedWriter out = Files.newBufferedWriter(script, StandardCharsets.UTF_8)) {
	    out.write("param([int]$PidToWait, [string]$ZipPath, [string]$AppDir, [string]$LogPath)\r\n");
	    out.write("$ErrorActionPreference = 'Stop'\r\n");
	    out.write("Start-Transcript -Path $LogPath -Append | Out-Null\r\n");
	    out.write("try {\r\n");
	    out.write("  if ($PidToWait -gt 0) { Wait-Process -Id $PidToWait -ErrorAction SilentlyContinue }\r\n");
	    out.write("  Start-Sleep -Seconds 1\r\n");
	    out.write("  $Stage = Join-Path ([System.IO.Path]::GetTempPath()) ('kami-update-stage-' + [guid]::NewGuid())\r\n");
	    out.write("  New-Item -ItemType Directory -Path $Stage | Out-Null\r\n");
	    out.write("  Expand-Archive -Path $ZipPath -DestinationPath $Stage -Force\r\n");
	    out.write("  $Source = $Stage\r\n");
	    out.write("  $Dirs = @(Get-ChildItem -LiteralPath $Stage -Directory)\r\n");
	    out.write("  if (($Dirs.Count -eq 1) -and (Test-Path -LiteralPath (Join-Path $Dirs[0].FullName 'bin'))) { $Source = $Dirs[0].FullName }\r\n");
	    out.write("  Copy-Item -Path (Join-Path $Source '*') -Destination $AppDir -Recurse -Force\r\n");
	    out.write("  $Run = Join-Path $AppDir 'run-kami-bin.bat'\r\n");
	    out.write("  if (Test-Path -LiteralPath $Run) { Start-Process -FilePath $Run -WorkingDirectory $AppDir }\r\n");
	    out.write("} finally {\r\n");
	    out.write("  Stop-Transcript | Out-Null\r\n");
	    out.write("}\r\n");
	}
    }

    private static String read(InputStream in) throws IOException {
	byte[] buf = new byte[1024 * 16];
	StringBuilder ret = new StringBuilder();
	int n;
	while((n = in.read(buf)) >= 0)
	    ret.append(new String(buf, 0, n, StandardCharsets.UTF_8));
	return ret.toString();
    }

    private static String str(JsonObject obj, String name) {
	return obj.has(name) && !obj.get(name).isJsonNull() ? obj.get(name).getAsString() : null;
    }

    private static String sanitize(String text) {
	return text.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static long currentPid() {
	String name = ManagementFactory.getRuntimeMXBean().getName();
	int p = name.indexOf('@');
	if(p >= 0)
	    name = name.substring(0, p);
	try {
	    return Long.parseLong(name);
	} catch(NumberFormatException e) {
	    return 0;
	}
    }

    private static String sha256(Path file) throws IOException {
	try {
	    MessageDigest md = MessageDigest.getInstance("SHA-256");
	    try(InputStream in = Files.newInputStream(file)) {
		byte[] buf = new byte[1024 * 128];
		int n;
		while((n = in.read(buf)) >= 0)
		    md.update(buf, 0, n);
	    }
	    StringBuilder ret = new StringBuilder();
	    for(byte b : md.digest())
		ret.append(String.format("%02x", b & 0xff));
	    return ret.toString();
	} catch(java.security.NoSuchAlgorithmException e) {
	    throw(new IOException(e));
	}
    }
}
