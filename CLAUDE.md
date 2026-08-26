# Claude Code Instructions

The canonical agent instructions are in `AGENTS.md`. Read it before editing.

Quick orientation:

- Single-module native Android app: Kotlin + Jetpack Compose + Gradle Kotlin
  DSL. No Bun/uv/Bundler/Docker — the web stacks from the 508.dev devkit were
  deleted when this repo was set up.
- Build and test with `./gradlew`. If the system JDK isn't Java 25, use
  `JAVA_HOME=/opt/android-studio/jbr ./gradlew <task>`.
- Read `DECISIONS.md` before touching `audio/` — the threading, audio-focus,
  and service-ownership rules there are easy to break with a change that
  looks reasonable and compiles fine.
- A green `./gradlew check` does not prove playback works. Say what you
  verified on a device and what you didn't.
- Use gitignored `.context/` for concise workspace-local operational memory.
  Promote durable knowledge into tracked docs instead of committing `.context/`.
