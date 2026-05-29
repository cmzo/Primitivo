package com.matiasclemenzo.primitivo;

class EquipBox extends PanelBox {

    private static final int SLOT_SIZE = 36;

    private final Player player;

    EquipBox(PanelTheme theme, Player player) {
        super(theme, "EQUIPAMIENTO");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        equipRow(pen, "Arma",     "(sin equipar)");
        pen.gap(PanelTheme.GAP_LINE);
        equipRow(pen, "Armadura", "(sin equipar)");
    }

    // Slot a la izquierda y etiqueta centrada verticalmente contra el slot, misma fila.
    private void equipRow(Pen pen, String label, String value) {
        int sx = pen.x;
        int sy = pen.y;  // borde superior del slot

        if (pen.mode == Pen.Mode.SHAPES) {
            pen.shapes.setColor(theme.SLOT_BG);
            pen.shapes.rect(sx, sy - SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            pen.shapes.setColor(theme.WOOD_MED);
            pen.shapes.rect(sx,                 sy - SLOT_SIZE, SLOT_SIZE, 2);
            pen.shapes.rect(sx,                 sy - 2,         SLOT_SIZE, 2);
            pen.shapes.rect(sx,                 sy - SLOT_SIZE, 2,         SLOT_SIZE);
            pen.shapes.rect(sx + SLOT_SIZE - 2, sy - SLOT_SIZE, 2,         SLOT_SIZE);
        }

        if (pen.mode == Pen.Mode.TEXT) {
            int capH  = (int) theme.font.getCapHeight();
            int textY = sy - (SLOT_SIZE - capH) / 2;  // centrado vertical en el slot
            theme.font.setColor(theme.TEXT);
            theme.font.draw(pen.batch, label + ": " + value, sx + SLOT_SIZE + 12, textY);
        }

        pen.gap(SLOT_SIZE);
    }
}
