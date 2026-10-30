# Changelog

## 1.3.0-beta.3

Fixes for long chapter names.

- Chapter names are no longer cut mid-word: in the journal, the details pane and the quest
  editor, a name that does not fit is shortened with `...` instead of ending abruptly.
- The journal chapter column and the editor row give chapter names more room, so normal
  names such as `Chapter I - Ashen Road` are shown in full.
- In the editor, the chapter and the objective count now sit at opposite ends of the row
  and no longer share one narrow column.
- Book theme: the journal title and the theme button no longer overlap the cover frame;
  the header starts below the inner page edge.

## 1.3.0-beta.2

Fixes for the first beta build.

- Softer ink: primary text `#5A4530`, secondary `#6E563E`, muted `#7E6547`.
  Text stays readable on both parchment and book pages without looking black.
- No drop shadows on theme text, so titles and list rows are no longer doubled.
- Fixed five Russian theme strings that shipped as `???` in beta.1:
  quest counter, journal theme label, theme names (Пергамент/Книга) and the
  `theme` option name in the client config.

## 1.3.0-beta.1

Interface themes: Paper and Book. This is a visual release, quest data and server logic are unchanged.

- Two client themes: **Paper** (parchment with dark ink, default) and **Book** (leather cover with
  paper pages, spine and bookmark).
- Theme engine with 9-slice pixel-art textures: panels, insets, slots, buttons, dividers,
  scrollbars, icons and decorations.
- Every StoryQuest screen is reskinned: journal, quest editor, quest form, objectives, conditions,
  marker editor, dialogue, HUD tracker and waypoint compass.
- The journal has a theme button that cycles Paper/Book and saves the choice; the same value is
  available in the client config (`theme`).
- The journal layout was adjusted for the themes: chapter list, details pane, status icons,
  requirements and objective markers.
- All default orange accents are gone. Status colours are green (done), red (error/locked) and
  blue-grey (information).
- New white marker glyphs (`arrow`, `check`, `pin`, `star`).
- Removed the `accentColor` and `panelColor` client options. Old config files keep working,
  unknown keys are ignored.

## 1.2.0

Quest prerequisites, branches and co-op.

- Quest conditions: required quests, all/any mode, hide until available, and branch locks.
- Conditions editor with search, cycle detection, missing references and mixed-scope warnings.
- The journal shows locked quests, requirements, branch state and shared quests.
- Shared co-op quests (`scope = SHARED`) use one world progress: any player can start the quest
  and complete its objectives; start/complete commands, messages and dialogue run for every
  online player.
- `/storyquest reset-shared [quest]` resets world-shared progress.
- JSON export/import: `/storyquest export [name]` and
  `/storyquest import <name> [merge|replace] [confirm]`.
  Full quest definitions are exported; imports are validated, atomic and create automatic DAT
  backups (last 20 are kept).
- `/storyquest force-start` starts a quest while bypassing conditions for testing.
- Existing worlds load with defaults (`SOLO`, no requirements, no branch); no migration is needed.
- DAT remains internal storage; JSON is the exchange format.

## 1.1.0

Quest markers and waypoints.

- Every Visit objective can have its own marker: icon (checkmark, pin, star), color,
  label, visibility and an optional vertical light beam.
- Floating marker icons above objective locations, visible through terrain and fading
  with distance, plus a light beam that can be seen from far away.
- Waypoint compass at the top of the screen: rotating arrow, objective label and
  distance to the nearest unfinished objective of the tracked quest.
- Compass handles other dimensions ("another dimension") and shows "you are here"
  when the player is already inside the objective area.
- Marker settings are edited per objective in the quest editor (button "Marker...").
- New client config options: `waypointsEnabled`, `waypointCompass`, compass offsets,
  `waypointBeams`, `waypointBeamHeight`, `waypointMaxDistance`, `waypointIconScale`.
- Existing quests get default markers automatically when loaded.

## 1.0.0

First release.

- In-game quest editor (`K`): main info, story text, objectives, scripts.
- Quest journal (`J`) with chapters, descriptions and objective status.
- Configurable HUD tracker (corner, offsets, scale, opacity, colors).
- Dialogue screens with a narrator and pages.
- Objectives: Manual (command) and Visit (dimension + X/Y/Z + radius).
- Commands on quest start and completion with `%player%` support.
- Per-player progress that survives death; quests stored in the world save.
- English and Russian localization.
- Works on dedicated servers (no client classes on the server side).
