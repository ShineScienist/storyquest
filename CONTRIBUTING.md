# Contributing to StoryQuest

Thanks for helping. The project is a small NeoForge mod; the rules are short.

## Development setup

1. Install JDK 21.
2. Clone the repository and run:

```bash
./gradlew build      # compile and package
./gradlew runClient  # dev client with the mod
./gradlew runServer  # dev server
```

The Gradle wrapper is included; no separate Gradle installation is needed.
On Windows use `gradlew.bat`.

## What to work on

- Bugs and small improvements: open a pull request directly.
- New gameplay mechanics: open an issue first, so the design matches
  [docs/ROADMAP.md](../docs/ROADMAP.md) and does not conflict with planned work.
- UI work: keep both themes (Paper and Book) working. Read
  [docs/ROADMAP.md](../docs/ROADMAP.md) before changing layout.

## Pull request checklist

- `gradlew build` passes.
- Tested in game: journal (`J`), editor (`K`), HUD and waypoints.
- Both Paper and Book themes checked if you touched a screen.
- Russian and English language files updated for every new user-facing string.
- No jars, logs, worlds, screenshots or local design sources in the commit.
- One logical change per PR; describe the steps you used to verify it.

## Code style

- Java 21, 4-space indentation, no wildcard imports.
- Keep client-only code under `client/` and never load client classes on a
  dedicated server.
- User-facing text goes through language keys (`en_us.json`, `ru_ru.json`).
- Pixel-art textures are 1x assets with 9-slice borders; do not upscale them.

## Reporting bugs

Use the issue template and include the mod version, Minecraft/NeoForge version,
steps to reproduce, and the log or crash report. For visual bugs, attach a
screenshot and mention the GUI scale.
