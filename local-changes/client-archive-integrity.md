# Client archive corruption and finalizer interruption

## Report and evidence (2026-09-30)

The reported item-widget crash was `ClassFormatError: Illegal exception table
range in class file haven/GItem$ContentsWindow`. Read-only inspection of the
Desktop `KamisLabyrinthClient/bin/hafen.jar` found one damaged entry out of
4,893: that exact class failed its ZIP CRC check. The archive SHA-256 was
`2e26d46efd39e62ff89377f07ba7d58dd9446a143ea1ee4481000cdd58439b88`.

Inspection of the damaged bytes found an exception-table range of 3..257 in
`chstate(String)`, whose code length was only 170 bytes. The fresh main build
has zero exception handlers in that method. This establishes corrupt program
bytes in that installation; it does not establish how they were corrupted or
that the game cache was involved. The user has not yet confirmed which launcher
produced the report. The installed archive was left unchanged as evidence.

## Changes

- `ClientIntegrity` checks all entries in the main program JAR for CRC/size
  mismatches, unreadable entries, duplicate names and a missing client entry.
- Startup performs this read-only check once, before `main2` initializes game
  resources. A failure stops startup with the archive path and an instruction
  to reinstall program files, keeping the game cache. Development launches
  from class directories and custom loaders without a local archive skip it.
- Ant's `jar` target runs the same check and fails packaging on a bad archive.
- `Finalizer` treats interruption of its queue wait as a wake-up. It continues
  processing pending cleaners; actual cleaner failures still produce warnings.
- The `BollData` v3/v4 warning is unchanged. As described in `doc/resource-code`,
  newer server resource code can override the older bundled implementation.

CRC validation detects accidental corruption; it is not a signature check or a
Java bytecode verifier. It checks `hafen.jar`, not every dependency or resource
JAR, and cannot prevent modifications made after the check. The check never
repairs or replaces any file and never accesses game caches.

## Verification

From the repository root, with Python and a JDK available:

```text
python tools/run-client-reliability-tests.py
ant bin
java -cp bin/hafen.jar haven.ClientIntegrity bin/hafen.jar
```

The standalone tests compile the production integrity checker and finalizer
against small test-only collaborators so no game, config or cache initialization
occurs. Fixtures remain under `build/client-reliability-tests`. Eighteen checks
cover valid stored/deflated entries, corruption, truncation, empty/missing
archives, read-only failure handling, interrupted cleanup, exactly-once cleanup,
and continued processing/reporting after a failing cleaner.

The new verifier rejected the actual damaged Desktop JAR with the expected
class name. A fresh main-based `ant bin` completed successfully; an independent
read-only parser verified all 4,734 class structures and exception-table ranges
in its output. No game session was launched. The resource compiler also printed
`Invalid number of decoded files for code` for `ui/tt/cn.res`; this existing
packaging path is outside the archive/finalizer change and needs separate review
before treating this as a fully validated release. The fresh `hafen.jar` passed
all archive checks.

## Branch and recovery

This change is developed on `fix/classfile-validation` from the main branch.
Inventory development remains on `test`. The built replacement program archive
is `bin/hafen.jar` in the main worktree. Replacing the damaged installed archive
is still a separate deployment step: close client instances using that archive,
retain the damaged program JAR as evidence, then install a verified build.
Do not clear or replace game caches to address this program-file failure.
