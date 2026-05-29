package com.matiasclemenzo.primitivo;

import java.util.List;

class InventoryBox extends PanelBox {

    private static final int SLOT  = 40;
    private static final int GAP   = 4;
    private static final int GCOLS = 4;
    private static final int GROWS = 3;

    private final Player player;

    InventoryBox(PanelTheme theme, Player player) {
        super(theme, "INVENTARIO");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        int gridTop = pen.y;
        int totalH  = GROWS * (SLOT + GAP);

        if (pen.mode == Pen.Mode.SHAPES) {
            for (int r = 0; r < GROWS; r++) {
                for (int c = 0; c < GCOLS; c++) {
                    int sx = pen.x + c * (SLOT + GAP);
                    int sy = gridTop - totalH + r * (SLOT + GAP);
                    pen.shapes.setColor(theme.SLOT_BG);
                    pen.shapes.rect(sx, sy, SLOT, SLOT);
                    pen.shapes.setColor(theme.WOOD_MED);
                    pen.shapes.rect(sx,            sy,            SLOT, 2);
                    pen.shapes.rect(sx,            sy + SLOT - 2, SLOT, 2);
                    pen.shapes.rect(sx,            sy,            2,    SLOT);
                    pen.shapes.rect(sx + SLOT - 2, sy,            2,    SLOT);
                }
            }
        }

        if (pen.mode == Pen.Mode.TEXT) {
            List<Item> items = player.getInventoryItems();
            for (int i = 0; i < Math.min(items.size(), GCOLS * GROWS); i++) {
                int c  = i % GCOLS;
                int r  = i / GCOLS;
                int sx = pen.x + c * (SLOT + GAP);
                int sy = gridTop - totalH + r * (SLOT + GAP);
                String nm = items.get(i).getName();
                if (nm.length() > 4) nm = nm.substring(0, 4);
                theme.fontSm.setColor(theme.TEXT);
                theme.fontSm.draw(pen.batch, nm, sx + 3, sy + 22);
            }
        }

        pen.gap(totalH);
    }
}
