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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class AlarmManager {

	private static LinkedHashMap<String, Alarm> alarms = new LinkedHashMap<String, Alarm>();

	public static void init() {
		installDefaults();
		load();
	}

	// Play an alarm for gob with resname, if it has one
	public static boolean play(String resname, Gob gob) {
		Alarm al = alarms.get(resname);
		if (al != null && al.enabled) {
			if (al.knocked || !gob.anyOf(GobTag.KO, GobTag.DEAD)) {
				al.play(gob.glob.sess.ui);
				return true;
			}
		}
		return false;
	}

	// Load settings from file or use defaults if file does not exist
	public static void load() {
		alarms.clear();
		File config = configFile();
		if(!config.exists()) {
			defaultSettings();
		} else {
			loadFromFile(config);
		}
	}

	// Load config from the given file
	private static void loadFromFile(File config) {
		try {
			for(String s : Files.readAllLines(Paths.get(config.toURI()), StandardCharsets.UTF_8)) {
				String[] split = s.split("(;)");
				if(!alarms.containsKey(split[0]))
					alarms.put(split[0], new Alarm(Boolean.parseBoolean(split[1]), split[2], split[3], Integer.parseInt(split[4]), Boolean.parseBoolean(split[5])));
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	// Loads settings from the list
	public static void load(AlarmWindow.AlarmList list) {
		alarms.clear();
		for(AlarmWindow.AlarmItem ai : list.items) {
			alarms.put(ai.getGobResname(), new Alarm(ai.getEnabled(), ai.getAlarmName(), ai.getAlarmFilename(), ai.getVolume(), ai.getKnocked()));
		}
	}

	// Save current settings to file
	public static void save() {
		try {
			Files.createDirectories(configFile().toPath().getParent());
			BufferedWriter bw = Files.newBufferedWriter(Paths.get(configFile().toURI()), StandardCharsets.UTF_8);
			for(Map.Entry<String, Alarm> e : alarms.entrySet()) {
				bw.write(e.getKey() + ";" + e.getValue().enabled + ";" + e.getValue().alarmName + ";" + e.getValue().filePath.replace(".wav", "") + ";" + e.getValue().volume + ";" + e.getValue().knocked+"\n");
			}
			bw.flush();
			bw.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static AlarmWindow.AlarmItem[] getAlarmItems() {
		AlarmWindow.AlarmItem[] alarmItems = new AlarmWindow.AlarmItem[alarms.size()];
		Iterator<Map.Entry<String, Alarm>> it = alarms.entrySet().iterator();
		for(int i=0; i<alarmItems.length; i++) {
			Map.Entry<String, Alarm> e = it.next();
			alarmItems[i] = new AlarmWindow.AlarmItem(e.getKey(), e.getValue().enabled, e.getValue().alarmName, e.getValue().filePath, e.getValue().volume, e.getValue().knocked);
		}
		return alarmItems;
	}

	// Loads the default settings
	public static void defaultSettings() {
		alarms.clear();
		loadFromFile(defaultConfigFile());
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
		public boolean enabled, knocked;
		public String alarmName;

		public Alarm(boolean enabled, String alarmName, String filePath, int volume, boolean knocked) {
			this.enabled = enabled;
			this.filePath = filePath;
			this.volume = volume;
			this.knocked = knocked;
			this.alarmName = alarmName;
		}

		public void play(UI ui) {
			String filePath2 = filePath.endsWith(".wav") ? filePath : filePath + ".wav";
			File file = new File(alarmDir(), filePath2);
			if(!file.exists()) {
				System.out.println("Error while playing an alarm, file " + file.getAbsolutePath() + " does not exist!");
				return;
			}
			try {
				AudioInputStream in = AudioSystem.getAudioInputStream(file);
				AudioFormat tgtFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2,4, 44100, false);
				AudioInputStream pcmStream = AudioSystem.getAudioInputStream(tgtFormat, in);
				Audio.CS klippi = new Audio.PCMClip(pcmStream, 2, 2);
                ui.sfx(new Audio.VolAdjust(klippi, volume/50.0));
			} catch(UnsupportedAudioFileException e) {
				e.printStackTrace();
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
	}
}
