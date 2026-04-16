```
╔═════════════════════════════════════════════════════════════════╗
║                                                                 ║
║ ██████╗ ██████╗ ██╗███╗   ███╗██╗████████╗██╗██╗   ██╗ ██████╗  ║
║ ██╔══██╗██╔══██╗██║████╗ ████║██║╚══██╔══╝██║██║   ██║██╔═══██╗ ║
║ ██████╔╝██████╔╝██║██╔████╔██║██║   ██║   ██║██║   ██║██║   ██║ ║
║ ██╔═══╝ ██╔══██╗██║██║╚██╔╝██║██║   ██║   ██║╚██╗ ██╔╝██║   ██║ ║
║ ██║     ██║  ██║██║██║ ╚═╝ ██║██║   ██║   ██║ ╚████╔╝ ╚██████╔╝ ║
║ ╚═╝     ╚═╝  ╚═╝╚═╝╚═╝     ╚═╝╚═╝   ╚═╝   ╚═╝  ╚═══╝   ╚═════╝  ║
║                                                                 ║
║             A simple CLI based RPG · Matias Clemenzo            ║
║                                                                 ║
╚═════════════════════════════════════════════════════════════════╝

```

## Indice

- [Descripción general](#descripción-general)
- [Mecánicas principales](#mecánicas-principales)
    - [Creación de personajes](#creación-de-personaje)
    - [Stats](#stats)
    - [Combate por turnos](#combate-por-turnos)
    - [Inventario](#inventario)
    - [Progresión](#progresión)
- [Alcance](#alcance-v10)
- [ULM](#ULM)
- [Conceptos de POO aplicados](#conceptos-de-poo-aplicados)
- [Tecnologías](#tecnologías)
- [Estado del proyecto](#estado-del-proyecto)

 
---

## Descripción general

Primitivo es un RPG de consola en el que el jugador crea un personaje eligiendo su **raza** y su **clase**, y lo lleva a través de combates por turnos contra enemigos. El foco del juego está en la **gestión del personaje**: su inventario, sus stats, sus habilidades y cómo estos elementos interactúan entre sí durante el combate.

No tiene interfaz gráfica. Toda la interacción es a través de texto en consola.

---

## Mecánicas principales

### Creación de personaje
El jugador elige:
- Una **raza** (Human, Elf, Dwarf, Orc) — cada una aplica modificadores positivos y negativos a los stats base
- Una **clase** (Fighter, Wizard, Rogue, Healer, Ranger) — define las habilidades disponibles y el stat principal que escala con el nivel

La combinación de raza y clase produce un personaje único con un perfil de stats diferente. Un Elfo Mago y un Enano Mago comparten clase, pero sus stats base difieren por la raza.

### Stats
Cada personaje tiene los siguientes atributos base, modificados por raza y equipamiento:

| Stat | Descripción |
|---|---|
| `STR` (Strength) | Daño físico |
| `DEX` (Dexterity) | Iniciativa y evasión |
| `INT` (Intelligence) | Poder mágico |
| `CON` (Constitution) | Puntos de vida máximos |
| `WIS` (Wisdom) | Resistencia a efectos de estado |


Cada raza reparte un total de +7 puntos entre sus bonos. Así ninguna raza es objetivamente mejor que otra — solo diferente.


| Stat | Human | Elf | Dwarf | Orc |
|------|-------|-----|-------|-----|
| STR  | +2    | -1  | +3    | +5  |
| DEX  | +2    | +3  | -1    | +1  |
| INT  | +1    | +3  | +1    | -2  |
| CON  | +1    | -1  | +3    | +2  |
| WIS  | +1    | +3  | +1    | +1  |

### Combate por turnos
- El orden de turno se determina por `DEX + d20` (dado de 20 caras)
- En su turno, el jugador puede: **atacar**, **usar una habilidad**, **usar un ítem** o **huir**
- El enemigo actúa con IA simple: selecciona acciones según su estado (HP, habilidades disponibles)
- Al final de cada turno completo se aplican efectos de estado (veneno, regeneración, etc.)
- El combate termina cuando uno de los dos llega a 0 HP

### Inventario
- El personaje lleva una mochila con capacidad limitada de ítems
- Los ítems se dividen en: **Weapon** (arma), **Armor** (armadura) y **Potion** (consumible)
- Equipar un arma o armadura modifica los stats del personaje en tiempo real
- Los enemigos derrotados pueden dejar loot

### Progresión
- Al ganar combates el personaje obtiene experiencia (XP)
- Al acumular suficiente XP sube de nivel, lo que mejora sus stats y puede desbloquear nuevas habilidades

---

## Alcance v1.0

Lo que **sí** pretende incluir la primera versión:

- Creación de personaje con elección de raza y clase
- Sistema de stats con modificadores por raza y equipamiento
- Combate por turnos contra un enemigo a la vez
- Inventario con ítems equipables y consumibles
- Sistema de habilidades por clase
- Efectos de estado básicos (veneno, regeneración)
- Progresión por XP y niveles

---

## ULM

El diagrama muestra las clases principales del sistema y sus relaciones. Las flechas con triángulo hueco indican **herencia**, las flechas simples indican **asociación** y el rombo indica **composición**.

```mermaid
classDiagram
    class Character {
        -String name
        -int level
        -int hp
        -int xp
        -Race race
        -CharacterClass charClass
        -Inventory inventory
        -Stats stats
        +attack(Enemy) void
        +takeDamage(int) void
        +isAlive() boolean
        +levelUp() void
    }

    class Stats {
        -int strength
        -int dexterity
        -int intelligence
        -int constitution
        -int wisdom
        +getModifier(String stat) int
        +applyModifier(String stat, int value) void
    }

    class Race {
        <<abstract>>
        -String name
        -String description
        +applyModifiers(Stats) void
    }

    class CharacterClass {
        <<abstract>>
        -String className
        -String primaryStat
        -List~Skill~ skills
        +useSkill(Skill, Enemy) void
        +levelUp() void
        +getAvailableSkills() List
    }

    class Inventory {
        -List~Item~ items
        -Weapon equippedWeapon
        -Armor equippedArmor
        +addItem(Item) void
        +removeItem(Item) void
        +equip(Item) void
        +listItems() void
    }

    class Item {
        <<abstract>>
        -String name
        -String description
        -int value
        +use(Character) void
    }

    class Skill {
        -String name
        -int manaCost
        -int damage
        -String effect
        +activate(Character, Enemy) void
        +isAvailable(Character) boolean
    }

    class Enemy {
        -String name
        -int hp
        -Stats stats
        -List~Item~ lootTable
        -int xpReward
        +takeDamage(int) void
        +isAlive() boolean
        +dropLoot() List
        +chooseAction(Character) void
    }

    class BattleSystem {
        -Character player
        -Enemy enemy
        -int turn
        +startBattle() void
        +playerTurn() void
        +enemyTurn() void
        +applyStatusEffects() void
        +checkBattleEnd() boolean
    }

    class Human {
        +applyModifiers(Stats) void
    }
    class Elf {
        +applyModifiers(Stats) void
    }
    class Dwarf {
        +applyModifiers(Stats) void
    }
    class Orc {
        +applyModifiers(Stats) void
    }

    class Fighter { }
    class Wizard { }
    class Rogue { }
    class Healer { }
    class Ranger { }

    class Weapon {
        -int attackBonus
        -String damageType
    }
    class Armor {
        -int defenseBonus
    }
    class Potion {
        -int healAmount
    }

    Character --> Stats : tiene
    Character --> Race : tiene
    Character --> CharacterClass : tiene
    Character *-- Inventory : compone

    Race <|-- Human
    Race <|-- Elf
    Race <|-- Dwarf
    Race <|-- Orc

    CharacterClass <|-- Fighter
    CharacterClass <|-- Wizard
    CharacterClass <|-- Rogue
    CharacterClass <|-- Healer
    CharacterClass <|-- Ranger

    CharacterClass --> Skill : contiene

    Inventory *-- Item : contiene
    Item <|-- Weapon
    Item <|-- Armor
    Item <|-- Potion

    BattleSystem --> Character : gestiona
    BattleSystem --> Enemy : gestiona
```

---

## Conceptos de POO aplicados

| Concepto | Dónde se aplica |
|---|---|
| **Clases y objetos** | Cada entidad del juego (personaje, enemigo, ítem) es una clase |
| **Encapsulamiento** | Los atributos son `private`, accesibles solo mediante métodos |
| **Herencia** | `Race` y `CharacterClass` son abstractas; las razas y clases concretas las extienden |
| **Polimorfismo** | `applyModifiers()` se comporta distinto según la raza; `use()` distinto según el ítem |
| **Composición** | `Character` contiene un `Inventory`; `Inventory` contiene `Item`s |

---

## Tecnologías

- Java 21.0.10 / Maven 3.9.14
- Consola (Wezterm)/ Scanner para input
- GIT / Github
- VS Code
- API: [D&D 5e SRD API](https://5e-bits.github.io/docs/)

---

## Cronograma

| # | Tarea | Descripción | Estado |
|---|-------|-------------|--------|
| 1 | Documentación inicial | README, UML y diseño de clases | ✅ Completado |
| 2 | `Stats` | Atributos base y modificadores | ✅ Completado |
| 3 | `Race` | Clase abstracta base | ✅ Completado |
| 4 | `Human`, `Elf`, `Dwarf`, `Orc` | Subclases de `Race` con modificadores | ✅ Completado |
| 5 | `Item` | Clase abstracta base de ítems | ✅ Completado |
| 6 | `Weapon`, `Armor`, `Potion` | Subclases de `Item` | ✅ Completado |
| 7 | `Inventory` | Gestión de ítems y equipamiento | ✅ Completado |
| 8 | `CharacterClass` | Clase abstracta con habilidades y levelUp | ✅ Completado |
| 9 | `Character` | Clase principal del personaje | ✅ Completado |
| 10 | `Skill` | Habilidades con `activate()` e `isAvailable()` | 🚧 En progreso |
| 11 | `Enemy` | Clase con IA simple y loot | ⏳ Pendiente |
| 12 | `Fighter`, `Wizard`, `Rogue`, `Healer`, `Ranger` | Subclases de `CharacterClass` | ⏳ Pendiente |
| 13 | `BattleSystem` | Lógica de combate por turnos | ⏳ Pendiente |
| 14 | Scanner / UI | Creación de personaje por consola | ⏳ Pendiente |
| 15 | Integración | Conectar todas las clases y probar flujo completo | ⏳ Pendiente |
| 16 | Javadoc | Documentar todas las clases | 🚧 En progreso |
| 17 | Testing | Pruebas básicas de las clases principales | ⏳ Pendiente |

