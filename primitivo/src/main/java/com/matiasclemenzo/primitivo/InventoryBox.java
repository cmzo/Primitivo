package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;

class InventoryBox extends PanelBox {

    private static final int   GAP   = 5;
    private static final int   GCOLS = 6;   // columnas (llenan el ancho de la sección)
    private static final int   GROWS = 2;   // filas
    private static final Color SEL   = new Color(1f, 0.82f, 0.35f, 1f);  // resaltado dorado

    int selected = -1;   // -1 nada, 0..11 slot resaltado (lo fija PlayerPanel)

    private final Player player;

    InventoryBox(PanelTheme theme, Player player) {
        super(theme, "INVENTARIO");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        // El lado del slot se deriva del ancho disponible para llenar la sección.
        int slot   = (pen.w - (GCOLS - 1) * GAP) / GCOLS;
        int top    = pen.y;
        int totalH = GROWS * slot + (GROWS - 1) * GAP;

        if (pen.mode == Pen.Mode.SHAPES) {
            for (int row = 0; row < GROWS; row++) {
                for (int col = 0; col < GCOLS; col++) {
                    int sx = pen.x + col * (slot + GAP);
                    int sy = top - slot - row * (slot + GAP);  // fila 0 = arriba
                    boolean sel = (row * GCOLS + col) == selected;
                    pen.shapes.setColor(theme.SLOT_BG);
                    pen.shapes.rect(sx, sy, slot, slot);
                    pen.shapes.setColor(sel ? SEL : theme.WOOD_MED);
                    pen.shapes.rect(sx,            sy,            slot, 2);
                    pen.shapes.rect(sx,            sy + slot - 2, slot, 2);
                    pen.shapes.rect(sx,            sy,            2,    slot);
                    pen.shapes.rect(sx + slot - 2, sy,            2,    slot);
                }
            }
        }

        if (pen.mode == Pen.Mode.TEXT) {
            List<Item> items = player.getInventoryItems();
            int pad = Math.max(4, slot / 8);
            for (int i = 0; i < Math.min(items.size(), GCOLS * GROWS); i++) {
                int col = i % GCOLS;
                int row = i / GCOLS;
                int sx  = pen.x + col * (slot + GAP);
                int sy  = top - slot - row * (slot + GAP);
                Item item = items.get(i);
                TextureRegion icon = ItemIcons.get(item.getIconIndex());
                if (icon != null) {
                    pen.batch.draw(icon, sx + pad, sy + pad, slot - 2 * pad, slot - 2 * pad);
                } else {
                    String nm = item.getName();
                    if (nm.length() > 5) nm = nm.substring(0, 5);
                    theme.fontSm.setColor(theme.TEXT);
                    theme.fontSm.draw(pen.batch, nm, sx + 4, sy + slot - 6);
                }
            }
        }

        pen.gap(totalH);
    }
}
