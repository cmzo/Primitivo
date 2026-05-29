package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;

class Pen {

    enum Mode { MEASURE, SHAPES, TEXT }

    final int        bx;      // x exterior izquierdo de la caja
    final int        bw;      // ancho exterior de la caja
    final int        x;       // x del contenido = bx + PAD
    final int        w;       // ancho del contenido = bw - 2*PAD
    int              y;       // cursor; decrece hacia abajo
    final Mode       mode;
    final PanelTheme theme;
    SpriteBatch      batch;   // sólo en modo TEXT
    ShapeRenderer    shapes;  // sólo en modo SHAPES

    Pen(Mode mode, int boxX, int boxW, int startY, PanelTheme theme) {
        this.mode  = mode;
        this.bx    = boxX;
        this.bw    = boxW;
        this.x     = boxX + PanelTheme.PAD;
        this.w     = boxW - 2 * PanelTheme.PAD;
        this.y     = startY;
        this.theme = theme;
    }

    int lineH(BitmapFont f) {
        return (int) f.getCapHeight() + PanelTheme.GAP_LINE;
    }

    // ── Operaciones de texto ─────────────────────────────────────────

    void text(BitmapFont f, Color c, String s) {
        if (mode == Mode.TEXT) {
            f.setColor(c);
            f.draw(batch, s, x, y);
        }
        y -= lineH(f);
    }

    void textRight(BitmapFont f, Color c, String s) {
        if (mode == Mode.TEXT) {
            f.setColor(c);
            f.draw(batch, s, bx, y, bw, Align.right, false);
        }
        y -= lineH(f);
    }

    // Dibuja dos cadenas en la misma línea; rightColX es la x absoluta de la columna derecha
    void text2col(BitmapFont f, Color c, String left, String right, int rightColX) {
        if (mode == Mode.TEXT) {
            f.setColor(c);
            f.draw(batch, left,  x,         y);
            f.draw(batch, right, rightColX, y);
        }
        y -= lineH(f);
    }

    // Barra de título: shape en SHAPES, texto en TEXT; siempre avanza TITLE_H
    void title(String s) {
        if (mode == Mode.SHAPES) {
            shapes.setColor(theme.WOOD_MED);
            shapes.rect(bx, y - PanelTheme.TITLE_H, bw, PanelTheme.TITLE_H);
        }
        if (mode == Mode.TEXT && s != null) {
            int capH  = (int) theme.fontMd.getCapHeight();
            int textY = y - (PanelTheme.TITLE_H - capH) / 2;
            theme.fontMd.setColor(theme.TEAL);
            theme.fontMd.draw(batch, s, x, textY);
        }
        y -= PanelTheme.TITLE_H;
    }

    // ── Operaciones de shapes ────────────────────────────────────────

    void bar(Color bg, Color fill, float pct) {
        if (mode == Mode.SHAPES) {
            shapes.setColor(bg);
            shapes.rect(x, y - PanelTheme.BAR_H, w, PanelTheme.BAR_H);
            if (pct > 0f) {
                shapes.setColor(fill);
                shapes.rect(x, y - PanelTheme.BAR_H, w * pct, PanelTheme.BAR_H);
            }
        }
        y -= PanelTheme.BAR_H;
    }

    // Grilla de slots (rows×cols, slot px de lado, gap entre slots)
    void grid(int rows, int cols, int slot, int gap) {
        if (mode == Mode.SHAPES) {
            int totalH = rows * (slot + gap);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int sx = x + c * (slot + gap);
                    int sy = y - totalH + r * (slot + gap);
                    shapes.setColor(theme.SLOT_BG);
                    shapes.rect(sx, sy, slot, slot);
                    shapes.setColor(theme.WOOD_MED);
                    shapes.rect(sx,            sy,            slot, 2);
                    shapes.rect(sx,            sy + slot - 2, slot, 2);
                    shapes.rect(sx,            sy,            2,    slot);
                    shapes.rect(sx + slot - 2, sy,            2,    slot);
                }
            }
        }
        y -= rows * (slot + gap);
    }

    // ── Operación especial: sprite con scissor (modo TEXT) ───────────

    void sprite(SpriteSheet sheet, int dir, int size) {
        if (mode == Mode.TEXT) {
            int drawSz = (int)(size * 2.5f);
            int off    = (drawSz - size) / 2;
            int sx     = x;
            int sy     = y - size;
            batch.flush();
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            HdpiUtils.glScissor(sx + 4, sy + 4, size - 8, size - 8);
            sheet.draw(batch, dir, sx - off + 4, sy - off + 4, drawSz, drawSz);
            batch.flush();
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        }
        y -= size;
    }

    // ── Desplazamiento puro ──────────────────────────────────────────

    void gap(int px) {
        y -= px;
    }
}
