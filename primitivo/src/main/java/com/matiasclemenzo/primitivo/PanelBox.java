package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

abstract class PanelBox {

    PanelTheme theme;
    int        x;            // x exterior izquierdo; asignado por VStack
    int        width;        // ancho exterior; asignado por VStack
    int        top;          // y del borde superior; asignado por VStack
    String     title;        // null → sin barra de título
    int        cachedH = -1;
    int        forcedHeight = -1;  // si >=0, el fondo usa esta altura (para igualar cajas de un row)
    boolean    borderless = false; // si true, no dibuja fondo ni borde (caja sin marco propio)

    PanelBox(PanelTheme theme, String title) {
        this.theme = theme;
        this.title = title;
    }

    abstract void layout(Pen pen);

    // y de arranque del cursor: con título la franja va pegada al borde superior;
    // sin título dejamos el padding superior antes del contenido.
    private int startY() {
        return (title != null) ? top : top - PanelTheme.PAD;
    }

    // Corre el layout: franja de título pegada arriba + padding, luego el contenido.
    private void runLayout(Pen pen) {
        if (title != null) {
            pen.title(title);
            pen.gap(PanelTheme.PAD);
        }
        layout(pen);
    }

    // Altura medida corriendo el layout — NUNCA se suma a mano
    int height() {
        if (cachedH < 0) {
            Pen pen = new Pen(Pen.Mode.MEASURE, x, width, startY(), theme);
            runLayout(pen);
            cachedH = (top - pen.y) + PanelTheme.PAD;
        }
        return cachedH;
    }

    void drawShapes(ShapeRenderer s) {
        int h = (forcedHeight >= 0) ? forcedHeight : height();
        if (!borderless) {
            // Fondo de la caja
            s.setColor(theme.PARCH);
            s.rect(x, top - h, width, h);
            // Borde de madera
            s.setColor(theme.WOOD);
            s.rect(x,             top - h, width, 2);
            s.rect(x,             top - 2, width, 2);
            s.rect(x,             top - h, 2,     h);
            s.rect(x + width - 2, top - h, 2,     h);
        }
        // Shapes internas
        Pen pen = new Pen(Pen.Mode.SHAPES, x, width, startY(), theme);
        pen.shapes = s;
        runLayout(pen);
    }

    void drawText(SpriteBatch b) {
        Pen pen = new Pen(Pen.Mode.TEXT, x, width, startY(), theme);
        pen.batch = b;
        runLayout(pen);
    }
}
