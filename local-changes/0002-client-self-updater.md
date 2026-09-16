# Client Self Updater

## What Changed

- Added `config.homedir=hashdir` to the shipped config so user files are stored under `%APPDATA%\Haven and Hearth\kami-client` instead of inside the client folder.
- Added `ClientUpdater`, which reads `kami.update.manifest`, downloads the release zip, starts a temporary PowerShell installer, exits the client, and lets the installer replace app files.
- Added a `Check for updates` button in Options.
- Added `ant dist`, which writes `dist/KamiClient.zip` and a matching `dist/update.json`.

## Release Flow

1. Set `kami.update.manifest` in `etc/ansgar-config.properties` to the hosted URL for `update.json`.
2. Run `build-kami-bin.bat`, then `ant dist` if needed.
3. Upload both `dist/KamiClient.zip` and `dist/update.json` to the same public folder.

The generated manifest uses a relative zip URL, so moving both files together is enough.

## Manifest Format

```json
{
  "version": "15.09.2026 20:00",
  "url": "KamiClient.zip",
  "sha256": "optional sha256",
  "notes": "Short update notes"
}
```

## Verify

- Start the client from an extracted zip.
- Open Options and click `Check for updates`.
- With an unchanged manifest version, it should say the client is up to date.
- With a different manifest version, it should download, exit, replace files, and restart.
