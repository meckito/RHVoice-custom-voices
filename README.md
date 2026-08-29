# RHVoice

> ## This is a fork: RHVoice Custom
>
> This repository is a fork of [RHVoice/RHVoice](https://github.com/RHVoice/RHVoice)
> which adds **one** feature to the Android application: **installing a voice from a
> file on your device** — either from inside the application or straight from a file
> manager via "Open with". Upstream can only install voices from its own online
> catalogue.
>
> Documentation: [English](doc/en/Custom-voices.md) ·
> [polski](doc/pl/Custom-voices.md)
>
> ### What is different from upstream
>
> * New screen **Own voices**: import, switch on/off, remove.
> * Accepts both Android voice packages and NVDA add-ons (`.nvda-addon`); the
>   voice subtree is normalised on import. Voices only — language data is never
>   imported.
> * Registers as a handler for voice packages, so "Open with" works.
> * Designed for screen reader users: results are persistent on-screen text in a
>   live region (not toasts), and controls carry the voice name in their labels.
> * **No changes to the C++/JNI engine code.** The whole feature is Java plus
>   resources, and it is covered by unit tests that run without a device.
>
> ### Installs alongside the official application — read this
>
> The fork uses its own application id (`org.tomecki.rhvoice.customvoices`) and its
> own name (**RHVoice Custom**), so it **does not overwrite** an official RHVoice
> installation. Both can be installed at the same time and both appear in the
> system text-to-speech settings as separate engines.
>
> Consequences worth knowing:
>
> * The two installations **do not share data**. Languages and voices downloaded in
>   the official application are not visible to the fork; the fork downloads its own
>   copies, so expect additional storage use.
> * Voices imported from a file exist **only in the fork**.
> * Switching your default text-to-speech engine between the two is a normal system
>   setting, but remember that per-voice choices are engine-specific.
>
> ### Licence
>
> Unchanged: GPL-2.0, as upstream. The source of this fork is public, which is what
> the licence requires when the built application is distributed.

RHVoice is a free and open-source speech synthesizer.

## Features

### Speech synthesis method

RHVoice uses [statistical parametric synthesis](https://en.wikipedia.org/wiki/Speech_synthesis#HMM-based_synthesis).
It relies on existing open-source speech technologies (mainly
[HTS](https://hts.sp.nitech.ac.jp) and related software).

Voices are built from recordings of natural speech. They have small footprints,
because only statistical models are stored on users' computers. And though the
voices lack the naturalness of the synthesizers which generate speech by
combining segments of the recordings themselves, they are still very
intelligible and resemble the speakers who recorded the source material.

### Supported languages

Initially, RHVoice could speak only Russian. Now it also supports:

* American and Scottish English
* Brazilian Portuguese
* Esperanto
* Georgian
* Ukrainian
* Kyrgyz
* Tatar
* Macedonian
* Albanian
* Polish

In theory, it is possible to implement support for
other languages, if all the necessary resources can be found or
created.

### Synthesis example

If you want to listen to an example of speech synthesis, You can use the TTS
service on [this page](https://data2data.ru/tts/).

### Supported platforms

RHVoice supports the following platforms:

* Windows (prebuilt binaries is available in documentation)
* GNU/Linux (building instructions and packaging status
  can be found in "Compiling instructions" section of documentation.
* Android (can be installed thru
  [F-Droid](https://f-droid.org/packages/com.github.olga_yakovleva.rhvoice.android/)
  or [Google Play](https://play.google.com/store/apps/details?id=com.github.olga_yakovleva.rhvoice.android)

It is compatible with standard text-to-speech interfaces on these platforms:
SAPI5 on Windows, [Speech Dispatcher](https://devel.freebsoft.org/speechd) on
GNU/Linux and Android's text-to-speech APIs. It can also be used by the
[NVDA screen reader](https://www.nvaccess.org) directly (the driver is provided
by RHVoice itself).

## Documentation

All prebuild binaries packages, main
and legal information and more
are available in three languages:

* [English](doc/en/index.md)
* [Русский](doc/ru/index.md)
* [Українська](doc/ua/index.md)

## Community

### Official

* [GitHub Discussions](https://github.com/RHVoice/RHVoice/discussions/)
* [Mailing list](https://groups.io/g/RHVoice-rus) (Russian)

### Unofficial

* IRC channel: `#rhvoice` at [irc.libera.chat](ircs://irc.libera.chat:6697)
* Matrix room: [#rhvoice:libera.chat](https://matrix.to/#/#rhvoice:libera.chat)
