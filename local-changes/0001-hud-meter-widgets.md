# 0001 - Movable Custom HUD Meter Widgets

## Summary

Adds movable/resizable HUD controls for health, stamina, energy, speed, and quest objective UI.

The health/stamina/energy meters keep the original server-backed `IMeter` widgets alive for data, hide their original drawing, and render our own custom bars from the same meter values/tooltips.

## User-Facing Behavior

- HP, stamina, and energy can be moved and resized when movable HUD widgets are unlocked.
- Speed selector can be moved.
- Quest objective UI is wrapped in its own movable box.
- HP displays current health, soft health limit, and hard health limit.
- Energy displays raw values, with 10000 as the normal maximum, and supports values above 100%.
- Meter borders follow the UI theme:
  - `Theme.Pretty` uses `Window.wbox`.
  - `Theme.Small` uses the slim HUD window box assets.
- The old temporary GUI debug commands and visible resize-corner marks were removed.

## Files Added

- `src/haven/MeterWidgetBox.java`
- `src/haven/MovableWidgetBox.java`
- `src/haven/ResizableDraggableWidget.java`

## Files Modified

- `src/haven/CFG.java`
- `src/haven/DraggableWidget.java`
- `src/haven/GameUI.java`
- `src/me/ender/CustomOptPanels.java`
- `build.xml`

## Files Removed

- `src/me/ender/StatMeterWdg.java`

## Important Code Anchors

- `GameUI.addcmeter`
- `GameUI.addchild`
- `GameUI.cdestroy`
- `GameUI.meterbox`
- `GameUI.metername`
- `CFG.SHOW_FLOATING_STAT_WDGS`
- `CFG.LOCK_FLOATING_STAT_WDGS`
- `DraggableWidget.setCfg`
- `DraggableWidget.initCfg`
- `MeterWidgetBox.draw`
- `MeterWidgetBox.frame`
- `MeterWidgetBox.hptext`
- `MeterWidgetBox.energytext`

## Verification Checklist

1. Toggle `Show draggable HP/Stamina/Energy bars` on in options.
2. Toggle `Lock movable HUD widgets` off.
3. Drag HP, stamina, energy, speed, and quest UI.
4. Resize HP, stamina, and energy.
5. Confirm HP label shows `current/soft/hard`.
6. Confirm energy label shows raw `current/max`, not just percent.
7. Switch UI theme between `Pretty` and `Small`; meter borders should change with the theme.
8. Confirm no yellow/white resize dots remain visible during normal gameplay.
9. Confirm `:guidbg` and `:meterdbg` are not available in the console.

## Build Notes

Normal Ant builds may hit a JDK/Windows `AccessDeniedException` while javac closes jar files, especially under sandboxed execution. The workaround used during this change was:

1. Compile changed source files against a temporary jar made from `build/classes` and `build/classes-lib`.
2. Rebuild `build/hafen.jar` from `build/classes` and `build/classes-lib`.
3. Run the supported `bin` layout through `run-kami-bin.bat`; avoid maintaining a separate packaged `dist` copy for local testing.

The `build.xml` change adds `build/classes` to the main javac classpath so incremental compiles can resolve already-built project classes when the environment allows javac to read them normally.

## Merge Notes

The most likely conflict point is `GameUI.addchild`, because upstream UI widgets are created there. If upstream changes how `IMeter`, `Speedget`, or quest widgets are added, reapply the wrapping logic carefully instead of replacing the whole method.

`MeterWidgetBox` depends on tooltip parsing for exact HP and energy numbers, so if upstream changes meter tooltips, update `HP_TIP` and `NUMBERS` parsing.
