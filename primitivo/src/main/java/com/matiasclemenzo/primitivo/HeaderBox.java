package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.utils.Align;

class HeaderBox extends PanelBox {

    private final Player player;

    HeaderBox(PanelTheme theme, Player player) {
        super(theme, null);  // sin barra de título — es la cabecera
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        // Nombre (izquierda) y Lv.N (derecha) en la misma fila
        if (pen.mode == Pen.Mode.TEXT) {
            theme.fontLg.setColor(theme.TEXT);
            theme.fontLg.draw(pen.batch, player.getName(), pen.x, pen.y);
            theme.fontMd.setColor(theme.TEXT_DIM);
            theme.fontMd.draw(pen.batch, "Lv." + player.getLevel(),
                    pen.x, pen.y, pen.w, Align.right, false);
        }
        pen.gap(pen.lineH(theme.fontLg));
        pen.text(theme.fontMd, theme.TEXT_DIM, player.getClassName());
    }
}
