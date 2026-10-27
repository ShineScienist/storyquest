# Публикация StoryQuest

> **Статус:** 1.2.0 опубликован на CurseForge. Тексты ниже — рабочий шаблон для
> следующего релиза. Перед загрузкой 1.3.0 обновите версию, changelog и описание
> (в 1.3.0 появились темы Paper и Book).

Здесь лежат готовые тексты и пошаговая инструкция для CurseForge и Modrinth.
Ничего дополнительно настраивать не нужно: jar, иконка, лицензия и метаданные уже в проекте.

## Что публикуем

| Что | Где взять |
|-----|-----------|
| Jar | `build\libs\storyquest-<version>.jar` (собирается `gradlew build`) |
| Иконка 128×128 | `src\main\resources\storyquest.png` |
| Лицензия | `LICENSE` (MIT) |
| Версия | из `gradle.properties`; сейчас `1.3.0-beta.2`, Minecraft `1.21.1`, NeoForge `21.1.0+` (собрано на 21.1.264) |
| Среда | клиент и сервер (Client and server) |

Проверка перед загрузкой:

```bash
gradlew build
```

и убедиться, что в `build\libs` лежит свежий jar.

---

## Modrinth

Публикуется сразу, модерация не нужна.

1. Войди на [modrinth.com](https://modrinth.com) → **Create a project** → **Mod**.
2. **Name:** `StoryQuest`, **Slug:** `storyquest` (слаг свободен на момент подготовки).
3. **Summary** — короткое описание (до 256 символов):

   > Story-driven quests for Minecraft maps: in-game editor, journal, HUD tracker, dialogues, visit zones, conditions, co-op and JSON exchange.

4. **Description** — вставь английский текст из раздела «Текст: EN» ниже.
5. **Icon** — загрузи `src\main\resources\storyquest.png`.
6. **Environment** — *Client and server*.
7. **License** — *MIT License*.
8. **Links** — Source: `https://github.com/ShineScienist/storyquest`, Issues: `https://github.com/ShineScienist/storyquest/issues`.
9. **Tags** — `adventure`, `management`, `utility`, `game-mechanics`; загрузчик `NeoForge`, версия игры `1.21.1`.
10. Вкладка **Versions** → **Create version**:
    - Version number: `1.3.0-beta.2`
    - Channel: `Beta`
    - Loaders: `NeoForge`
    - Game version: `1.21.1`
    - File: `storyquest-1.3.0-beta.2.jar`
    - Changelog: текст из раздела «Changelog 1.3.0-beta.2».

---

## CurseForge

Новый проект проходит модерацию (обычно от нескольких часов до пары дней), после одобрения файлы становятся видны всем.

1. [curseforge.com](https://www.curseforge.com) → **Authors** → **Create Project** → **Minecraft** → **Mods**.
2. **Name:** `StoryQuest`. Если имя занято — `StoryQuest: Narrative Quests` или `StoryQuest (NeoForge)`.
3. **Summary** (одна строка):

   > Story-driven quests for Minecraft maps: in-game editor, journal, HUD tracker, dialogues, visit zones, conditions, co-op and JSON exchange.

4. **Description** — вставь английский текст (CurseForge понимает Markdown: вставь как есть в редактор описания).
   Русский текст можно добавить вторым блоком в конец — аудитория CF интернациональная.
5. **Category:** *Adventure*, дополнительно *Utility* / *Server* (если доступны).
6. **Game Version:** `1.21.1`, **Mod Loader:** `NeoForge`.
7. **License:** MIT (поле `License` в настройках проекта).
8. **Avatar:** `src\main\resources\storyquest.png`.
9. Вкладка **Files** → **Upload File**:
    - File: `storyquest-1.3.0-beta.2.jar`
    - Release type: `Beta`
    - Game version: `1.21.1`, loader `NeoForge`
    - Changelog: текст из раздела «Changelog 1.3.0-beta.2».

> Зависимости указывать не нужно: требуется только NeoForge, он есть у всех, кто ставит моды на 1.21.1.

---

## Текст: EN

```markdown
# StoryQuest

**Story-driven quests for Minecraft maps - in-game authoring, optional JSON exchange.**

StoryQuest is a lightweight quest system for **Minecraft 1.21.1 (NeoForge)**, built for adventure maps,
RPG servers and story campaigns. It replaces the "collect 64 cobblestone" loop with chapters, dialogues
and scripted events.

## Features

- **In-game quest editor** (`K`, operators only) — tabs for main info, story text, objectives and scripts.
- **Quest journal** (`J`) — chapters, descriptions, objective status, accept and track buttons.
- **Two interface themes** — **Paper** (parchment with dark ink, default) and **Book**
  (leather cover with paper pages, spine and bookmark). Switch them with the button in the
  journal header or in the client config; no restart needed.
- **HUD tracker** — shows the tracked quest and its objectives; corner, offsets, scale, opacity and colors
  are configurable on the client.
- **Dialogues** — a narrator line and paginated text (pages split by `|`) on quest start and completion,
  or on demand from a command.
- **Two objective types:**
  - *Manual* — completed by a command or command block;
  - *Visit* — completed automatically when the player enters a sphere
    (dimension + X/Y/Z + radius).
- **Quest markers and waypoints** — each Visit objective can have its own marker: a checkmark,
  pin or star icon, custom color, label and an optional vertical light beam. A top-screen compass
  shows the direction, name and distance to the nearest objective of the tracked quest, and
  handles other dimensions.
- **Scripts** — commands on quest start and on completion, separated by `;` or newlines,
  with a `%player%` placeholder. They run as the player with permission level 2,
  so `@s`, `give`, `effect`, `summon`, `tellraw`, `function` and friends all work.
- **Conditions and branches** — require completed quests (all or any), hide locked quests,
  and make alternative branches mutually exclusive.
- **Co-op shared quests** — one world progress for the whole group; any player can start
  the quest or complete an objective, while commands and dialogue run for every online player.
  Use `@s` for personal rewards, because each command runs once per player.
- **JSON export and import** — full quest definitions, validation, merge or replace import,
  and automatic DAT backups.
- **Per-player progress** stored in player NBT (`PlayerPersisted`) — survives death and respawn.
  Shared quest progress is stored in the world.
- **Quests live in the world save** (`data/storyquest_quests.dat`) — copy the world, copy the quests.
- **Localized** in English and Russian.

## Quick start

1. Join a world with cheats enabled (or be an operator on a server).
2. Press `K` to open the editor, then **New**.
3. Fill in the ID (`chapter_1_intro`), title, chapter and description.
4. **Text** tab — intro and final dialogue.
5. **Objectives** tab — add at least one objective. For *Visit*, press **My position**
   to grab your current coordinates and open **Marker...** to pick its icon, color and beam.
6. **Script** tab — commands to run on start and on completion.
7. Optional: open **Conditions...** to add prerequisites, a branch, or make the quest shared.
8. Save, then run:

   ```
   /storyquest start chapter_1_intro
   ```

## Commands

| Command | Purpose |
|---|---|
| `/storyquest journal` | Open the journal |
| `/storyquest editor` | Open the editor (operator) |
| `/storyquest list` | List quest ids |
| `/storyquest start <id> [players]` | Start a quest with condition checks |
| `/storyquest force-start <id> [players]` | Start a quest while bypassing conditions |
| `/storyquest complete <id> [players]` | Complete a quest and run its completion commands |
| `/storyquest objective complete <id> <n> [players]` | Complete a single objective |
| `/storyquest track <id> [players]` | Pin a quest in the HUD |
| `/storyquest untrack [players]` | Clear the tracker |
| `/storyquest reset [players]` | Reset personal quest progress |
| `/storyquest reset-shared [quest]` | Reset world-shared progress |
| `/storyquest export [name]` | Export all quests to JSON |
| `/storyquest import <name> [merge\|replace] [confirm]` | Import quests from JSON |
| `/storyquest dialogue <players> <speaker> <text>` | Show a dialogue |

## Configuration

- `config/storyquest-common.toml` — visit check interval, chat messages, sounds,
  manual start from the journal.
- `config/storyquest-client.toml` — HUD placement, scale, opacity and colors; quest marker
  visibility, compass placement, beam height and render distance.
- Everything is also available in game: **Mods → StoryQuest → Config**.

## Requirements

- Minecraft **1.21.1**
- **NeoForge 21.1.x** (built against 21.1.264)
- Java 21

Works in single player and on dedicated servers. No other mods required.

## Notes

- One tracked quest at a time (several quests can be active).
- No branching dialogue trees - branch with conditions and commands. They are planned for 1.4.0.
- Item/kill objectives are done with commands plus `/storyquest objective complete`.
- Co-op in 1.2.0+ uses one shared world progress. Separate parties are planned for 1.4.0.
- JSON export/import is command-based; editor buttons are not included yet.
- MIT license.
```

---

## Текст: RU

```markdown
# StoryQuest

**Сюжетные квесты для карт Minecraft — редактор в игре и необязательный JSON-обмен.**

StoryQuest — лёгкая квестовая система для **Minecraft 1.21.1 (NeoForge)** для приключенческих карт,
RP-серверов и сюжетных кампаний. Вместо «принеси 64 булыжника» — главы, диалоги и скриптовые события.

## Возможности

- **Редактор квестов в игре** (`K`, только для операторов): вкладки «Основное», «Текст», «Цели», «Скрипты».
- **Журнал квестов** (`J`): главы, описания, статус целей, кнопки «Принять» и «Отслеживать».
- **Две темы интерфейса** — **Paper** (пергамент с тёмными чернилами, по умолчанию) и **Book**
  (кожаный переплёт с бумажными страницами, корешком и закладкой). Переключаются кнопкой в шапке
  журнала или в настройках клиента, без перезапуска игры.
- **HUD-трекер**: активный квест и его цели; угол, смещение, масштаб, прозрачность и цвета настраиваются на клиенте.
- **Диалоги**: имя рассказчика и постраничный текст (страницы разделяются `|`) при старте и завершении квеста
  или по команде.
- **Два типа целей:**
  - *Вручную* — закрывается командой или командным блоком;
  - *Посещение* — закрывается автоматически, когда игрок входит в сферу
    (измерение + X/Y/Z + радиус).
- **Метки и вейпоинты** — у каждой цели «Посещение» своя метка: галочка, булавка или звезда,
  цвет, подпись и луч света в мире. Сверху экрана — компас с направлением, названием и
  расстоянием до ближайшей цели отслеживаемого квеста (умеет показывать другое измерение).
- **Скрипты**: команды при старте и при завершении, разделяются `;` или переводом строки,
  поддерживают `%player%`. Выполняются от имени игрока с правами оператора, поэтому работают
  `@s`, `give`, `effect`, `summon`, `tellraw`, `function` и т.д.
- **Условия и ветки**: требования по завершённым квестам (все или любой), скрытие до выполнения
  и взаимоисключающие ветки.
- **Общие квесты для коопа**: один прогресс на мир; любой игрок начинает квест или закрывает
  цель, а команды и диалоги получают все игроки онлайн. Для личных наград используйте `@s`:
  каждая команда выполняется отдельно от имени каждого игрока.
- **Экспорт и импорт JSON**: полные определения квестов, проверка, режимы merge/replace и
  автоматические резервные копии DAT.
- **Прогресс каждого игрока** хранится в NBT (`PlayerPersisted`) и переживает смерть и респавн.
  Прогресс общих квестов хранится в мире.
- **Квесты лежат в файле мира** (`data/storyquest_quests.dat`) — копируешь мир, копируются и квесты.
- **Локализация**: русский и английский.

## Быстрый старт

1. Зайди в мир с включёнными читами (или будь оператором на сервере).
2. Нажми `K` — откроется редактор, затем **«Новый»**.
3. Заполни ID (`chapter_1_intro`), название, главу и описание.
4. Вкладка **«Текст»** — вступление и финальный диалог.
5. Вкладка **«Цели»** — добавь хотя бы одну цель. Для «Посещения» нажми **«Мои координаты»**,
   а затем **«Настроить...»** у метки, чтобы выбрать иконку, цвет и луч.
6. Вкладка **«Скрипты»** — команды при старте и при завершении.
7. По желанию открой **«Условия...»** и добавь требования, ветку или сделай квест общим.
8. Сохрани и выполни:

   ```
   /storyquest start chapter_1_intro
   ```

## Команды

| Команда | Назначение |
|---|---|
| `/storyquest journal` | Открыть журнал |
| `/storyquest editor` | Открыть редактор (оператор) |
| `/storyquest list` | Список id квестов |
| `/storyquest start <id> [игроки]` | Начать квест с проверкой условий |
| `/storyquest force-start <id> [игроки]` | Начать квест, обойдя условия |
| `/storyquest complete <id> [игроки]` | Завершить квест и выполнить команды завершения |
| `/storyquest objective complete <id> <n> [игроки]` | Закрыть одну цель |
| `/storyquest track <id> [игроки]` | Закрепить квест в HUD |
| `/storyquest untrack [игроки]` | Убрать трекер |
| `/storyquest reset [игроки]` | Сбросить личный прогресс квестов |
| `/storyquest reset-shared [quest]` | Сбросить общий прогресс мира |
| `/storyquest export [имя]` | Экспортировать все квесты в JSON |
| `/storyquest import <имя> [merge\|replace] [confirm]` | Импортировать квесты из JSON |
| `/storyquest dialogue <игроки> <спикер> <текст>` | Показать диалог |

## Настройки

- `config/storyquest-common.toml` — частота проверки зон, сообщения в чат, звуки,
  разрешение брать квесты из журнала.
- `config/storyquest-client.toml` — положение, масштаб, прозрачность и цвета HUD; видимость
  меток квестов, положение компаса, высота луча и дальность отображения.
- Всё доступно в игре: **Mods → StoryQuest → Config**.

## Требования

- Minecraft **1.21.1**
- **NeoForge 21.1.x** (собрано на 21.1.264)
- Java 21

Работает в одиночной игре и на выделенных серверах. Другие моды не нужны.

## Примечания

- Отслеживается один квест за раз (активными могут быть несколько).
- Дерева диалогов с выборами нет — ветвление делается условиями и командами. Оно запланировано на 1.4.0.
- Цели «принести предмет» / «убить моба» делаются командами и `/storyquest objective complete`.
- Кооп в 1.2.0+ — один общий прогресс на мир. Отдельные пати запланированы на 1.4.0.
- Экспорт и импорт JSON доступны командами; кнопок в редакторе пока нет.
- Лицензия MIT.
```

---

## Changelog 1.3.0-beta.2

```markdown
Interface themes: Paper and Book. This is a visual release; quest data and server logic are unchanged.

- Two client themes: Paper (parchment with dark ink, default) and Book (leather cover with paper
  pages, spine and bookmark).
- Theme engine with 9-slice pixel-art textures: panels, insets, slots, buttons, dividers,
  scrollbars and icons.
- Every screen is reskinned: journal, quest editor, quest form, objectives, conditions, marker
  editor, dialogue, HUD tracker and waypoint compass.
- The journal theme button cycles Paper/Book and saves the choice; the same option is in the
  client config ("Interface theme").
- Soft ink palette without drop shadows, readable on both parchment and book pages.
- White marker glyphs (arrow, check, pin, star).
- Removed the "accentColor" and "panelColor" client options; old config files keep working.

Fixed since beta.1:
- Five Russian theme strings shipped as "???" and are now correct (quest counter, journal theme
  label, theme names, config option name).
- Title and button text is no longer doubled by drop shadows.
```

## Changelog 1.2.0

```markdown
Quest prerequisites, branches and co-op.

- Quest conditions: required quests, all/any mode, hide until available, and branch locks.
- Conditions editor with search, cycle detection, missing references and mixed-scope warnings.
- The journal shows locked quests, requirements, branch state and shared quests.
- Shared co-op quests (scope = SHARED) use one world progress: any player can start the quest
  and complete its objectives; start/complete commands, messages and dialogue run for every
  online player.
- /storyquest reset-shared [quest] resets world-shared progress.
- JSON export/import: /storyquest export [name] and
  /storyquest import <name> [merge|replace] [confirm].
  Full quest definitions are exported; imports are validated, atomic and create automatic DAT
  backups (last 20 are kept).
- /storyquest force-start starts a quest while bypassing conditions for testing.
- Existing worlds load with defaults (SOLO, no requirements, no branch); no migration is needed.
- DAT remains internal storage; JSON is the exchange format.
```

## Changelog 1.1.0

```markdown
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
- New client config options: waypointsEnabled, waypointCompass, compass offsets,
  waypointBeams, waypointBeamHeight, waypointMaxDistance, waypointIconScale.
- Existing quests get default markers automatically when loaded.
```

## Changelog 1.0.0

```markdown
First release.

- In-game quest editor (K): main info, story text, objectives, scripts.
- Quest journal (J) with chapters, descriptions and objective status.
- Configurable HUD tracker (corner, offsets, scale, opacity, colors).
- Dialogue screens with a narrator and pages.
- Objectives: Manual (command) and Visit (dimension + X/Y/Z + radius).
- Commands on quest start and completion with %player% support.
- Per-player progress that survives death; quests stored in the world save.
- English and Russian localization.
- Works on dedicated servers (no client classes on the server side).
```

---

## Чек-лист перед публикацией

- [ ] `gradlew build` проходит без ошибок.
- [ ] В `build\libs` лежит свежий `storyquest-<version>.jar`.
- [ ] Версия в `gradle.properties` (`mod_version`) совпадает с версией в описании.
- [ ] В игре проверены: редактор, журнал, HUD, диалог, цель «Посещение», команды старта/финиша.
- [ ] Проверены обе темы: переключение кнопкой в журнале, сохранение выбора после перезапуска.
- [ ] Текст читаем в Paper и Book, без теней; в журнале и в конфиге нет «???».
- [ ] Проверены условия, ветки, скрытые квесты, `force-start` и сброс прогресса.
- [ ] Проверены общие квесты в коопе: старт одним игроком, цель другим, поздний вход и `reset-shared`.
- [ ] Проверены экспорт/импорт JSON, merge/replace и резервные копии.
- [ ] Проверены метки: иконка, цвет, текст, луч и компас (в т.ч. в другом измерении).
- [ ] На сервере мод запускается (без клиентских классов).
- [ ] Выбрана лицензия MIT и она же указана на площадке.
