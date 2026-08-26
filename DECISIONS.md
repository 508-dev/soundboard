# Soundboard Decisions

Why this app is built the way it is. Each entry is a decision that was
genuinely open at the time, with the reasoning that closed it — not a
description of the code. Add to it when you make a call a future reader would
otherwise have to reverse-engineer.

## Native Android, Not A Web/Service Stack

This repo was generated from the 508.dev devkit, which is tailored to web
projects (Bun/TypeScript, Python, Ruby, Docker Compose). None of that survives
here. The product is a single-user, offline, local-audio app: there is no
server, no account, no network call at all. A native Android app is the whole
product surface.

The devkit's durable hygiene (agent instructions, docs shape, CI conventions,
Renovate policy, GitHub templates) was kept and adapted; its stack packs were
deleted. `MANIFEST.md` records the original inventory in case a backend is ever
added.

## Conventions Follow `emotion-tracker`

A sibling app under the same co-op umbrella — `emotion-tracker` — already
converted this devkit to Android. Rather than re-derive the toolchain, this repo
matches it deliberately: same AGP/Gradle/Kotlin pins, same `dev.co508.*`
namespace, same single-`:app`-module layout, same ktlint setup, same
`scripts/*.sh` → `./gradlew` wrappers, same GPL-3/F-Droid posture.

Divergences from the sibling are listed below with reasons ("DataStore Over
Room", the `androidx.media3` dependency). Anything not listed should match; if
you find yourself differing for no reason, prefer the sibling's choice.

## Jetpack Compose Over Views

The whole UI is a single scrolling list of cards, a FAB, and three modals. That
is squarely Compose's strong case, it is what the sibling app uses, and it keeps
the custom volume dial (`ui/components/VolumeDial.kt`) as ~100 lines of `Canvas`
plus two gesture detectors rather than a custom `View` subclass.

## Media3 ExoPlayer, One Player Per Sound

The product requirement is *N* sounds looping simultaneously, each with its own
volume, each independently startable and stoppable. Options considered:

- **`SoundPool`** — built for exactly this shape (many simultaneous streams,
  per-stream volume and looping) but decodes into memory and caps a sample
  around 1 MB compressed. Ambient loops are minutes long. Rejected.
- **`MediaPlayer`** — free, in-framework, has `setLooping` and `setVolume`.
  But looping a compressed format (MP3/AAC) leaves an audible gap at the loop
  point, because the decoder is torn down and re-primed. For an app whose main
  use is a continuous soundscape, that gap is the one defect users would
  actually notice. Rejected.
- **Media3 ExoPlayer, one instance per sound** — gapless via
  `REPEAT_MODE_ONE`, per-player `volume`, robust `content://` handling.
  Chosen. Costs ~1–2 MB of APK and one player object per *playing* sound.

Players are created lazily on first play and retained while paused (so resume
is instant), then released when the sound leaves the board.

**Not `MediaSessionService`.** A `MediaSession` maps 1:1 to a single `Player`,
and this app has *N*. Trying to expose a mixer through one session means
inventing a fake aggregate player. The app uses a plain foreground `Service`
with its own notification instead, and gives up system transport-control
integration (Bluetooth buttons, lock-screen scrubber) — which do not have a
sensible meaning for a mix anyway. Revisit only if a "master transport" concept
is added to the product.

## Engine In Application, Service For Lifetime Only

`SoundboardEngine` is owned by `SoundboardApp` (the `Application`), not by
`PlaybackService`. The service exists solely to keep the process alive and show
the notification Android requires in exchange; it reads engine state and never
owns players.

The alternative — players inside the service, UI bound to it — puts an
asynchronous `bindService` handshake between a tap and a sound, and forces every
UI action to handle "not bound yet". Since the engine has to outlive the
Activity but not the process, the Application is the correct scope, and the
service becomes a small, stateless mirror.

Consequence: the engine is main-thread-only (ExoPlayer requires single-threaded
access from its creating thread), which is fine because every caller is either
Compose or the service's main-dispatcher scope.

## Audio Focus Held Centrally, Not Per Player

ExoPlayer can manage audio focus itself via `setAudioAttributes(attrs, true)`.
With *N* players that would be *N* focus requests from one app, competing with
each other — the system grants focus to the latest requester, so our own players
would duck and pause one another.

Instead every player is built with `handleAudioFocus = false` and
`AudioFocusHolder` owns exactly one `AUDIOFOCUS_GAIN` request for the engine as
a whole: taken when the first sound starts, abandoned when the last stops.
Transient loss pauses the playing set and remembers it so focus regain restores
exactly that set; `CAN_DUCK` scales every player by a shared duck factor without
touching the user's configured percentages.

## Volume Is Linear Amplitude, Relative To System Volume

A sound's percentage sets ExoPlayer's `volume` directly (`percent / 100`), which
scales the stream *before* the Android media volume is applied. The two
therefore compose exactly as the spec asks: the hardware keys move the whole mix
together, the in-app percentage moves one sound against the others, and the
relationship survives system volume changes with no work on our part.

The mapping is linear in amplitude, not perceptual — 50% is −6 dB, which sounds
louder than "half". A perceptual taper was considered and deferred: it makes the
displayed number stop meaning anything simple, and the right curve is easier to
choose once there is real listening feedback.

## DataStore Over Room

The sibling app uses Room; this one does not. The entire persisted state is one
short ordered list with no relations, no queries, and no history — the user has
a handful of sounds, not a journal. Room would add KSP, a schema directory,
migration tests, and a DAO to store what is naturally a single JSON document.

`DataStore<SoundLibrary>` with a `kotlinx.serialization` codec covers it in two
small files, and keeps all list logic in `SoundLibrary` as pure functions that
unit-test without an Android runtime. Revisit if sounds ever gain
cross-references, tags, or a play history.

A malformed file deserializes to an empty board rather than throwing. The board
is rebuildable in seconds, so refusing to launch would be the worse failure.

## Reference Picked Files By URI, Never Copy

The spec asks for this explicitly, and it is also the right default: audio files
are large, users already have them organised, and silently duplicating them into
app storage doubles disk use and creates a second copy that drifts.

The picker uses `ACTION_OPEN_DOCUMENT` (via `OpenDocument`), **not**
`ACTION_GET_CONTENT` — only the former returns a URI whose read permission can
be persisted with `takePersistableUriPermission` and survive a reboot. The
permission is released when the sound is removed, so the app does not hoard
grants.

The cost is that the app cannot guarantee a sound still plays: the file may be
moved, renamed, deleted, or its grant revoked. That is surfaced honestly as a
`PlaybackStatus.UNAVAILABLE` row the user can see and delete, rather than hidden
behind a silent failure.

**The board is excluded from backup** (`res/xml/backup_rules.xml`,
`data_extraction_rules.xml`) for the same reason: URI grants are scoped to this
install on this device, so a restored board would be entirely broken rows.
Restoring to an empty board is the more honest outcome.

## Notification: Summary Plus "Stop All"

Android's standard notification layout shows at most three actions, and this app
can have any number of sounds playing. Per-sound controls would need a custom
`RemoteViews` layout that degrades past three or four rows.

The notification therefore reports a count ("3 sounds playing") with a single
"Stop all" action, and tapping it opens the app where per-sound control already
lives. Playback continues if the user denies `POST_NOTIFICATIONS`; only the
notification and its Stop All are withheld.

## Sounds Loop Until Paused

Each sound loops (`REPEAT_MODE_ONE`) rather than playing once. The product is a
soundscape mixer — rain plus a fan plus a café — where one-shot playback would
make the play/pause toggle and the per-sound volume mixing pointless. A per-sound
one-shot/loop toggle is a plausible later addition to the long-press sheet, which
is why that sheet exists as a list rather than a single Delete button.

## Whole Row Toggles Playback

The spec puts a play/pause button on the right of each card. That button is
there, but the whole card is also clickable and does the same thing: it is a far
easier target on a phone, and it makes the card's ripple mean something. The
volume percentage and long-press gesture keep their own handling, so the row tap
is unambiguous.

## No Dependency Injection Framework

Matching the sibling app. Two objects (`SoundRepository`, `SoundboardEngine`)
are constructed in `SoundboardApp` and reached through a `CreationExtras`
helper. Hilt would be more machinery than the graph justifies. Don't add one
without discussing it.

## GPL-3, Targeting F-Droid

Matching the sibling app. Every shipped dependency must itself be free software,
because F-Droid builds from source and requires the whole app transitively to
qualify. In practice: AndroidX and Kotlin-stdlib-class libraries only — no Google
Play Services, no Firebase, no closed-source SDKs. `androidx.media3` is
Apache-2.0 and qualifies. Compose's dynamic color API is a pure AndroidX API
despite the "Material You" branding, and is fine.

## SDK Levels: minSdk 26, targetSdk 36, compileSdk 37

Matching the sibling app. minSdk 26 (Android 8.0) gives notification channels
and `AudioFocusRequest` natively, so there are no compat branches in the audio or
notification code. targetSdk stays one below compileSdk until API 37's behaviour
changes are reviewed.

## Deferred: Instrumented (`androidTest`) Coverage

Unit tests cover the pure logic: library mutations, persistence round-trips and
corruption handling, and the dial's angle→percent mapping. The parts that are
not covered — ExoPlayer behaviour, the foreground service lifecycle, SAF
permission persistence — all need a real device or emulator and are verified by
hand for now. Add `androidTest` coverage when the audio layer next changes
shape.
