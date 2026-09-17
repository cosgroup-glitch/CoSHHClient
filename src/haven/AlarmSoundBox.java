package haven;

import haven.iosys.tk.FilePicker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AlarmSoundBox extends SDropBox<AlarmSoundBox.SoundChoice, Widget> {
    private static final SoundChoice SELECT_FILE = new SoundChoice("Select file...", null);
    private final List<SoundChoice> choices = new ArrayList<>();
    private final Consumer<String> selected;

    static class SoundChoice {
	final String name;
	final String value;

	SoundChoice(String name, String value) {
	    this.name = name;
	    this.value = value;
	}
    }

    public AlarmSoundBox(int width, String initial, Consumer<String> selected) {
	super(width, UI.scale(180), UI.scale(20));
	this.selected = selected;
	for(GobIcon.NotificationSetting sound : GobIcon.NotificationSetting.builtin)
	    choices.add(new SoundChoice(sound.name, "res:" + sound.res));
	addFileChoice(initial);
	choices.add(SELECT_FILE);
	select(initial);
    }

    private void addFileChoice(String sound) {
	if(sound == null || sound.isEmpty() || sound.startsWith("res:"))
	    return;
	String value = AlarmManager.normalizeSound(sound);
	if(choices.stream().noneMatch(choice -> value.equals(choice.value)))
	    choices.add(new SoundChoice(new File(value).getName(), value));
    }

    public String value() {
	return sel == null ? null : sel.value;
    }

    public void select(String value) {
	String normalized = AlarmManager.normalizeSound(value);
	addFileChoice(normalized);
	for(SoundChoice choice : choices) {
	    if(Utils.eq(choice.value, normalized)) {
		super.change(choice);
		return;
	    }
	}
    }

    protected List<SoundChoice> items() {return choices;}
    protected Widget makeitem(SoundChoice item, int idx, Coord sz) {
	return SListWidget.TextItem.of(sz, Text.std, () -> item.name);
    }

    public void change(SoundChoice choice) {
	SoundChoice previous = sel;
	super.change(choice);
	if(choice == SELECT_FILE) {
	    FilePicker dialog = ui.wnd.toolkit().picker().make(FilePicker.Mode.OPEN, ui.wnd);
	    dialog.filter("PCM wave file", "wav");
	    dialog.show().map(path -> {
		if(path == null) {
		    super.change(previous);
		} else {
		    try {
			Path target = AlarmManager.alarmDir().toPath().resolve(path.getFileName().toString());
			Files.createDirectories(target.getParent());
			if(!path.toAbsolutePath().normalize().equals(target.toAbsolutePath().normalize()))
			    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
			String value = target.getFileName().toString();
			addFileChoice(value);
			select(value);
			selected.accept(value);
		    } catch(IOException e) {
			ui.error("Could not copy alarm sound: " + e.getMessage());
			super.change(previous);
		    }
		}
	    }).report(ui);
	} else {
	    selected.accept(choice.value);
	}
    }
}
