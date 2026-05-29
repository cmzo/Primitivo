# Primitivo — Tareas

## Completado

### Fase 1 — Modelo de dominio
- [x] `Stats`, `Race` (Human, Elf, Dwarf, Orc), `CharacterClass` (Fighter, Wizard, Rogue, Healer, Ranger)
- [x] `Character` (abstracto), `Player`, `Enemy`
- [x] `Item` → `Weapon`, `Armor`, `Potion`; `Inventory`
- [x] `Skill` + 8 habilidades concretas (BolaFuego, DrenarVida, PunaladaTrapera, AtaqueDoble, Curar, GolpeSagrado, FlechaCertera, LluviaFlechas)

### Fase 2 — Capa gráfica LibGDX
- [x] Setup LibGDX 1.12.1 + LWJGL3 en Maven
- [x] `DesktopLauncher`, `PrimitivoGame`
- [x] `BattleScreen` — campo de batalla, menú de acciones, menú de habilidades, barra lateral con stats/HP/XP
- [x] Retrato del personaje en panel lateral con silhouette placeholder
- [x] `CharacterCreationScreen` — flujo Nombre → Raza → Clase con preview de stats en vivo
- [x] Menú de pausa (ESC) en batalla y overworld
- [x] `OverworldScreen` — mapa tile-based 25×22, movimiento con flechas, encuentros aleatorios en hierba
- [x] `SaveManager` — guardado automático con LibGDX Preferences (posición + estado del jugador)
- [x] `PrimitivoGame.create()` — carga partida guardada o va a creación de personaje
- [x] Sprites animados — `SpriteSheet` helper, integración en overworld y battle screen
- [x] Direcciones del sprite corregidas (craftpix top-down: row 0=sur, 1=oeste, 2=este, 3=norte)
- [x] Estructura de assets organizada (`sprites/`, `tiles/`, `ui/`, `audio/`)
- [x] Sprites del swordsman movidos a `sprites/characters/swordsman/lvl1-3/` con nombres simples (idle, walk, run, attack, hurt, death)
- [x] Sistema de derrota sin game over — reaparición en la posada con HP mínimo (20% maxHp)
- [x] Tile `INN` con visual de madera/techo rojo/puerta/ventanas; restaura HP al pisarlo
- [x] `INN_COL/INN_ROW` como constantes públicas en OverworldScreen (usadas por BattleScreen)
- [x] **Movimiento suave** — interpolación lerp en 0.12s; posición visual desacoplada de la lógica; `isKeyPressed` para movimiento continuo al mantener tecla
- [x] **XP y level-up** — `gainXp()` en Character, level-up incrementa maxHp, overlay en batalla muestra XP ganado y subida de nivel en dorado

### Fase 3 — UI y Sistema de guardado
- [x] **Slots múltiples** — 10 slots de guardado (0-9) con info de personaje (nombre, level)
- [x] **Comandos tipo Vim** en OverworldScreen:
  - `:wq` / `:qs` — guardar en slot activo + salir
  - `:q!` — salir sin guardar (inmediato)
  - `:q1` a `:q9` — guardar en slot específico + cambiar slot activo
  - `:load` — overlay de selección de slots con [0-9] para cargar
  - `:help` — mostrar referencia de comandos
- [x] **InputAdapter para keyTyped()** — captura de caracteres independiente del layout de teclado (funciona con cualquier idioma)
- [x] **Panel lateral visual** — 4 tamaños de fuente Press Start 2P (16/12/10/8px) con jerarquía clara:
  - Nombre del personaje (16px, blanco)
  - Clase (12px, marrón claro)
  - Títulos de sección: STATS, EQUIPAMIENTO, HABILIDADES, INVENTARIO (12px, teal)
  - Cuerpo: stats, skills, labels (10px, marrón oscuro)
  - Ítems en slots (8px)
- [x] **Grid de inventario** — 4 cols × 3 filas = 12 slots visuales con casilleros dibujados
- [x] **Barra HP/XP** — colores ajustados (verde oliva, ámbar, rojo tierra), labels separados de las barras
- [x] **Retrato escalado** — 130×130 marco con sprite a 2.5× + scissor test para recorte limpio (no desborda el panel)
- [x] **Paleta de colores** — parchment, madera, teal para headers, marrones para texto, consistente

### Fase 3b — Overworld visual
- [x] **Sprites de árboles** — 40+ variantes en Trees_shadow/ (Autumn, Moss, Broken, Burned, etc.)
- [x] **Renderizado con depth-sorting** — dos pasadas (árboles al norte del jugador, luego jugador, luego árboles al sur) para oclusión correcta
- [x] **Alternancia visual** — tree2/tree3 por paridad (r+c) % 2 para variedad sin cargar todas las variantes

### Fase 4 — Refactor del panel a componente común
- [x] **Arquitectura de cajas independientes** — `Pen` (cursor de layout en 3 modos: MEASURE/SHAPES/TEXT), `PanelBox` (base abstracta, mide su altura corriendo el layout — cero doble contabilidad del espaciado), `VStack` (apila cajas con dos pasadas globales shapes/text)
- [x] **`PanelTheme`** — paleta, fuentes y constantes de espaciado (PAD, GAP_LINE, GAP_VSTACK, TITLE_H, BAR_H) en un solo lugar
- [x] **Cajas concretas** — `HeaderBox`, `StatsBox`, `PortraitBox`, `EquipBox`, `SkillsBox`, `InventoryBox`; cada una toca solo su propio contenido
- [x] **`HBox`** — Stats (izq) y retrato (der) lado a lado; igualan altura, suman el ancho de una caja normal
- [x] **Pulido visual** — franja de título pegada al borde superior, retrato sin doble marco, "Lv.N" respetando el padding
- [x] **`PlayerPanel`** — componente común del panel lateral compartido por `OverworldScreen` y `BattleScreen` (antes la batalla tenía su propio panel hardcodeado con coordenadas Y absolutas y fondo de madera distinto)

---

## Próximo

### Battle screen visual
- [ ] Sprites de enemigos en batalla (al menos 3-4 tipos básicos)
- [ ] Animaciones de ataque/defensa/habilidad
- [ ] Mejor layout de la batalla — enemigo a la izquierda, jugador a la derecha, acciones abajo

### Mejoras del mapa
- [ ] Más variedad de tiles (camino de piedra, suelo de tierra, agua animada)
- [ ] Zonas con encuentros de mayor nivel alejadas del punto de inicio
- [ ] Tileset propio en lugar de ShapeRenderer simples

### Contenido de progresión
- [ ] Pantalla de looteo post-batalla — ítems dropeados al derrotar enemigos
- [ ] Inventario interactivo (tecla I o Tab) — ver, equipar, descartar ítems
- [ ] Equipamiento funcional — aplicar stats de Weapon/Armor al daño/defensa
- [ ] Curva de dificultad — enemigos por zona, no solo por nivel del jugador

---

## Backlog

### Progresión
- [ ] Curva de dificultad — enemigos más fuertes por zona, no solo por nivel del jugador
- [ ] **Pantalla de looteo post-batalla** — al derrotar un enemigo, mostrar los ítems dropeados antes de volver al overworld; el jugador elige qué tomar
- [ ] **Inventario interactivo** — pantalla dedicada (tecla I o Tab) donde se puede ver, equipar y descartar ítems de forma rápida e intuitiva; equipar debe sentirse satisfactorio (preview de stats, comparación con lo equipado)
- [ ] Equipamiento funcional — aplicar stats de Weapon/Armor al daño/defensa en batalla

### Contenido
- [ ] Más tipos de enemigos con sprites propios (Goblin, Esqueleto, Bandido, Rata Gigante)
- [ ] NPCs en el mapa con los que hablar (Q para interactuar)
- [ ] Tienda — comprar/vender ítems
- [ ] Dungeons — zonas cerradas con enemigos más difíciles y loot

### Arte
- [ ] Sprites de enemigos (al menos 2-3 básicos para la battle screen)
- [ ] Tileset propio para el overworld (grass, árboles, agua, camino en PNG)
- [ ] Múltiples frames de animación del jugador en overworld (walk vs idle vs combat)
- [ ] Animación de ataque en batalla (sprite del jugador se desplaza hacia el enemigo)
- [ ] UI icons para ítems y habilidades

### Progresión (continuación)
- [ ] Curva de dificultad — enemigos más fuertes por zona, no solo por nivel del jugador

### Técnico
- [ ] Separar lógica de combate de `BattleScreen` a una clase `BattleSystem`
- [ ] Sistema de diálogo reutilizable (caja de texto con avance por ENTER)
- [ ] Pantalla de título con opciones Nueva Partida / Cargar / Salir
- [ ] Soporte para guardado en múltiples plataformas (Android, Web via TeaVM)
- [ ] Audio — música de fondo por zona, efectos de sonido para ataques

---

## Decisiones de diseño pendientes

- **Mapa**: ¿mapa único grande con scroll, o pantallas separadas (estilo Pokémon)?
- **Muerte**: ¿penalización por derrota además del HP mínimo? (ej: perder oro/XP)
- **Clases**: ¿el jugador puede cambiar de clase al subir de nivel o es fija desde la creación?
- **Historia**: ¿hay narrativa/objetivo o es un roguelite de exploración?
