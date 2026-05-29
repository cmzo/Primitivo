# Refactor del panel lateral (OverworldScreen) — Plan de ejecución

> Handoff para Sonnet. Objetivo: convertir el panel lateral derecho en un conjunto
> de **cajas independientes apiladas verticalmente**, mantenibles y sin doble
> contabilidad del espaciado.

## Contexto / por qué

El `drawSidePanel()` actual ya partió las secciones en clases internas
(`PanelHeader`, `PanelPortrait`, `PanelStats`, `PanelEquip`, `PanelSkills`,
`PanelInventory`), pero **reproduce el problema que se quería eliminar**:

1. **Doble contabilidad del espaciado.** Cada sección suma su `height` a mano en
   el constructor (`height = 13 + 6 + 11 + ...`) y vuelve a derivar las mismas
   posiciones inline en `draw()` (`tempY -= 13 + 6; ...`). Cambiar un gap obliga a
   tocar dos lugares sincronizados. **Esto es lo que hay que matar.**
2. **No hay caja real.** Las secciones dibujan sobre el fondo compartido; no hay
   contenedor visible con bounds propios.
3. **Constructores con 8 colores a mano**, repetidos por sección.

## Decisión de diseño tomada por el usuario

**Cajas VISIBLES**: cada sección tiene su propio fondo + borde de madera + barra de
título. Estilo inventario clásico RPG. Apiladas una debajo de otra, mismo ancho
que el contenido actual del panel (`CW`, a partir de `CX`).

```
┌─ STATS ──────────────┐
│ STR 12      INT 11   │
│ DEX 13      CON 11   │
│ WIS 11               │
│ HP ███████░░  59/59  │
└──────────────────────┘
┌─ EQUIPAMIENTO ───────┐
│ [ ] Arma             │
│ [ ] Armadura         │
└──────────────────────┘
```

## Arquitectura objetivo

Clases nuevas, todas en `com.matiasclemenzo.primitivo` (paquete plano, como el
resto del código fuente):

### 1. `PanelTheme`
Objeto único con TODO lo compartido. Se construye una vez en `OverworldScreen` y
se pasa entero a cada caja (mata los constructores de 8 args).
- Paleta: `PARCH, SLOT_BG, WOOD, WOOD_MED, TEXT, TEXT_DIM, TEAL, XP_BLUE`
  (mismos valores que hoy en `drawSidePanel`).
- Fuentes: `fontLg, fontMd, font, fontSm` (referencias a las de OverworldScreen).
- Constantes de espaciado en UN solo lugar:
  `PAD` (padding interno de la caja), `GAP_LINE` (entre líneas de texto),
  `GAP_VSTACK` (entre cajas), `TITLE_H` (alto de la barra de título),
  `BAR_H` (alto barras HP/XP).
- Helper `hpColor(float pct)` movido aquí (hoy está en OverworldScreen).

### 2. `Pen` — cursor de layout (la pieza clave)
Elimina la doble contabilidad. Un mismo método `layout(Pen)` por caja se corre en
tres modos; el cursor avanza idéntico en los tres, así **la altura se MIDE, nunca
se suma a mano**, y las posiciones siempre coinciden.

```java
enum Mode { MEASURE, SHAPES, TEXT }

class Pen {
    final int x, width;     // origen del contenido (x=CX+PAD, width=CW-2*PAD)
    int y;                  // cursor, decrece
    Mode mode;
    PanelTheme theme;
    SpriteBatch batch;      // sólo en modo TEXT
    ShapeRenderer shapes;   // sólo en modo SHAPES

    // Operaciones de TEXTO (sólo dibujan en modo TEXT; siempre avanzan el cursor)
    void text(BitmapFont f, Color c, String s);
    void textRight(BitmapFont f, Color c, String s);     // alineado a la derecha
    void text2col(BitmapFont f, Color c, String l, String r, int rightColX);
    void title(String s);   // barra de título: shape en SHAPES, texto en TEXT, avanza TITLE_H

    // Operaciones de SHAPES (sólo dibujan en modo SHAPES; siempre avanzan)
    void bar(Color bg, Color fill, float pct);           // avanza BAR_H
    void slotRow(int size, int count, int labelGapX);    // casilleros 36/40px
    void grid(int rows, int cols, int slot, int gap);    // grilla inventario

    // Operación especial (portrait, modo TEXT con scissor); avanza por su alto
    void sprite(SpriteSheet sheet, int dir, int size);

    void gap(int px);       // sólo avanza
}
```
Cada método sabe si es op de "texto" o de "shape" y sólo ejecuta el dibujo en el
modo correspondiente, **pero siempre avanza `y`**. Los gaps salen de `theme`, no
de literales sueltos.

### 3. `PanelBox` — base abstracta de la caja
```java
abstract class PanelBox {
    PanelTheme theme;
    int x, width;           // los fija el VStack
    int top;                // y del borde superior; lo fija el VStack
    String title;

    // ÚNICO lugar donde vive el layout de la caja:
    abstract void layout(Pen pen);

    // Altura = se mide corriendo el layout, NO se suma a mano:
    int height() {
        Pen pen = new Pen(Mode.MEASURE, x, width, contentTop(), theme);
        pen.title(title);          // si tiene título
        layout(pen);
        return (top - pen.y) + theme.PAD;   // incluye padding inferior
    }

    void drawShapes(ShapeRenderer s) {
        // 1) caja exterior: fondo + borde de madera (usa height() ya conocido)
        // 2) Pen SHAPES corre title()+layout() para shapes internos
    }
    void drawText(SpriteBatch b) {
        // Pen TEXT corre title()+layout() para textos internos
    }
}
```

### 4. `VStack` — contenedor vertical
```java
class VStack {
    int x, width, top;       // CX, CW, H - MARGIN_TOP
    List<PanelBox> boxes;

    void add(PanelBox b);
    void layoutBoxes();      // asigna x/width/top a cada caja, top -= height()+GAP_VSTACK
    void drawShapes(ShapeRenderer s);  // un solo begin/end para todas
    void drawText(SpriteBatch b);      // un solo begin/end para todas
}
```
Esto también arregla el bug recurrente `begin must be called first`: dos pasadas
globales (shapes, luego text), no begin/end por sección.

### 5. Cajas concretas (subclases de `PanelBox`)
- `HeaderBox` — nombre (fontLg) + `Lv.N` a la derecha + clase (fontMd). **Sin
  título de barra** (es la cabecera). Considerar `title = null` y que `layout`
  dibuje directo.
- `PortraitBox` — el retrato 130×130 con scissor (mover la lógica actual de
  `PanelPortrait`). Puede ir SIN barra de título.
- `StatsBox` — title "STATS"; `text2col` para STR/INT, DEX/CON; WIS; `bar` HP; `bar` XP.
- `EquipBox` — title "EQUIPAMIENTO"; `slotRow` + label "Arma"/"Armadura".
- `SkillsBox` — title "HABILIDADES"; loop de skills con `text2col` (nombre / usos).
- `InventoryBox` — title "INVENTARIO"; `grid(3,4,40,4)` + nombres de items.

### 6. `OverworldScreen.drawSidePanel()` queda así
```java
private void drawSidePanel() {
    // fondo del panel + marco madera 5px (igual que hoy)
    PanelTheme theme = new PanelTheme(fontLg, fontMd, font, fontSm);
    VStack stack = new VStack(CX, CW, H - theme.MARGIN_TOP);
    stack.add(new HeaderBox(theme, player));
    stack.add(new PortraitBox(theme, player, idleSprite));
    stack.add(new StatsBox(theme, player));
    stack.add(new EquipBox(theme, player));
    stack.add(new SkillsBox(theme, player));
    stack.add(new InventoryBox(theme, player));
    stack.layoutBoxes();
    stack.drawShapes(shapes);
    stack.drawText(batch);
}
```

---

## Tareas ordenadas (cada una compila antes de pasar a la siguiente)

- [ ] **T1 — `PanelTheme`.** Crear la clase con paleta + fuentes + constantes +
  `hpColor`. Quitar `hpColor` de OverworldScreen (o dejar un delegado). `mvn compile`.

- [ ] **T2 — `Pen`.** Implementar el cursor con los tres modos y todas las
  operaciones. Probar mentalmente que MEASURE y TEXT/SHAPES avanzan idéntico.
  Las ops de texto usan `font.getCapHeight()`/`getLineHeight()` para el alto real,
  no literales mágicos.

- [ ] **T3 — `PanelBox` + `VStack`.** Base abstracta con `height()` por medición y
  render en dos fases; contenedor que apila y hace los begin/end globales.

- [ ] **T4 — `StatsBox` primero** (es la más representativa: texto, 2 columnas,
  barras). Integrarla sola en `drawSidePanel` junto a un stub de las demás para
  ver una caja real renderizada. Ajustar `PAD`, `TITLE_H`, `GAP_LINE` en `theme`
  hasta que se vea bien. **Verificar visualmente con `mvn exec:exec`.**

- [ ] **T5 — Resto de cajas**: `HeaderBox`, `PortraitBox`, `EquipBox`,
  `SkillsBox`, `InventoryBox`. Una por una, compilando.

- [ ] **T6 — Cablear `drawSidePanel`** al VStack completo. Borrar las clases
  internas viejas (`PanelHeader`…`PanelInventory`) y el código muerto.

- [ ] **T7 — Pulido visual.** Correr el juego, ajustar SÓLO constantes en
  `PanelTheme` (PAD, GAP_LINE, GAP_VSTACK, TITLE_H). Confirmar que ningún texto
  toca bordes y que las cajas no se solapan ni se salen del panel (alto total de
  las 6 cajas + gaps debe caber en `H`).

## Reglas / criterios de aceptación

- **CERO alturas sumadas a mano.** Si ves `height = a + b + c...`, está mal: la
  altura se mide con el Pen. (Excepción: constantes atómicas como `TITLE_H`,
  `BAR_H`, tamaño de slot — esas son datos, no sumas de layout.)
- **Los gaps viven sólo en `PanelTheme`.** Nada de literales de espaciado sueltos
  dentro de las cajas.
- **Cada caja toca sólo su propio contenido.** Cambiar `StatsBox` no debe afectar a
  ninguna otra.
- **Dos pasadas globales** (shapes, luego text). Ninguna caja hace su propio
  begin/end de shapes o batch.
- Compila en cada tarea (`mvn compile`) y se ve bien al final (`mvn exec:exec`).
- macOS: lanzar siempre con `mvn exec:exec` (no `exec:java`).

## Referencia rápida de constantes/valores actuales (para no perderlos)

- Panel: `PX`, `W_PANEL=480`, `H=720`, contenido en `CX=PX+18`, `CW=W_PANEL-36`.
- Paleta (valores exactos): ver `drawSidePanel()` actual en `OverworldScreen.java`.
- Retrato: 130×130, sprite a 2.5× con `HdpiUtils.glScissor`, `SpriteSheet.DIR_DOWN`.
- Inventario: grid 4 cols × 3 filas, slot 40px, gap 4px.
- Equip slots: 36×36, label con offset `CX+46`.
- Barras HP/XP: alto 10px, ancho `CW`.
