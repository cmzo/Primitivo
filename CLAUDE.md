# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Primitivo** is a CLI-based turn-by-turn RPG in Java. The player creates a character (choosing race + class), manages an inventory, and fights enemies in text-only console combat. Source lives under `primitivo/` (Maven project).

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

The design uses two inheritance trees and one composition chain:

**Race hierarchy** — `Race` (abstract) → `Human`, `Elf`, `Dwarf`, `Orc`. Each concrete race overrides `applyModifiers(Stats)` to add/subtract from the five stats. Note: `applyModifiers` is not marked `abstract` in the base class — the empty body is intentional as a default no-op, but all four subclasses are expected to override it.

**Item hierarchy** — `Item` (abstract) → `Weapon`, `Armor`, `Potion`. Each overrides `use(Character)`. `Inventory` holds a `List<Item>` plus separate references to the currently equipped `Weapon` and `Armor`; equipping is handled by `Inventory.equipItem()` via `instanceof` checks.

**CharacterClass hierarchy** — `CharacterClass` (abstract) → `Fighter`, `Wizard`, `Rogue`, `Healer`, `Ranger` (concrete subclasses not yet implemented). Holds a `List<Skill>` and a `primaryStat` string used by `levelUp(Stats)` to increment that stat by 1.

**Skill** — abstract class with a default `activate()` that rolls a d20 (≥10 hits). Concrete skill subclasses override this for class-specific behavior.

**Stats** — plain value object. Stat names are plain strings (`"strength"`, `"dexterity"`, `"intelligence"`, `"constitution"`, `"wisdom"`); both `getModifier()` and `applyModifier()` use a switch on these strings. Return `-1` on unknown stat.

**Character** — abstract class (concrete player class not yet created). Holds `Race`, `CharacterClass`, `Inventory`, and `Stats`. `levelUp()` delegates to `CharacterClass.levelUp(stats)`.

**Enemy** — concrete standalone class. Simple AI in `chooseAction()`: attacks normally above 50 HP, doubles damage below it.

**BattleSystem** — planned but not yet implemented.

## LibGDX layer

The game uses **LibGDX 1.12.1** with the LWJGL3 desktop backend.

Entry point: `DesktopLauncher` → `PrimitivoGame` (extends `Game`) → `BattleScreen` (implements `Screen`).

`BattleScreen` owns the render loop. It uses `ShapeRenderer` for placeholder rectangles (sprites, HP bars) and `BitmapFont`/`SpriteBatch` for text. Input is polled via `Gdx.input.isKeyJustPressed()` each frame. The screen has four states: `PLAYER_MENU`, `AFTER_PLAYER_ACTION`, `AFTER_ENEMY_ACTION`, `BATTLE_END`.

macOS note: LWJGL3/GLFW requires the first thread. `exec:exec` in `pom.xml` forks a new JVM with `-XstartOnFirstThread`; `exec:java` (same JVM, secondary thread) will crash with an `IllegalStateException`.

## Known gaps

- `CharacterClass.useSkill()` has a placeholder comment — skill dispatch not implemented.
- Only `Fighter` exists as a concrete `CharacterClass`. `Wizard`, `Rogue`, `Healer`, `Ranger` are pending.
- No `BattleSystem` class yet — combat logic currently lives in `BattleScreen` directly.
