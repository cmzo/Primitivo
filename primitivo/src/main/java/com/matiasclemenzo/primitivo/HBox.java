package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

// Fila de dos cajas lado a lado: la derecha tiene ancho fijo, la izquierda ocupa
// el resto. Ambas comparten el mismo top y se igualan a la altura mayor. La suma
// de anchos (+ gap) es el ancho que tendría una caja normal del VStack.
class HBox extends PanelBox {

    static final int GAP_H = PanelTheme.GAP_VSTACK;

    private final PanelBox left;
    private final PanelBox right;
    private final int      rightWidth;

    HBox(PanelTheme theme, PanelBox left, PanelBox right, int rightWidth) {
        super(theme, null);
        this.left       = left;
        this.right      = right;
        this.rightWidth = rightWidth;
    }

    private void assignChildren() {
        left.x      = x;
        left.width  = width - rightWidth - GAP_H;
        left.top    = top;
        left.cachedH = -1;

        right.x      = x + width - rightWidth;
        right.width  = rightWidth;
        right.top    = top;
        right.cachedH = -1;

        int h = Math.max(left.height(), right.height());
        left.forcedHeight  = h;
        right.forcedHeight = h;
    }

    @Override
    void layout(Pen pen) { /* HBox delega en sus hijos; sin layout propio */ }

    @Override
    int height() {
        assignChildren();
        return Math.max(left.height(), right.height());
    }

    @Override
    void drawShapes(ShapeRenderer s) {
        assignChildren();
        left.drawShapes(s);
        right.drawShapes(s);
    }

    @Override
    void drawText(SpriteBatch b) {
        assignChildren();
        left.drawText(b);
        right.drawText(b);
    }
}
