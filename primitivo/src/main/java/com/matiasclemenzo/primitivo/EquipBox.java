package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;

class EquipBox extends PanelBox {

    private static final int   SLOT_SIZE = 40;
    private static final Color SEL   = new Color(1f,   0.82f, 0.35f, 1f);  // resaltado dorado
    private static final Color GREEN = new Color(0.13f, 0.48f, 0.15f, 1f); // mejor (verde bosque)
    private static final Color RED   = new Color(0.72f, 0.20f, 0.12f, 1f); // peor (rojo tierra)

    int  selected = -1;   // -1 nada, 0 arma, 1 armadura (lo fija PlayerPanel)
    Item preview;         // ítem del inventario bajo el cursor (para comparar)

    private final Player player;

    EquipBox(PanelTheme theme, Player player) {
        super(theme, "EQUIPAMIENTO");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        Inventory inv = player.getInventory();
        equipRow(pen, "Arma",     inv.getEquippedWeapon(), 0);
        pen.gap(PanelTheme.GAP_LINE);
        equipRow(pen, "Armadura", inv.getEquippedArmor(),  1);
    }

    // Slot con el ícono de lo equipado + etiqueta; resaltado si está seleccionado.
    private void equipRow(Pen pen, String label, Item equipped, int slotId) {
        int sx = pen.x;
        int sy = pen.y;  // borde superior del slot

        if (pen.mode == Pen.Mode.SHAPES) {
            pen.shapes.setColor(theme.SLOT_BG);
            pen.shapes.rect(sx, sy - SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
            pen.shapes.setColor(selected == slotId ? SEL : theme.WOOD_MED);
            pen.shapes.rect(sx,                 sy - SLOT_SIZE, SLOT_SIZE, 2);
            pen.shapes.rect(sx,                 sy - 2,         SLOT_SIZE, 2);
            pen.shapes.rect(sx,                 sy - SLOT_SIZE, 2,         SLOT_SIZE);
            pen.shapes.rect(sx + SLOT_SIZE - 2, sy - SLOT_SIZE, 2,         SLOT_SIZE);
        }

        if (pen.mode == Pen.Mode.TEXT) {
            if (equipped != null) {
                TextureRegion icon = ItemIcons.get(equipped.getIconIndex());
                if (icon != null)
                    pen.batch.draw(icon, sx + 4, sy - SLOT_SIZE + 4, SLOT_SIZE - 8, SLOT_SIZE - 8);
            }
            int capH  = (int) theme.font.getCapHeight();
            int textY = sy - (SLOT_SIZE - capH) / 2;
            String name = "(vacío)";
            if (equipped instanceof Weapon) name = equipped.getName() + " (+" + ((Weapon) equipped).getAttackBonus() + " atq)";
            else if (equipped instanceof Armor) name = equipped.getName() + " (+" + ((Armor) equipped).getDefenseBonus() + " def)";
            else if (equipped != null) name = equipped.getName();
            theme.font.setColor(equipped != null ? theme.TEXT : theme.TEXT_DIM);
            theme.font.draw(pen.batch, label + ": " + name, sx + SLOT_SIZE + 12, textY);

            // Comparación: si el cursor del inventario está sobre un ítem de este
            // tipo, mostrá cuánto cambia el bonus respecto de lo equipado.
            boolean matches = (slotId == 0 && preview instanceof Weapon)
                           || (slotId == 1 && preview instanceof Armor);
            if (matches) {
                int eq = (slotId == 0)
                        ? (equipped instanceof Weapon ? ((Weapon) equipped).getAttackBonus() : 0)
                        : (equipped instanceof Armor  ? ((Armor)  equipped).getDefenseBonus() : 0);
                int pv = (slotId == 0) ? ((Weapon) preview).getAttackBonus()
                                       : ((Armor)  preview).getDefenseBonus();
                int delta = pv - eq;
                String ds = (delta > 0 ? "+" : "") + delta;
                theme.font.setColor(delta > 0 ? GREEN : delta < 0 ? RED : theme.TEXT_DIM);
                theme.font.draw(pen.batch, ds, sx, textY, pen.w - 8, Align.right, false);  // margen a la derecha
            }
        }

        pen.gap(SLOT_SIZE);
    }
}
