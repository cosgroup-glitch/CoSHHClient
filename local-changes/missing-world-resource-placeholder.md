# Purple placeholder for failed world resources

When the server introduces a world object and its resource lookup ends with
`Resource.LoadFailedException` (including `NoSuchResourceException`),
`ResDrawable` now installs a built-in purple box instead of allowing that failure
to terminate object-update processing. This handles the reported DNS failure
for `gfx/terobjs/bumlings/leadglance2`.

The box uses shared geometry and two purple shades, with no downloaded image,
texture or model. It inherits the object's transform and click identity. Hover
text identifies the unavailable resource and version. The original exception
is retained on the drawable and reported in the log, with a bounded set of 128
recent resource/version names to limit repeated warnings.

The replacement exposes an empty virtual resource named
`local/missing-world-object` so metadata consumers do not query the failed
resource again or classify it as a fully loaded tree, animal, or cupboard.
Normal asynchronous `Loading` still propagates to the existing loader.
Unrelated exceptions are not swallowed. Object-state updates retain the marker;
a later replacement with a usable resource creates the normal drawable.

This is limited to the initial world-object resource lookup. It does not replace
failed terrain, inventory images, overlays, sprite dependencies, or failures
occurring later during sprite execution. It does not add automatic network
retries; a terminally failed resource may require restarting the client after
connectivity recovers. The generic box does not represent the original object's
dimensions, collision boundary, or special mesh click targets.

## Cache safety

The fallback only handles the terminal exception after the existing resource
lookup. It never deletes, overwrites, repairs, retries or re-indexes any cache
entry. No resource-loader, cache-provider, eviction or cache-recovery code was
changed. The placeholder is memory-only and cannot be stored under the failed
resource's name. State updates never re-query that failed reference.

## Validation

Run `python tools/run-drawable-fallback-tests.py`. The test compiles the actual
`ResDrawable.java` against isolated collaborators and executes the server-delta
handler, so it never initializes the game or accesses network/cache data.
Seventeen checks cover normal loading, failed and absent resources, safe
metadata, original diagnostics, state updates without repeated lookup, lifecycle
calls, later drawable replacement, and propagation of loading/programming errors.
The sprite/render collaborators are test doubles; this is not a GPU rendering
test. The production sprite and tooltip compile in the complete client build.

`ant jar` and the final `ant bin` packaging completed successfully, with archive
integrity checks passing. The packaged `bin/hafen.jar` matches `build/hafen.jar`
and contains the fallback and integrity-check classes. The existing 18
archive/finalizer regression checks also passed (35 checks in total).

At the user's request, the placeholder build was staged separately and launched
using the established test cache, settings and preferences. Process 9728 stayed
alive through the startup check with no matched startup exceptions. No cache was
cleared, and no previous staged JAR was overwritten. That process was no longer
running when final packaging was checked; no exit cause was established.
The user subsequently confirmed the visual test with a screenshot: cupboards
rendered as purple boxes while surrounding objects rendered normally. Hover,
click and re-entry behavior were not separately confirmed in that message.

The final package build continues to report the separate existing resource
compiler warning `Invalid number of decoded files for code` for `ui/tt/cn.res`.
Ant reports success; this warning has not been fixed by the placeholder change.
The deployable local program files are in the main worktree's `bin` directory.
This work is included in the v0.1.11 bug-fix release preparation.

Merge-sensitive files: `ResDrawable.java` constructor and `getires`,
`Gob.java` tooltip, and the new `MissingResourceSprite.java`.

## Opt-in visual test

Start the client with the JVM argument
`-Dkami.test.missing-world-resource=gfx/terobjs/cupboard` before `-jar` to test
the placeholder on cupboards. The value matches one exact resource name. The
flag is empty by default and never saved to preferences, settings or caches.
After a successful resource lookup, it injects a clearly identified simulated
`LoadFailedException` through the same fallback handler used by real failures.
Other resource names behave normally. Restart without the argument to restore
normal rendering. This does not damage or delete a resource, disable the network,
or force a resource download. Normal in-progress loads are not bypassed.

The regression runner now tests two fresh JVMs: 18 checks with the flag absent
and 21 with it targeting cupboards. Both passed, followed by `ant jar` and the
archive-integrity check. The visual test uses a separate staged program build,
with `launch-purple-cupboards.ps1` and `launch-normal.ps1`, and reuses the existing
test cache/settings. Enter a house with cupboards; check the purple box, hover
text, object clicks, and walking away/returning. The user confirmed placeholder
appearance in the supplied screenshot. Normal release launchers do not set this
diagnostic property.
