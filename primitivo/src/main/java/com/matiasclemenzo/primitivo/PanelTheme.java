package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

class PanelTheme {

    // Palette
    final Color PARCH    = new Color(0.84f, 0.73f, 0.57f, 1f);
    final Color SLOT_BG  = new Color(0.69f, 0.57f, 0.41f, 1f);
    final Color WOOD     = new Color(0.20f, 0.11f, 0.05f, 1f);
    final Color WOOD_MED = new Color(0.38f, 0.22f, 0.10f, 1f);
    final Color TEXT     = new Color(0.16f, 0.08f, 0.02f, 1f);
    final Color TEXT_DIM = new Color(0.46f, 0.32f, 0.18f, 1f);
    final Color TEAL     = new Color(0.14f, 0.54f, 0.46f, 1f);
    final Color XP_BLUE  = new Color(0.22f, 0.48f, 0.82f, 1f);

    // Fonts (references only — owned by OverworldScreen)
    final BitmapFont fontLg;   // 16px  nombre del personaje
    final BitmapFont fontMd;   // 12px  títulos de sección, clase
    final BitmapFont font;     // 10px  stats, skills, labels
    final BitmapFont fontSm;   // 8px   items dentro de slots

    // Spacing constants — tune in T7
    static final int PAD        = 6;   // padding interno de la caja (top/bottom/left/right)
    static final int GAP_LINE   = 4;   // entre líneas de texto
    static final int GAP_VSTACK = 16;  // entre cajas (más aire; deja lugar abajo para widgets futuros)
    static final int TITLE_H    = 22;  // alto de la barra de título (con aire arriba/abajo del texto)
    static final int BAR_H      = 10;  // alto barras HP/XP
    static final int MARGIN_TOP = 15;  // margen superior antes de la primera caja

    PanelTheme(BitmapFont fontLg, BitmapFont fontMd, BitmapFont font, BitmapFont fontSm) {
        this.fontLg = fontLg;
        this.fontMd = fontMd;
        this.font   = font;
        this.fontSm = fontSm;
    }

    Color hpColor(float pct) {
        if (pct > 0.5f)  return new Color(0.28f, 0.62f, 0.18f, 1f);
        if (pct > 0.25f) return new Color(0.75f, 0.55f, 0.10f, 1f);
        return                 new Color(0.72f, 0.20f, 0.12f, 1f);
    }
}
