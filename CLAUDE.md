# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Primitivo** is a top-down pixel-art RPG in Java using LibGDX. The player creates a character (race + class), explores a tile-based overworld, and fights enemies in turn-based battles. See `TAREAS.md` for the full task backlog.

Source lives under `primitivo/` (Maven project).

## Commands

All commands run from `primitivo/`:

```bash
mvn compile        # compile
mvn test           # run tests
mvn package        # build JAR
mvn exec:exec      # launch the game (forks a new JVM with -XstartOnFirstThread, required on macOS for GLFW)
```

Run a single test class:
```bash
mvn test -Dtest=AppTest
```

## Architecture

### Domain model (no LibGDX dependency)

**Race hierarchy** — `Race` (abstract) → `Human`, `Elf`, `Dwarf`, `Orc`. Each overrides `applyModifiers(Stats)`. `applyModifiers` has an empty default body in the base class (intentional no-op).

**CharacterClass hierarchy** — `CharacterClass` (abstract) → `Fighter`, `Wizard`, `Rogue`, `Healer`, `Ranger`. Holds `List<Skill>` and `primaryStat` string used by `levelUp(Stats)`.

**Skill** — abstract class; default `activate()` rolls d20 (≥10 hits). Concrete subclasses: `BolaFuego`, `DrenarVida`, `PunaladaTrapera`, `AtaqueDoble`, `Curar`, `GolpeSagrado`, `FlechaCertera`, `LluviaFlechas`.

**Item hierarchy** — `Item` (abstract) → `Weapon`, `Armor`, `Potion`. `Inventory` holds `List<Item>` plus equipped `Weapon`/`Armor` references; `equipItem()` uses `instanceof`.

**Stats** — plain value object. Stat names are plain strings (`"strength"`, `"dexterity"`, `"intelligence"`, `"constitution"`, `"wisdom"`); `getModifier()`/`applyModifier()` switch on them. Return `-1` on unknown.

**Character** (abstract) → **Player** (concrete). **Enemy** is a standalone concrete class; `chooseAction()` doubles damage below 50 HP.

### LibGDX layer

Entry point: `DesktopLauncher` → `PrimitivoGame` (extends `Game`) → screens.

**Screen flow:**
```
PrimitivoGame.create()
  ├─ save exists → OverworldScreen(player, col, row)
  └─ no save    → CharacterCreationScreen
                      └─ on confirm → OverworldScreen
                                          └─ on encounter → BattleScreen(returnScreen=this)
                                                                ├─ win  → savePlayerState → returnScreen.show()
                                                                └─ lose → deleteSave → CharacterCreationScreen
```

**Screens:**
- `CharacterCreationScreen` — step flow NAME→RACE→CLASS, live stat preview, keyboard name input via `isKeyJustPressed(Keys.A–Z)`
- `OverworldScreen` — 25×22 tile map (TILE=32px), arrow key movement, 20% encounter chance on grass tiles, auto-save on each step, pause menu (ESC)
- `BattleScreen` — 4-state machine (PLAYER_MENU → AFTER_PLAYER_ACTION → enemy turn → BATTLE_END), skill sub-menu, pause overlay

**Rendering:** `ShapeRenderer` for filled rectangles (map tiles, HP bars, UI backgrounds), `SpriteBatch`+`BitmapFont` for text and sprites. Camera: `OrthographicCamera.setToOrtho(false, 1280, 720)`. Never mix `shapes.begin()` and `batch.begin()` — always end one before starting the other.

**Sprites:** `SpriteSheet` helper wraps a `Texture`, advances a frame timer each `render(delta)`, and extracts `TextureRegion` frames from a grid. Direction rows for the craftpix top-down swordsman: `DIR_DOWN=0`, `DIR_LEFT=1`, `DIR_RIGHT=2`, `DIR_UP=3`. Overworld player drawn at 3×TILE (96×96) centered.

**Save system:** `SaveManager` uses LibGDX `Preferences` (`primitivo_save`). Saves: position, HP, XP, level, stats, race, class. `save()` writes everything; `savePlayerState()` updates HP/XP/stats without touching position; `deleteSave()` clears on game over.

### Assets structure

Working directory for `exec:exec` is `primitivo/assets/` (set in `pom.xml`). Paths passed to `Gdx.files.internal()` are relative to that directory.

```
assets/
  sprites/
    characters/        ← player character sprite sheets
    enemies/           ← enemy sprite sheets
  tiles/
    overworld/         ← terrain tile textures
  ui/
    icons/             ← item/skill icons
    fonts/             ← bitmap fonts
  audio/
    music/
    sfx/
  craftpix-net-180537-free-swordsman-1-3-level-pixel-top-down-sprite-character/
    PNG/Swordsman_lvl1/Without_shadow/   ← current player sprites (64×64 frames, 4 rows)
```

## macOS note

LWJGL3/GLFW requires the main thread. Use `mvn exec:exec` (forks JVM with `-XstartOnFirstThread`). `mvn exec:java` runs on a secondary thread and crashes with `IllegalStateException`.

## Known gaps / next priorities

See `TAREAS.md` for the full task list. Short version of what's next:
- Save slots + Vim-style commands (`:s`, `:qs`, `:q!`)
- Defeat → respawn at inn instead of game over
- XP/level-up flow
- Tile textures replacing `ShapeRenderer` rectangles
