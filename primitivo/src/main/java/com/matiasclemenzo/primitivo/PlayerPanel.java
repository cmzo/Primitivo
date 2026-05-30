package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

// Panel lateral del jugador, compartido por todas las pantallas (overworld, batalla…).
// Construye el tema, las cajas y el VStack una vez; lee el estado del Player en vivo
// al dibujar. Posee sus propias fuentes y se encarga de liberarlas en dispose().
class PlayerPanel {

    // Geometría del panel — idéntica en todas las pantallas (área de juego 800px + panel 480px)
    static final int W_PANEL = 480;
    static final int H       = 720;
    static final int PX      = 800;
    static final int CX      = PX + 18;
    static final int CW      = W_PANEL - 36;

    private static final int PORTRAIT_W = 160;

    private final PanelTheme   theme;
    private final VStack       stack;
    private final EquipBox     equipBox;
    private final InventoryBox invBox;
    private final BitmapFont font;    // 10px
    private final BitmapFont fontMd;  // 12px
    private final BitmapFont fontLg;  // 16px
    private final BitmapFont fontSm;  // 8px

    PlayerPanel(Player player, SpriteSheet portraitSprite) {
        font   = Fonts.build(10);
        fontMd = Fonts.build(12);
        fontLg = Fonts.build(16);
        fontSm = Fonts.build(8);

        theme    = new PanelTheme(fontLg, fontMd, font, fontSm);
        equipBox = new EquipBox(theme, player);
        invBox   = new InventoryBox(theme, player);

        stack = new VStack(CX, CW, H - PanelTheme.MARGIN_TOP, theme);
        stack.add(new HeaderBox(theme, player));
        stack.add(new HBox(theme,
                new StatsBox(theme, player),
                new PortraitBox(theme, portraitSprite), PORTRAIT_W));
        stack.add(equipBox);
        stack.add(new SkillsBox(theme, player));
        stack.add(invBox);
        stack.layoutBoxes();

        // Precargá los íconos de los ítems actuales (evita crear texturas dentro del batch)
        for (Item it : player.getInventoryItems()) ItemIcons.get(it.getIconIndex());
    }

    void draw(ShapeRenderer shapes, SpriteBatch batch) {
        // Fondo del panel + marco de madera exterior
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(theme.PARCH);
        shapes.rect(PX, 0, W_PANEL, H);
        shapes.setColor(theme.WOOD);
        shapes.rect(PX,               0,     5, H);
        shapes.rect(PX + W_PANEL - 5, 0,     5, H);
        shapes.rect(PX,               0,     W_PANEL, 5);
        shapes.rect(PX,               H - 5, W_PANEL, 5);
        shapes.end();

        stack.drawShapes(shapes);
        stack.drawText(batch);
    }

    // Resalta el slot enfocado en modo inventario.
    // sel: -1 nada · 0..11 inventario · 100 arma equipada · 101 armadura equipada.
    void setSelection(int sel) {
        invBox.selected   = (sel >= 0 && sel <= 11) ? sel : -1;
        equipBox.selected = (sel == 100) ? 0 : (sel == 101) ? 1 : -1;
    }

    // Ítem del inventario bajo el cursor, para mostrar la comparación en EquipBox.
    void setPreview(Item item) {
        equipBox.preview = item;
    }

    void dispose() {
        font.dispose();
        fontMd.dispose();
        fontLg.dispose();
        fontSm.dispose();
    }
}
