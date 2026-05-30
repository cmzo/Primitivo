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

// Pantalla de título / menú principal. Punto de entrada del juego:
// Nueva partida · Cargar partida · Opciones · Salir.
public class TitleScreen implements Screen {

    private static final int W = 1280;
    private static final int H = 720;

    private enum TState { MENU, CONFIRM_NEW, LOAD, OPTIONS }

    private static final String[] ITEMS = { "Nueva partida", "Cargar partida", "Opciones", "Salir" };

    private final PrimitivoGame game;

    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         titleFont;  // 48px
    private BitmapFont         menuFont;   // 20px
    private BitmapFont         smallFont;  // 12px

    private TState state      = TState.MENU;
    private int    sel        = 0;
    private int    confirmSel = 1;   // 0=Empezar, 1=Cancelar (default Cancelar)
    private String msg        = "";

    // Paleta (consistente con los overlays del juego)
    private final Color GOLD  = new Color(1.0f,  0.82f, 0.35f, 1);
    private final Color PARCH = new Color(0.84f, 0.73f, 0.57f, 1);
    private final Color DIM   = new Color(0.50f, 0.50f, 0.62f, 1);

    public TitleScreen(PrimitivoGame game) {
        this.game = game;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera    = new OrthographicCamera();
        camera.setToOrtho(false, W, H);
        batch     = new SpriteBatch();
        shapes    = new ShapeRenderer();
        titleFont = Fonts.build(48);
        menuFont  = Fonts.build(20);
        smallFont = Fonts.build(12);
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void render(float delta) {
        handleInput();
        draw();
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private void handleInput() {
        switch (state) {
            case MENU:        inputMenu();    break;
            case CONFIRM_NEW: inputConfirm(); break;
            case LOAD:        inputLoad();    break;
            case OPTIONS:
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.ENTER))
                    state = TState.MENU;
                break;
        }
    }

    private void inputMenu() {
        if (Gdx.input.isKeyJustPressed(Keys.UP))    { sel = (sel + ITEMS.length - 1) % ITEMS.length; msg = ""; }
        if (Gdx.input.isKeyJustPressed(Keys.DOWN))  { sel = (sel + 1) % ITEMS.length;                msg = ""; }
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) select();
    }

    private void select() {
        switch (sel) {
            case 0:  // Nueva partida
                if (SaveManager.hasSave(0)) { state = TState.CONFIRM_NEW; confirmSel = 1; }
                else                        startNewGame();
                break;
            case 1:  // Cargar partida
                if (anySave()) state = TState.LOAD;
                else           msg = "No hay partidas guardadas.";
                break;
            case 2:  state = TState.OPTIONS; break;
            case 3:  Gdx.app.exit();         break;
        }
    }

    private void inputConfirm() {
        if (Gdx.input.isKeyJustPressed(Keys.LEFT)  || Gdx.input.isKeyJustPressed(Keys.UP))   confirmSel = 0;
        if (Gdx.input.isKeyJustPressed(Keys.RIGHT) || Gdx.input.isKeyJustPressed(Keys.DOWN)) confirmSel = 1;
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) state = TState.MENU;
        if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            if (confirmSel == 0) startNewGame();
            else                 state = TState.MENU;
        }
    }

    private void inputLoad() {
        if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) { state = TState.MENU; return; }
        for (int k = Keys.NUM_0; k <= Keys.NUM_9; k++) {
            if (Gdx.input.isKeyJustPressed(k)) {
                int slot = k - Keys.NUM_0;
                if (SaveManager.hasSave(slot)) {
                    SaveManager.SaveData d = SaveManager.load(slot);
                    game.setScreen(new OverworldScreen(game, d.player, d.col, d.row,
                            "Partida cargada desde slot " + (slot == 0 ? "rapido" : slot) + ".", slot));
                }
                return;
            }
        }
    }

    private void startNewGame() {
        // No borramos el slot 0 acá: la partida nueva lo sobrescribe recién al
        // dar el primer paso, así cancelar la creación no destruye el guardado.
        game.setScreen(new CharacterCreationScreen(game));
    }

    private boolean anySave() {
        for (int s = 0; s <= 9; s++) if (SaveManager.hasSave(s)) return true;
        return false;
    }

    // ── Draw ──────────────────────────────────────────────────────────────

    private void draw() {
        Gdx.gl.glClearColor(0.06f, 0.06f, 0.10f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawBackdrop();
        drawTitleAndMenu();

        if (state == TState.CONFIRM_NEW) drawConfirm();
        if (state == TState.LOAD)        drawLoad();
        if (state == TState.OPTIONS)     drawOptions();
    }

    private void drawBackdrop() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // Banda decorativa detrás del título
        shapes.setColor(0.10f, 0.09f, 0.14f, 1);
        shapes.rect(0, 470, W, 130);
        shapes.setColor(0.22f, 0.16f, 0.10f, 1);
        shapes.rect(0, 470, W, 3);
        shapes.rect(0, 597, W, 3);
        shapes.end();
    }

    private void drawTitleAndMenu() {
        batch.begin();

        // Título con sombra
        titleFont.setColor(0.15f, 0.10f, 0.05f, 1);
        titleFont.draw(batch, "PRIMITIVO", 4, 556, W, Align.center, false);
        titleFont.setColor(GOLD);
        titleFont.draw(batch, "PRIMITIVO", 0, 560, W, Align.center, false);

        smallFont.setColor(PARCH);
        smallFont.draw(batch, "RPG de pixel art  -  aventura por turnos", 0, 502, W, Align.center, false);

        // Menú
        for (int i = 0; i < ITEMS.length; i++) {
            boolean s = (i == sel);
            menuFont.setColor(s ? Color.WHITE : DIM);
            menuFont.draw(batch, (s ? "> " : "  ") + ITEMS[i] + (s ? " <" : "  "),
                    0, 410 - i * 52, W, Align.center, false);
        }

        if (!msg.isEmpty()) {
            smallFont.setColor(0.95f, 0.55f, 0.35f, 1);
            smallFont.draw(batch, msg, 0, 150, W, Align.center, false);
        }

        smallFont.setColor(DIM);
        smallFont.draw(batch, "ARRIBA / ABAJO  mover         ENTER  seleccionar", 0, 60, W, Align.center, false);

        batch.end();
    }

    // ── Overlays ──────────────────────────────────────────────────────────

    private void drawConfirm() {
        drawDimBackdrop();
        int bw = 580, bh = 210, bx = W / 2 - bw / 2, by = H / 2 - bh / 2;
        drawBox(bx, by, bw, bh);

        batch.begin();
        menuFont.setColor(GOLD);
        menuFont.draw(batch, "NUEVA PARTIDA", 0, by + bh - 24, W, Align.center, false);
        smallFont.setColor(PARCH);
        smallFont.draw(batch, "Ya existe una partida guardada (slot rapido).", 0, by + bh - 78, W, Align.center, false);
        smallFont.draw(batch, "Empezar de nuevo la sobrescribira al avanzar.", 0, by + bh - 102, W, Align.center, false);

        menuFont.setColor(confirmSel == 0 ? Color.WHITE : DIM);
        menuFont.draw(batch, (confirmSel == 0 ? "> " : "  ") + "Empezar", bx, by + 52, bw / 2, Align.center, false);
        menuFont.setColor(confirmSel == 1 ? Color.WHITE : DIM);
        menuFont.draw(batch, (confirmSel == 1 ? "> " : "  ") + "Cancelar", bx + bw / 2, by + 52, bw / 2, Align.center, false);
        batch.end();
    }

    private void drawLoad() {
        drawDimBackdrop();
        int bw = 440, bh = 400, bx = W / 2 - bw / 2, by = H / 2 - bh / 2;
        drawBox(bx, by, bw, bh);

        batch.begin();
        menuFont.setColor(GOLD);
        menuFont.draw(batch, "CARGAR PARTIDA", 0, by + bh - 22, W, Align.center, false);

        int y = by + bh - 72;
        for (int slot = 0; slot <= 9; slot++) {
            SaveManager.SlotInfo info = SaveManager.getSlotInfo(slot);
            String key = "[" + slot + "] ";
            if (info.exists) {
                smallFont.setColor(Color.WHITE);
                smallFont.draw(batch, key + info.name + "   Lv." + info.level, bx + 34, y);
            } else {
                smallFont.setColor(0.45f, 0.45f, 0.50f, 1);
                smallFont.draw(batch, key + "(vacio)", bx + 34, y);
            }
            y -= 28;
        }

        smallFont.setColor(DIM);
        smallFont.draw(batch, "[0-9] cargar      ESC volver", 0, by + 22, W, Align.center, false);
        batch.end();
    }

    private void drawOptions() {
        drawDimBackdrop();
        int bw = 580, bh = 250, bx = W / 2 - bw / 2, by = H / 2 - bh / 2;
        drawBox(bx, by, bw, bh);

        batch.begin();
        menuFont.setColor(GOLD);
        menuFont.draw(batch, "OPCIONES", 0, by + bh - 24, W, Align.center, false);

        smallFont.setColor(PARCH);
        smallFont.draw(batch, "Proximamente:", bx + 44, by + bh - 82);
        smallFont.setColor(DIM);
        String[] soon = {
            "- Volumen de musica y efectos",
            "- Remapeo de controles",
            "- Pantalla completa / tamano de ventana",
        };
        int y = by + bh - 116;
        for (String s : soon) { smallFont.draw(batch, s, bx + 60, y); y -= 28; }

        smallFont.setColor(DIM);
        smallFont.draw(batch, "ESC o ENTER  volver", 0, by + 22, W, Align.center, false);
        batch.end();
    }

    private void drawDimBackdrop() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawBox(int bx, int by, int bw, int bh) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.06f, 0.04f, 1);
        shapes.rect(bx, by, bw, bh);
        shapes.setColor(0.45f, 0.32f, 0.15f, 1);
        shapes.rect(bx,          by + bh - 2, bw, 2);
        shapes.rect(bx,          by,          bw, 2);
        shapes.rect(bx,          by,          2,  bh);
        shapes.rect(bx + bw - 2, by,          2,  bh);
        shapes.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapes.dispose();
        titleFont.dispose();
        menuFont.dispose();
        smallFont.dispose();
    }
}
