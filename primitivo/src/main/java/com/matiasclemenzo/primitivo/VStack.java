package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.ArrayList;
import java.util.List;

class VStack {

    private final int             x;
    private final int             width;
    private final int             topY;
    private final List<PanelBox>  boxes = new ArrayList<>();
    private final PanelTheme      theme;

    VStack(int x, int width, int topY, PanelTheme theme) {
        this.x     = x;
        this.width = width;
        this.topY  = topY;
        this.theme = theme;
    }

    void add(PanelBox box) {
        box.x     = x;
        box.width = width;
        boxes.add(box);
    }

    // Asigna top a cada caja y mide las alturas. Llamar una vez después de add().
    void layoutBoxes() {
        int y = topY;
        for (PanelBox box : boxes) {
            box.top    = y;
            box.cachedH = -1;
            y -= box.height() + PanelTheme.GAP_VSTACK;
        }
    }

    // Una sola pasada global de shapes para todas las cajas
    void drawShapes(ShapeRenderer s) {
        s.begin(ShapeRenderer.ShapeType.Filled);
        for (PanelBox box : boxes) box.drawShapes(s);
        s.end();
    }

    // Una sola pasada global de texto para todas las cajas
    void drawText(SpriteBatch b) {
        b.begin();
        for (PanelBox box : boxes) box.drawText(b);
        b.end();
    }
}
