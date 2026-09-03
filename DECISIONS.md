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

The UI is a scrolling list of cards, a FAB, three modals, and a drawer over two
static pages. That is squarely Compose's strong case, it is what the sibling app
uses, and it keeps
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

The picker uses `ACTION_OPEN_DOCUMENT` (via `OpenMultipleDocuments`), **not**
`ACTION_GET_CONTENT` — only the former returns URIs whose read permission can
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

## Navigation Drawer Over Three Flat Destinations

The board, About, and Licenses are siblings with no hierarchy, so a
`ModalNavigationDrawer` behind a hamburger is the right container — it scales to
a Settings or Presets entry later without restructuring anything.

Classic `navigation-compose` rather than local state, matching the sibling app.
It costs one dependency, and buys correct system-back behaviour and
`saveState`/`restoreState` on the board's scroll position for free; hand-rolled
screen switching gets both of those wrong by default.

Unlike the sibling, the top bar is **not** hoisted into `AppScaffold`. The board
needs a FAB and a "stop all" action that the other two screens have no use for,
and hoisting would mean threading `SoundboardViewModel` up past screens that
don't want it. Each screen renders its own `DrawerScaffold` instead, which is
the shared top bar plus a hamburger, with slots for whatever chrome that screen
adds.

## The Licence List Is Hand-Maintained

`ui/about/LicensesScreen.kt` holds a literal `DEPENDENCIES` list. The obvious
alternative — Google's OSS-licences Gradle plugin — generates the list
automatically but is part of Play Services, which is not free software and would
undermine the F-Droid goal outright.

So the list is written by hand and grouped by project rather than by Maven
coordinate, since listing every `androidx.*` artifact separately is noise when
they share a licence. **It must be updated whenever
`gradle/libs.versions.toml` changes.** A stale list here is a compliance
problem, not a cosmetic one.

## Multi-Select When Adding Sounds

The picker uses `OpenMultipleDocuments` rather than `OpenDocument`. Adding
ambience happens in batches — a folder of field recordings, not one file — and
the single-select flow meant re-opening the picker once per file.

The batch is added in a single DataStore write, with display names resolved
before the transaction opens so one slow content provider can't hold it. A file
whose permission can't be persisted is skipped rather than failing the batch:
losing one file out of thirty shouldn't lose the other twenty-nine.

Note Android caps how many persisted URI grants an app may hold (512 on current
versions, 128 historically). Well beyond a plausible board, but it is the reason
grants are released on delete rather than left to accumulate.

## The Volume Percentage Is A Button, Not A Label

First pass rendered the percentage as a bare `TextButton`, and it read as a
status readout — nothing said "tap me". It is now a filled pill with a speaker
icon, in the same colour as the play button beside it, so the two read as one
cluster of controls sitting on the card. The icon switches to a muted speaker at
0% so a silenced sound is obvious without reading the number.

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

## Reordering Is A Separate Mode, Not A Long-Press Overload

The board already used long-press on a row to open the delete sheet. Adding
drag-to-reorder on the same row means either teaching long-press to mean two
different things (menu vs. drag start), or moving delete somewhere else so
long-press is unambiguous. Both are workable; the app instead adds an explicit
"rearrange" toggle in the top bar that swaps the whole list for
`ReorderableSoundList` — a different row entirely, with a dedicated drag handle
and a trailing delete icon, tap-to-play disabled. Normal mode is completely
unchanged.

The toggle also gives rearrange mode a natural home for the one-shot sort
shortcuts (name, volume, currently-playing) — thinking of the toggled state as
"the board as a table" rather than "drag mode" is what made this the right
split, not just a gesture-conflict workaround.

Sort shortcuts and a completed drag both go through
`SoundLibrary.reordered(order)` — a single "apply this id order" primitive — so
a sort is a one-time write, not a continuously maintained mode; the user can
drag again immediately after tapping a sort chip. "Currently playing" is the
one sort that can't be a pure `SoundLibrary` function, since playback status is
live engine state the library doesn't have — so all three sorts are computed in
`ReorderableSoundList` from `SoundRowState`, which already carries it. (They
started out in `SoundboardViewModel`, but that routes the order change through
a DataStore round-trip, which breaks the same-frame requirement below.)

Two scroll behaviours of `LazyColumn` shape the implementation. Foundation pins
scroll position to the first visible item's *key* when the item list changes
(`LazyListScrollPosition.updateScrollPositionIfTheFirstItemWasMoved`) — usually
what you want, but here it means (1) a drag swap involving the topmost visible
row scrolls the whole viewport along with it, and (2) a re-sorted board "jumps
to a random place" as the anchor follows whatever row happened to be on top.
An explicit `scrollToItem` clears the stored anchor key
(`requestPositionAndForgetLastKnownKey`), so both fixes request the desired
position in the same frame as the order change: re-pin the current position on
every swap (position-wise a no-op; it only disarms the anchor), and request the
top on a sort. Sorts are applied to the list's working order synchronously and
only then persisted via `commitOrder`, because the scroll request has to land
in the same frame as the order change to beat the anchor restore — any
round-trip in between lets a measure re-populate the anchor key first.

Drag mechanics are hand-rolled (`detectDragGestures` + `LazyListState.layoutInfo`
to find item bounds and swap), matching how `VolumeDial` already does custom
gesture handling in this codebase, rather than adding a third-party
reorderable-list dependency — Compose Foundation 1.12 (the version this app is
on) has no built-in list-reorder API. Revisit if a future Foundation release
ships one; hand-rolled swap-threshold math is the kind of code that's fine to
delete in favor of a first-party API.

## Releases Are Gated By A Release PR, Not By Merging To Main

Merging to `main` grooms a standing release PR; merging *that* PR is what tags a
version and publishes. The alternative — release on every merge to `main` — was
rejected for two reasons. It makes every merge a public release, with no way to
batch three commits into one version. And it needs CI to push a version-bump
commit directly to `main`, which means either leaving `main` unprotected or
granting a bot a branch-protection bypass. The release-PR model needs neither:
the bot only ever opens a PR, and a human merge is the deliberate act.

Both jobs live in one workflow run, which reads oddly until you know why: a tag
or PR created with `GITHUB_TOKEN` does not trigger another workflow. A separate
tag-triggered publish workflow is the obvious design and silently never fires.

## `versionCode` Is Derived From `version.txt`, And Stays A Literal

`version.txt` is the source of truth; `versionCode` is computed from it as
`major * 1000000 + minor * 1000 + patch` by `scripts/sync-version.sh`. Deriving
it means it cannot drift from the version name, and it rises with any semver
bump — which matters because Play permanently rejects a `versionCode` it has
already seen for an app, including from a deleted release.

The obvious tidier implementation is to compute it inside `app/build.gradle.kts`
from `versionName`. That was rejected: F-Droid's update bot regex-parses
`versionCode` and `versionName` textually out of the Gradle file to notice a new
version, so an expression there ends automatic f-droid.org releases with no
error anywhere. Generating literals into the file keeps both properties. CI runs
`scripts/sync-version.sh --check` so the generated values cannot go stale.

## Two F-Droid Paths, Because Neither Alone Is Enough

The app publishes to a self-hosted F-Droid repository on `gh-pages` *and* is
submitted to f-droid.org. They solve different halves of the problem.

f-droid.org gives discovery — users find the app by searching in the client they
already have — but nothing can be pushed to it from CI. It builds and signs on
its own infrastructure, on its own schedule, and each release reaches users days
later. The self-hosted repository is the opposite: fully automated and live
within minutes of a release, but users must add a URL by hand.

The cost of running both is that they sign with different keys, so a user cannot
switch between them without uninstalling. That is inherent to f-droid.org's
build model, not to this choice; F-Droid's reproducible-builds process is the
eventual fix.

The self-hosted repo is a git branch rather than a deployed directory because an
F-Droid repository is cumulative — every published version stays in the index —
so each run must add to the previous state rather than replace it.

## Store Copy Lives In `fastlane/`, Not In Each Store's Own Format

Play, the self-hosted F-Droid repo, and f-droid.org all need the same listing
text and per-release notes in three different layouts. All three can read (or be
fed from) the `fastlane/metadata/android/<locale>/` convention, so that is the
one place the copy is written. `scripts/fdroid-publish.sh` copies it into the
layout fdroidserver wants; the release workflow renames the changelog into the
`whatsnew-<locale>` layout Play wants; f-droid.org reads it from the repo
directly. Keeping per-store copies in the F-Droid metadata YAML would have been
less machinery and three things to keep in step.

## Deferred: Instrumented (`androidTest`) Coverage

Unit tests cover the pure logic: library mutations, persistence round-trips and
corruption handling, and the dial's angle→percent mapping. The parts that are
not covered — ExoPlayer behaviour, the foreground service lifecycle, SAF
permission persistence — all need a real device or emulator and are verified by
hand for now. Add `androidTest` coverage when the audio layer next changes
shape.
