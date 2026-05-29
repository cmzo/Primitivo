package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;

class PortraitBox extends PanelBox {

    private static final int PORT_SIZE = 130;
    private final SpriteSheet idleSprite;

    PortraitBox(PanelTheme theme, SpriteSheet idleSprite) {
        super(theme, null);  // sin barra de título
        this.borderless = true;  // ya tiene su propio marco de imagen
        this.idleSprite = idleSprite;
    }

    @Override
    void layout(Pen pen) {
        int sx = pen.x + (pen.w - PORT_SIZE) / 2;  // centrado horizontal en la caja
        int sy = pen.y - PORT_SIZE;

        if (pen.mode == Pen.Mode.SHAPES) {
            pen.shapes.setColor(theme.WOOD);
            pen.shapes.rect(sx - 4, sy - 4, PORT_SIZE + 8, PORT_SIZE + 8);
            pen.shapes.setColor(theme.SLOT_BG);
            pen.shapes.rect(sx, sy, PORT_SIZE, PORT_SIZE);
        }

        if (pen.mode == Pen.Mode.TEXT) {
            int drawSz = (int)(PORT_SIZE * 2.5f);
            int off    = (drawSz - PORT_SIZE) / 2;
            pen.batch.flush();
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            HdpiUtils.glScissor(sx + 4, sy + 4, PORT_SIZE - 8, PORT_SIZE - 8);
            idleSprite.draw(pen.batch, SpriteSheet.DIR_DOWN,
                    sx - off + 4, sy - off + 4, drawSz, drawSz);
            pen.batch.flush();
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }

        pen.gap(PORT_SIZE);
    }
}
