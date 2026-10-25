# Changelog

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
