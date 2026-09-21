package haven;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public class AlarmManager {

	private static LinkedHashMap<String, Alarm> alarms = new LinkedHashMap<String, Alarm>();
	private static LinkedHashMap<String, String> soundFolders = new LinkedHashMap<String, String>();

	public static void init() {
		installDefaults();
		load();
	}

	// Play an alarm for gob with resname, if it has one
	public static boolean play(String resname, Gob gob) {
		Alarm al = alarms.get(resname);
		if (al != null && al.enabled && !gob.anyOf(GobTag.KO, GobTag.DEAD)) {
			al.play(gob.glob.sess.ui);
			return true;
		}
		return false;
	}

	public static synchronized boolean has(String resname) {
		return resname != null && alarms.containsKey(resname);
	}

	public static synchronized boolean enabled(String resname, boolean fallback) {
		Alarm alarm = alarms.get(resname);
		return alarm == null ? fallback : alarm.enabled;
	}

	public static synchronized String sound(String resname) {
		Alarm alarm = alarms.get(resname);
		return alarm == null ? null : alarm.filePath;
	}

	public static synchronized int volume(String resname, int fallback) {
		Alarm alarm = alarms.get(resname);
		return alarm == null ? fallback : alarm.volume;
	}

	public static synchronized void create(String resname, String name, String sound, int volume) {
		if(resname == null || resname.isEmpty() || alarms.containsKey(resname))
			return;
		alarms.put(resname, new Alarm(true, name == null ? "" : name, normalizeSound(sound), volume));
		save();
	}

	public static synchronized void ensure(String resname, String name) {
		if(!has(resname))
			create(resname, name, "res:sfx/hud/mmap/bell1", 50);
	}

	public static synchronized void setEnabled(String resname, boolean enabled) {
		Alarm alarm = alarms.get(resname);
		if(alarm != null && alarm.enabled != enabled) {
			alarm.enabled = enabled;
			save();
		}
	}

	public static synchronized void setSound(String resname, String filename) {
		Alarm alarm = alarms.get(resname);
		if(alarm != null && filename != null && !filename.isEmpty()) {
			alarm.filePath = normalizeSound(filename);
			save();
		}
	}

	public static synchronized void setVolume(String resname, int volume) {
		Alarm alarm = alarms.get(resname);
		if(alarm != null && alarm.volume != volume) {
			alarm.volume = volume;
			save();
		}
	}

	public static String normalizeSound(String sound) {
		if(sound == null || sound.isEmpty() || sound.startsWith("res:"))
			return sound;
		return sound.endsWith(".wav") ? sound : sound + ".wav";
	}

	public static void preview(String sound, int volume, UI ui) {
		if(sound == null || sound.isEmpty() || ui == null)
			return;
		if(sound.startsWith("res:")) {
			String name = sound.substring(4);
			Indir<Resource> resid = Resource.local().load(name);
			ui.sess.glob.loader.defer(() -> {
				try {
					ui.sfx(new Audio.VolAdjust(Audio.fromres(resid.get()), volume / 50.0));
				} catch(RuntimeException e) {
					ui.error("Could not play " + name);
				}
			}, null);
			return;
		}
		File file = new File(alarmDir(), normalizeSound(sound));
		if(!file.exists()) {
			ui.error("Could not play " + file.getAbsolutePath());
			return;
		}
		try {
			AudioInputStream in = AudioSystem.getAudioInputStream(file);
			AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
			AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
			ui.sfx(new Audio.VolAdjust(new Audio.PCMClip(pcmStream, 2, 2), volume / 50.0));
		} catch(UnsupportedAudioFileException | IOException e) {
			new Warning(e, "could not play alarm " + file).issue();
		}
	}

	public static synchronized String resourceForIcon(String iconResource, String iconName) {
		if(iconResource == null)
			return null;
		String byName = uniqueResourceMatch(normalizeName(iconName), true);
		if(byName != null)
			return byName;
		if(alarms.containsKey(iconResource))
			return iconResource;
		for(String resource : alarms.keySet()) {
			if(iconResource.equals(Radar.iconForGob(resource)))
				return resource;
		}
		String byBasename = uniqueResourceMatch(basename(iconResource), false);
		if(byBasename != null)
			return byBasename;
		return null;
	}

	private static String uniqueResourceMatch(String value, boolean alarmName) {
		if(value == null || value.isEmpty())
			return null;
		String match = null;
		for(Map.Entry<String, Alarm> entry : alarms.entrySet()) {
			String candidate = alarmName ? normalizeName(entry.getValue().alarmName) : basename(entry.getKey());
			boolean matches = alarmName ? (candidate.equals(value) || candidate.startsWith(value + " ") || value.startsWith(candidate + " ")) : candidate.equals(value);
			if(matches) {
				if(match != null)
					return null;
				match = entry.getKey();
			}
		}
		return match;
	}

	private static String basename(String resource) {
		if(resource == null)
			return null;
		int split = resource.lastIndexOf('/');
		return normalizeName(split < 0 ? resource : resource.substring(split + 1));
	}

	private static String normalizeName(String name) {
		return name == null ? null : name.toLowerCase().replaceAll("[^a-z0-9]+", " ").trim();
	}

	public static synchronized String soundFolder(String resname) {
		String folder = soundFolders.get(resname);
		if(folder != null && !folder.isEmpty())
			return folder;
		Alarm alarm = alarms.get(resname);
		return safeFolderName(alarm == null ? basename(resname) : alarm.alarmName);
	}

	private static String safeFolderName(String name) {
		if(name == null)
			return "Other";
		String safe = name.replaceAll("[\\\\/:*?\"<>|]+", " ").replaceAll("\\s+", " ").trim();
		return safe.isEmpty() ? "Other" : safe;
	}

	public static List<String> soundFiles(String folder) {
		List<String> sounds = new ArrayList<>();
		if(folder == null || folder.isEmpty())
			return sounds;
		File dir = new File(alarmDir(), folder);
		File[] files = dir.listFiles((parent, name) -> name.toLowerCase().endsWith(".wav"));
		if(files != null) {
			for(File file : files)
				sounds.add(folder.replace(File.separatorChar, '/') + "/" + file.getName());
		}
		Collections.sort(sounds, String.CASE_INSENSITIVE_ORDER);
		return sounds;
	}

	public static List<SoundOption> defaultSounds() {
		List<SoundOption> sounds = new ArrayList<>();
		try {
			for(String line : Files.readAllLines(defaultSoundsFile().toPath(), StandardCharsets.UTF_8)) {
				String trimmed = line.trim();
				if(trimmed.isEmpty() || trimmed.startsWith("#"))
					continue;
				String[] split = trimmed.split(";", 2);
				if(split.length == 2 && !split[0].trim().isEmpty() && !split[1].trim().isEmpty())
					sounds.add(new SoundOption(split[0].trim(), normalizeSound(split[1].trim())));
			}
		} catch(IOException e) {
			new Warning(e, "could not load default alarm sounds").issue();
		}
		return sounds;
	}

	// Load settings from file or use defaults if file does not exist
	public static void load() {
		alarms.clear();
		soundFolders.clear();
		File config = configFile();
		if(config.exists())
			loadFromFile(config);
		loadFromFile(defaultConfigFile());
		loadSoundFolders(defaultConfigFile());
	}

	// Load config from the given file
	private static void loadFromFile(File config) {
		try {
			for(String s : Files.readAllLines(Paths.get(config.toURI()), StandardCharsets.UTF_8)) {
				String[] split = s.split(";");
				if(split.length >= 5 && !alarms.containsKey(split[0]))
					alarms.put(split[0], new Alarm(Boolean.parseBoolean(split[1]), split[2], split[3], Integer.parseInt(split[4])));
			}
		} catch(IOException | NumberFormatException e) {
			e.printStackTrace();
		}
	}

	private static void loadSoundFolders(File config) {
		try {
			for(String s : Files.readAllLines(config.toPath(), StandardCharsets.UTF_8)) {
				String[] split = s.split(";");
				if(split.length < 4)
					continue;
				String sound = split[3].replace('\\', '/');
				int slash = sound.lastIndexOf('/');
				if(slash > 0)
					soundFolders.put(split[0], sound.substring(0, slash));
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	// Save current settings to file
	public static void save() {
		try {
			Files.createDirectories(configFile().toPath().getParent());
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(configFile().toURI()), StandardCharsets.UTF_8);
			for(Map.Entry<String, Alarm> e : alarms.entrySet()) {
				bw.write(e.getKey() + ";" + e.getValue().enabled + ";" + e.getValue().alarmName + ";" + e.getValue().filePath.replace(".wav", "") + ";" + e.getValue().volume + ";false\n");
			}
			bw.flush();
			bw.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	// Loads the default settings
	public static void defaultSettings() {
		alarms.clear();
		soundFolders.clear();
		loadFromFile(defaultConfigFile());
		loadSoundFolders(defaultConfigFile());
	}

	public static File alarmDir() {
		return Config.getFile("AlarmSounds");
	}

	private static File configFile() {
		return new File(alarmDir(), "settings/yourSavedConfig");
	}

	private static File defaultConfigFile() {
		return new File(alarmDir(), "settings/defaultAlarms");
	}

	private static File defaultSoundsFile() {
		return new File(alarmDir(), "settings/defaultSounds");
	}

	private static void installDefaults() {
		try {
			File dir = alarmDir();
			Files.createDirectories(dir.toPath());
			Files.createDirectories(new File(dir, "settings").toPath());
			Path bundled = bundledAlarmDir();
			if(bundled != null)
				copyMissing(bundled, dir.toPath());
		} catch(IOException e) {
			e.printStackTrace(Debug.log);
		}
	}

	private static Path bundledAlarmDir() throws IOException {
		Path src = Utils.srcpath(AlarmManager.class);
		if(src != null) {
			Path dir = src.getParent();
			if(dir != null && dir.getFileName().toString().equalsIgnoreCase("bin"))
				dir = dir.getParent();
			if(dir != null) {
				Path candidate = dir.resolve("AlarmSounds");
				if(Files.isDirectory(candidate))
					return candidate;
			}
		}
		Path candidate = Utils.path("AlarmSounds");
		return Files.isDirectory(candidate) ? candidate : null;
	}

	private static void copyMissing(Path src, Path dst) throws IOException {
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(src)) {
			for(Path child : stream) {
				Path target = dst.resolve(child.getFileName().toString());
				if(Files.isDirectory(child)) {
					Files.createDirectories(target);
					copyMissing(child, target);
				} else if(!Files.exists(target)) {
					try(InputStream in = Files.newInputStream(child);
					    OutputStream out = Files.newOutputStream(target)) {
						byte[] buf = new byte[1024 * 64];
						int n;
						while((n = in.read(buf)) >= 0)
							out.write(buf, 0, n);
					}
				}
			}
		}
	}

	public static class Alarm {
		public String filePath;
		public int volume;
		public boolean enabled;
		public String alarmName;

		public Alarm(boolean enabled, String alarmName, String filePath, int volume) {
			this.enabled = enabled;
			this.filePath = filePath;
			this.volume = volume;
			this.alarmName = alarmName;
		}

		public void play(UI ui) {
			preview(filePath, volume, ui);
		}
	}

	public static class SoundOption {
		public final String name;
		public final String value;

		public SoundOption(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}
}
