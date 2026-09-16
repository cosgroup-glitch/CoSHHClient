# Local Change Tracking

This folder tracks changes that are ours, separate from the main Kami/client updates.

The goal is simple: when the upstream client updates, we can reapply our local features without relying on memory.

## Preferred Workflow

Use Git as the long-term source of truth:

1. Keep an `upstream` or `main-client` branch that matches the clean client from the other maintainers.
2. Keep our work on a separate branch, for example `kami-local`.
3. Each local feature should be one small commit with a clear name.
4. When the main client updates, update `upstream`, then rebase or cherry-pick `kami-local` commits onto it.
5. If we need portable patch files, run:

```powershell
git format-patch upstream..kami-local -o local-changes/patches
```

Then, on a fresh updated client:

```powershell
git am local-changes/patches/*.patch
```

## Current Repo Note

At the time this file was added, the workspace already had a `.git` folder, but the repository appeared to have the whole tree staged as new files. Before relying on commits, clean that up into a real baseline:

```powershell
git status --short
```

If there is no good upstream history, the practical reset is:

1. Put a clean copy of the main client in a separate folder.
2. Initialize/clone that as the baseline.
3. Copy or apply only our local changes from this folder and the files listed in each change note.

## Build Tool Note

Apache Ant is installed for this workspace, but Codex shell sessions may fail to find it on `PATH` or may report misleading sandbox/path issues. Do not assume Ant is unavailable just because `ant` fails in one shell. Prefer the project build flow and, if needed, ask to run it outside the sandbox or use the user's configured Ant environment.

## Local Change Notes

Each feature should get one markdown file here named like:

```text
0001-hud-meter-widgets.md
0002-next-feature.md
```

Each note should list:

- What changed.
- Which files changed.
- How to verify it.
- Any merge-sensitive spots.
