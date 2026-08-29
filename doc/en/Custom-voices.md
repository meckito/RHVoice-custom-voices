# Custom voices (fork feature)

This page describes a feature that **does not exist in upstream RHVoice**: installing
a voice from a file you already have on your device, without waiting for it to appear
in the official voice catalogue.

It is available in the fork
[meckito/RHVoice-custom-voices](https://github.com/meckito/RHVoice-custom-voices),
in the Android application only.

## What you need before you start

**The language of the voice must already be installed.** A voice contains only the
speaker's statistical models; the rules for turning text into sounds live in the
language data. If you import a Polish voice without the Polish language installed,
the import is refused with a message naming the missing language — install that
language first from the main screen, then import the voice again.

## Two ways to import

### From inside the application

1. Open the application.
2. Open the menu in the top bar and choose **Own voices**.
3. Press **Install a voice from a file**.
4. Pick the file in the system file picker.

### From a file manager ("Open with")

Tap the voice package in any file manager and choose **RHVoice Custom** from the
list of applications. The entry is labelled *Install a voice from a file*. Sharing
the file to the application ("Share" / "Send to") works the same way.

The application only offers itself for ZIP archives and for files whose name ends
in `.nvda-addon`; it does not register itself as a handler for every file on the
device.

## Supported package layouts

Both layouts are accepted, and both end up stored identically:

| Layout | Where `voice.info` is | Typical source |
|---|---|---|
| Android voice package | in the archive root | voice packages built for the Android application |
| NVDA add-on | inside a subdirectory, usually `data/` | `.nvda-addon` files published for the NVDA screen reader |

For an NVDA add-on, only the voice subtree is extracted. Everything belonging to the
add-on wrapper — `manifest.ini`, `doc/`, and language data in `langdata/` — is
ignored. **Language data is never imported**: this feature installs voices only.

The package must contain a `voice.info` file with at least `name`, `language` and
`format`. `revision` defaults to `0` when absent.

## Managing imported voices

On the **Own voices** screen each voice has:

* a switch — turning it off hides the voice from the system without deleting it,
* a remove button — asks for confirmation, then deletes both the registry entry and
  the voice data.

Changes take effect immediately; there is no need to restart the speech service.

## Accessibility

The feature was written for screen reader users:

* the result of an import is shown as **persistent text on the screen** inside a
  live region, not as a transient toast, so it is announced immediately and can be
  read again afterwards;
* the switch and the remove button carry the **name of the voice** in their labels,
  so with several voices on the list it is clear which one they act on;
* every control has a text label.

## Where the voices are stored

Imported voices live in the application's private storage, in a directory that is
deliberately separate from the one used for downloaded packages, so the upstream
clean-up routine can never delete them:

```
<app private dir>/app_local-voices/
    local-voices.properties    registry: id = name|language|enabled
    <voice id>/                voice data, with voice.info in its root
```

The voice identifier is derived from the voice name exactly the way upstream does it
(lower case, `-` replaced by `_`), so an imported voice and a downloaded voice of the
same name occupy the same identity.

## Safety and failure behaviour

* **Malicious archives are rejected.** An entry that tries to write outside the
  target directory (`../`, absolute paths) disqualifies the whole package. Nothing
  is extracted.
* **A failed import never damages a working voice.** Extraction happens in a
  temporary directory and is moved into place only at the very end, so if anything
  goes wrong the previously installed version keeps working.
* **Re-importing a voice replaces it** rather than merging with the old files, and
  it preserves your decision if you had switched that voice off.
* **Temporary files are always cleaned up**, including after an error.
* A damaged registry file does not hide your downloaded voices: the engine still
  receives all the normally installed ones.

## Known limitations

* One voice per file; importing several at once is not implemented.
* An imported voice is never made the default voice automatically.
* The voice must match a language the application knows by name; a voice for a
  language absent from the catalogue is skipped with a warning in the log.
