package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;

public class CharacterCreationScreen implements Screen {

    private enum Step { NAME, RACE, CLASS }

    private static final int SCREEN_W = 1280;
    private static final int SCREEN_H = 720;
    private static final int SPLIT    = 420;  // left panel width

    private static final Race[]   RACES       = { new Human(), new Elf(), new Dwarf(), new Orc() };
    private static final String[] RACE_LABELS = { "Humano", "Elfo", "Enano", "Orco" };
    private static final String[] RACE_DESC   = {
        "Adaptables y ambiciosos. Bonificacion a todos los stats.",
        "Antiguos como los bosques. Agiles e inteligentes.",
        "Forjados en piedra y fuego. Fuertes y resistentes.",
        "Salvajes y poderosos. Fuerza bruta incomparable."
    };

    private static final String[] CLASS_LABELS = { "Fighter", "Mago", "Picaro", "Sanador", "Explorador" };
    private static final boolean[] CLASS_AVAIL  = { true, true, true, true, true };
    private static final String[] CLASS_DESC    = {
        "Combatiente cuerpo a cuerpo.\nPrimario: STR\nHabilidades: Golpe Certero, Grito de Guerra.",
        "Hechicero arcano.\nPrimario: INT\nHabilidades: Bola de Fuego, Drenar Vida.",
        "Picaro agil.\nPrimario: DEX\nHabilidades: Punalada Trapera, Ataque Doble.",
        "Sanador sagrado.\nPrimario: WIS\nHabilidades: Curar, Golpe Sagrado.",
        "Explorador natural.\nPrimario: DEX\nHabilidades: Flecha Certera, Lluvia de Flechas."
    };

    private final PrimitivoGame game;

    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         font;

    private Step step = Step.NAME;
    private final StringBuilder nameBuffer = new StringBuilder("Heroe");
    private int selectedRace  = 0;
    private int selectedClass = 0;

    public CharacterCreationScreen(PrimitivoGame game) {
        this.game = game;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, SCREEN_W, SCREEN_H);
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();
        font   = Fonts.build(16);
    }

    @Override
    public void render(float delta) {
        handleInput();
        draw();
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private void handleInput() {
        switch (step) {
            case NAME:
                handleNameInput();
                if (Gdx.input.isKeyJustPressed(Keys.ENTER) && nameBuffer.length() > 0)
                    step = Step.RACE;
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE))
                    game.setScreen(new TitleScreen(game));
                break;

            case RACE:
                if (Gdx.input.isKeyJustPressed(Keys.UP))     selectedRace = Math.max(0, selectedRace - 1);
                if (Gdx.input.isKeyJustPressed(Keys.DOWN))   selectedRace = Math.min(RACES.length - 1, selectedRace + 1);
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) step = Step.NAME;
                if (Gdx.input.isKeyJustPressed(Keys.ENTER))  step = Step.CLASS;
                break;

            case CLASS:
                if (Gdx.input.isKeyJustPressed(Keys.UP))     selectedClass = Math.max(0, selectedClass - 1);
                if (Gdx.input.isKeyJustPressed(Keys.DOWN))   selectedClass = Math.min(CLASS_LABELS.length - 1, selectedClass + 1);
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) step = Step.RACE;
                if (Gdx.input.isKeyJustPressed(Keys.ENTER) && CLASS_AVAIL[selectedClass])
                    createAndStart();
                break;
        }
    }

    private void handleNameInput() {
        for (int k = Keys.A; k <= Keys.Z; k++) {
            if (Gdx.input.isKeyJustPressed(k) && nameBuffer.length() < 16) {
                boolean shift = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)
                             || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
                nameBuffer.append((char)((shift ? 'A' : 'a') + (k - Keys.A)));
            }
        }
        if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE) && nameBuffer.length() > 0)
            nameBuffer.deleteCharAt(nameBuffer.length() - 1);
        if (Gdx.input.isKeyJustPressed(Keys.SPACE) && nameBuffer.length() > 0 && nameBuffer.length() < 16)
            nameBuffer.append(' ');
    }

    private void createAndStart() {
        CharacterClass charClass;
        switch (selectedClass) {
            case 1:  charClass = new Wizard();  break;
            case 2:  charClass = new Rogue();   break;
            case 3:  charClass = new Healer();  break;
            case 4:  charClass = new Ranger();  break;
            default: charClass = new Fighter(); break;
        }
        Player player = new Player(nameBuffer.toString().trim(), RACES[selectedRace], charClass);
        SaveManager.saveChests(0, "");   // partida nueva: cofres sin abrir
        game.setScreen(new OverworldScreen(game, player));
    }

    // ── Drawing ──────────────────────────────────────────────────────────

    private void draw() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.13f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        drawLeftPanel();
        drawRightPanel();
    }

    private void drawLeftPanel() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapes.setColor(0.06f, 0.06f, 0.10f, 1);
        shapes.rect(0, 0, SPLIT, SCREEN_H);

        // Right border
        shapes.setColor(0.25f, 0.25f, 0.45f, 1);
        shapes.rect(SPLIT - 2, 0, 2, SCREEN_H);

        // Active section highlight
        float[] hy = sectionHighlight();
        shapes.setColor(0.10f, 0.10f, 0.18f, 1);
        shapes.rect(4, hy[0], SPLIT - 8, hy[1]);

        // Section dividers
        shapes.setColor(0.15f, 0.15f, 0.28f, 1);
        shapes.rect(8, 600, SPLIT - 16, 1);
        shapes.rect(8, 440, SPLIT - 16, 1);
        shapes.rect(8, 50,  SPLIT - 16, 1);

        // Name input box
        Color border = (step == Step.NAME)
                ? new Color(0.35f, 0.35f, 0.65f, 1)
                : new Color(0.18f, 0.18f, 0.30f, 1);
        shapes.setColor(border);
        shapes.rect(18, 616, SPLIT - 36, 36);
        shapes.setColor(0.06f, 0.06f, 0.10f, 1);
        shapes.rect(20, 618, SPLIT - 40, 32);

        shapes.end();

        batch.begin();

        // Title
        font.setColor(new Color(0.85f, 0.85f, 1.0f, 1));
        font.draw(batch, "CREAR PERSONAJE", 0, 700, SPLIT, Align.center, false);

        // ── 1. Nombre ──
        drawSectionLabel("1. NOMBRE", 18, 664, step == Step.NAME);
        boolean blink = (TimeUtils.millis() / 500) % 2 == 0;
        font.setColor(Color.WHITE);
        font.draw(batch, nameBuffer + (step == Step.NAME && blink ? "_" : ""), 28, 641);

        // ── 2. Raza ──
        drawSectionLabel("2. RAZA", 18, 580, step == Step.RACE);
        for (int i = 0; i < RACE_LABELS.length; i++) {
            boolean isSel  = (i == selectedRace);
            boolean active = (step == Step.RACE && isSel);
            if (active)      font.setColor(Color.WHITE);
            else if (isSel)  font.setColor(new Color(0.72f, 0.72f, 0.95f, 1));
            else             font.setColor(Color.GRAY);
            font.draw(batch, (active ? "> " : "  ") + RACE_LABELS[i], 26, 548 - i * 30);
        }

        // ── 3. Clase ──
        drawSectionLabel("3. CLASE", 18, 420, step == Step.CLASS);
        for (int i = 0; i < CLASS_LABELS.length; i++) {
            boolean active = (step == Step.CLASS && i == selectedClass);
            boolean avail  = CLASS_AVAIL[i];
            if (active)      font.setColor(Color.WHITE);
            else if (!avail) font.setColor(new Color(0.28f, 0.28f, 0.28f, 1));
            else             font.setColor(Color.GRAY);
            String suffix = avail ? "" : " (prox.)";
            font.draw(batch, (active ? "> " : "  ") + CLASS_LABELS[i] + suffix, 26, 388 - i * 30);
        }

        // Footer
        font.setColor(new Color(0.45f, 0.45f, 0.68f, 1));
        font.draw(batch, "ENTER confirmar    ESC volver", 0, 30, SPLIT, Align.center, false);

        batch.end();
    }

    private float[] sectionHighlight() {
        switch (step) {
            case NAME:  return new float[]{ 602, 106 };  // 602..708
            case RACE:  return new float[]{ 442, 158 };  // 442..600
            case CLASS: default: return new float[]{ 56, 384 };  // 56..440
        }
    }

    private void drawSectionLabel(String label, float x, float y, boolean active) {
        font.setColor(active
                ? new Color(0.75f, 0.75f, 1.0f, 1)
                : new Color(0.38f, 0.38f, 0.58f, 1));
        font.draw(batch, label, x, y);
    }

    private void drawRightPanel() {
        Stats preview = previewStats();
        int   rx      = SPLIT + 30;

        // Portrait (top-right of right panel, changes with equipment/damage later)
        int portX = SCREEN_W - 190;
        int portY = 476;
        int portW = 160;
        int portH = 200;

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Portrait bg + border
        shapes.setColor(0.14f, 0.14f, 0.24f, 1);
        shapes.rect(portX, portY, portW, portH);
        shapes.setColor(0.35f, 0.35f, 0.60f, 1);
        shapes.rect(portX,             portY + portH - 2, portW, 2);
        shapes.rect(portX,             portY,             portW, 2);
        shapes.rect(portX,             portY,             2,     portH);
        shapes.rect(portX + portW - 2, portY,             2,     portH);

        // Silhouette
        int pcx = portX + portW / 2;
        shapes.setColor(0.55f, 0.60f, 0.80f, 1);
        shapes.rect(pcx - 18, portY + 138, 36, 36);  // head
        shapes.rect(pcx - 20, portY +  78, 40, 60);  // body
        shapes.rect(pcx - 33, portY +  82, 13, 42);  // left arm
        shapes.rect(pcx + 20, portY +  82, 13, 42);  // right arm
        shapes.rect(pcx - 22, portY +  10, 18, 68);  // left leg
        shapes.rect(pcx +  4, portY +  10, 18, 68);  // right leg

        // HP bar preview
        int   hpPreview = preview.getModifier("constitution") * 4 + 10;
        int   barW      = portX - rx - 20;
        float hpPct     = Math.min(1f, hpPreview / 75f);
        shapes.setColor(0.20f, 0.20f, 0.20f, 1);
        shapes.rect(rx, 452, barW, 10);
        shapes.setColor(0.20f, 0.70f, 0.25f, 1);
        shapes.rect(rx, 452, barW * hpPct, 10);

        shapes.end();

        batch.begin();

        // Race name + description
        font.setColor(Color.WHITE);
        font.draw(batch, RACE_LABELS[selectedRace], rx, 700);
        font.setColor(new Color(0.68f, 0.68f, 0.88f, 1));
        font.draw(batch, RACE_DESC[selectedRace], rx, 668, portX - rx - 20, Align.left, true);

        // Stats with colored deltas (2 columnas)
        font.setColor(new Color(0.60f, 0.60f, 0.88f, 1));
        font.draw(batch, "ESTADISTICAS", rx, 596);

        String[] statKeys   = { "strength", "dexterity", "intelligence", "constitution", "wisdom" };
        String[] statLabels = { "STR", "DEX", "INT", "CON", "WIS" };
        int[]    colX       = { rx, rx + 300, rx, rx + 300, rx };
        int[]    rowY       = { 564, 564, 534, 534, 504 };

        for (int i = 0; i < statKeys.length; i++) {
            int total = preview.getModifier(statKeys[i]);
            int delta = total - 10;
            font.setColor(Color.WHITE);
            font.draw(batch, statLabels[i] + "  " + total, colX[i], rowY[i]);
            if (delta != 0) {
                font.setColor(delta > 0
                        ? new Color(0.35f, 0.90f, 0.35f, 1)
                        : new Color(0.90f, 0.35f, 0.35f, 1));
                font.draw(batch, (delta > 0 ? "(+" : "(") + delta + ")", colX[i] + 150, rowY[i]);
            }
        }

        // HP preview label + value
        font.setColor(new Color(0.68f, 0.68f, 0.88f, 1));
        font.draw(batch, "HP estimado:", rx, 478);
        font.setColor(Color.WHITE);
        font.draw(batch, String.valueOf(hpPreview), rx + 230, 478);

        // Class description (only when on CLASS step)
        if (step == Step.CLASS) {
            font.setColor(new Color(0.62f, 0.62f, 0.88f, 1));
            font.draw(batch, CLASS_LABELS[selectedClass], rx, 422);
            font.setColor(CLASS_AVAIL[selectedClass] ? new Color(0.80f, 0.80f, 1.0f, 1) : Color.GRAY);
            font.draw(batch, CLASS_DESC[selectedClass], rx, 394, SCREEN_W - rx - 20, Align.left, true);
        }

        // Start hint
        if (step == Step.CLASS && CLASS_AVAIL[selectedClass]) {
            font.setColor(new Color(0.40f, 0.90f, 0.40f, 1));
            font.draw(batch, "ENTER: comenzar la aventura!", 0, 40, SCREEN_W, Align.center, false);
        }

        batch.end();
    }

    private Stats previewStats() {
        Stats s = new Stats(10, 10, 10, 10, 10);
        RACES[selectedRace].applyModifiers(s);
        return s;
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        font.dispose();
    }
}
