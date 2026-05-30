package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Align;
import java.util.ArrayList;
import java.util.List;

public class OverworldScreen implements Screen {

    // Layout
    private static final int W        = 800;   // ancho del área de juego (viewport del mundo)
    private static final int W_PANEL  = 480;   // panel lateral
    private static final int H        = 720;
    private static final int TILE     = 32;
    private static final int STATUS_H = 32;    // barra de estado inferior
    private static final int VIEW_W   = W;           // viewport del mundo: ancho
    private static final int VIEW_H   = H - STATUS_H; // viewport del mundo: alto (sobre la barra)

    // Tipos de tile
    private static final int PATH  = 0;
    private static final int GRASS = 1;
    private static final int TREE  = 2;
    private static final int WATER = 3;
    private static final int INN   = 4;
    private static final int FENCE = 5;

    // Punto de reaparición tras derrota
    public static final int INN_COL = 2;
    public static final int INN_ROW = 2;

    private static final Color[] TILE_COLOR = {
        new Color(0.72f, 0.68f, 0.55f, 1),  // PATH
        new Color(0.35f, 0.60f, 0.25f, 1),  // GRASS
        new Color(0.14f, 0.34f, 0.14f, 1),  // TREE
        new Color(0.25f, 0.52f, 0.78f, 1),  // WATER
        new Color(0.75f, 0.58f, 0.30f, 1),  // INN  — madera cálida
    };
    private static final Color[] TILE_SHADOW = {
        new Color(0.56f, 0.53f, 0.43f, 1),
        new Color(0.27f, 0.47f, 0.19f, 1),
        new Color(0.10f, 0.24f, 0.10f, 1),
        new Color(0.18f, 0.40f, 0.60f, 1),
        new Color(0.55f, 0.42f, 0.20f, 1),
    };

    // Mapa del overworld, cargado desde un archivo externo (ver TileMap).
    // Más grande que el viewport → hay scroll. Se carga una sola vez al
    // referenciar la clase (Gdx ya está inicializado para entonces).
    private static final TileMap MAP      = new TileMap("maps/overworld.txt");
    private static final int     MAP_ROWS = MAP.rows;
    private static final int     MAP_COLS = MAP.cols;

    private enum OwState { EXPLORING, PAUSED, COMMAND, LOAD_MENU, HELP }

    private static final String WALK_PATH = "sprites/characters/swordsman/lvl1/walk.png";
    private static final String RUN_PATH  = "sprites/characters/swordsman/lvl1/run.png";
    private static final int FRAME_W = 64;
    private static final int FRAME_H = 64;

    private final PrimitivoGame game;
    private final Player        player;

    private int     playerCol;
    private int     playerRow;
    private int     facingRow  = SpriteSheet.DIR_DOWN;
    private OwState state      = OwState.EXPLORING;
    private int     pauseSel   = 0;
    private String  statusMsg  = "Usa las flechas para moverte  |  ESC: pausa";
    private int     activeSlot = 0;
    private String  cmdBuffer  = "";

    // Smooth movement — caminar / correr (Shift)
    private static final float WALK_STEP = 0.16f;  // duración de un paso caminando
    private static final float RUN_STEP  = 0.09f;  // ídem corriendo (Shift)
    private float   moveDuration = WALK_STEP;
    private boolean isRunning;
    private float visualX, visualY;
    private float startVisX, startVisY;
    private float targetX, targetY;
    private float moveTimer;
    private boolean isMoving;

    private static final String IDLE_PATH    = "sprites/characters/swordsman/lvl1/idle.png";
    private static final int   IDLE_FRAME_W = 64;
    private static final int   IDLE_FRAME_H = 64;

    private OrthographicCamera worldCam;  // sigue al jugador, scrollea
    private OrthographicCamera uiCam;     // fija: barra de estado, panel, overlays
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         font;    // 10px  barra de estado + overlays
    private BitmapFont         fontLg;  // 16px  títulos de overlays
    private SpriteSheet        walkSprite;
    private SpriteSheet        runSprite;
    private SpriteSheet        idleSprite;
    private Texture            mainTilesTex;
    private NinePatch          panelPatch;
    private Texture[]          treeTexs;   // variantes de árbol (128×128)
    private Texture            grassTex;
    private Texture            plainsTex;
    private Texture            decorTex;   // decor_16x16 (rocas, etc.)
    private Texture            fencesTex;  // cerca de madera
    private TextureRegion      grassReg;   // tile de pasto (16×16)
    private TextureRegion      dirtReg;    // tile de tierra (plains, celda 2,1)
    private TextureRegion      rockReg;    // roca (decor_16x16 celda 2,4)
    private TextureRegion      hFenceReg;  // tramo de cerca horizontal (fences 2,0)
    private TextureRegion      postReg;    // poste de cerca (fences 0,0)
    private PlayerPanel        playerPanel;

    public OverworldScreen(PrimitivoGame game, Player player) {
        this(game, player, MAP_COLS / 2, MAP_ROWS / 2, null, 0);
    }

    public OverworldScreen(PrimitivoGame game, Player player, int startCol, int startRow) {
        this(game, player, startCol, startRow, null, 0);
    }

    public OverworldScreen(PrimitivoGame game, Player player, int startCol, int startRow, String initialMsg) {
        this(game, player, startCol, startRow, initialMsg, 0);
    }

    public OverworldScreen(PrimitivoGame game, Player player, int startCol, int startRow, String initialMsg, int activeSlot) {
        this.game       = game;
        this.player     = player;
        this.playerCol  = startCol;
        this.playerRow  = startRow;
        this.activeSlot = activeSlot;
        if (initialMsg != null) this.statusMsg = initialMsg;
        this.visualX    = startCol * TILE - TILE;
        this.visualY    = worldTileY(startRow) - TILE;
        this.targetX    = this.visualX;
        this.targetY    = this.visualY;
        this.startVisX  = this.visualX;
        this.startVisY  = this.visualY;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void show() {
        worldCam    = new OrthographicCamera();
        worldCam.setToOrtho(false, VIEW_W, VIEW_H);
        uiCam       = new OrthographicCamera();
        uiCam.setToOrtho(false, W + W_PANEL, H);
        batch       = new SpriteBatch();
        shapes      = new ShapeRenderer();
        font        = Fonts.build(10);
        fontLg      = Fonts.build(16);
        walkSprite   = new SpriteSheet(WALK_PATH, FRAME_W, FRAME_H, 0.10f);
        runSprite    = new SpriteSheet(RUN_PATH,  FRAME_W, FRAME_H, 0.07f);
        idleSprite   = new SpriteSheet(IDLE_PATH, IDLE_FRAME_W, IDLE_FRAME_H, 0.18f);

        playerPanel = new PlayerPanel(player, idleSprite);

        mainTilesTex = new Texture(Gdx.files.internal("ui/main_tiles.png"));
        panelPatch   = buildPanelPatch(mainTilesTex);
        // Variantes de árbol (probá cambiar/agregar nombres de tiles/overworld/trees/Trees_shadow/)
        String[] treeFiles = {
            "Tree1.png", "Moss_tree1.png", "Fruit_tree1.png", "Flower_tree1.png",
        };
        treeTexs = new Texture[treeFiles.length];
        for (int i = 0; i < treeFiles.length; i++)
            treeTexs[i] = new Texture(Gdx.files.internal("tiles/overworld/trees/Trees_shadow/" + treeFiles[i]));

        grassTex     = new Texture(Gdx.files.internal("tiles/overworld/grass.png"));
        plainsTex    = new Texture(Gdx.files.internal("tiles/overworld/plains.png"));
        decorTex     = new Texture(Gdx.files.internal("tiles/overworld/decor_16x16.png"));
        fencesTex    = new Texture(Gdx.files.internal("tiles/overworld/fences.png"));
        for (Texture t : new Texture[]{ grassTex, plainsTex, decorTex, fencesTex })
            t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        grassReg     = new TextureRegion(grassTex);                    // 16×16 completo
        dirtReg      = new TextureRegion(plainsTex, 32, 16, 16, 16);   // tierra (col 2, fila 1)
        rockReg      = new TextureRegion(decorTex,  32, 64, 16, 16);   // roca  (col 2, fila 4)
        hFenceReg    = new TextureRegion(fencesTex, 32,  0, 16, 16);   // cerca horizontal (col 2, fila 0)
        postReg      = new TextureRegion(fencesTex,  0,  0, 16, 16);   // poste (col 0, fila 0)

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyTyped(char character) {
                if (state == OwState.EXPLORING && character == ':') {
                    cmdBuffer = "";
                    state = OwState.COMMAND;
                    return true;
                }
                if (state == OwState.COMMAND && character >= ' ' && character < 127 && character != '\r') {
                    cmdBuffer += character;
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render(float delta) {
        if (isMoving) (isRunning ? runSprite : walkSprite).update(delta);
        idleSprite.update(delta);
        updateMovement(delta);
        handleInput();
        draw();
    }

    // ── Input ─────────────────────────────────────────────────────────────

    private void handleInput() {
        if (state == OwState.EXPLORING) {
            int dc = 0, dr = 0;
            if (Gdx.input.isKeyPressed(Keys.LEFT))  dc = -1;
            if (Gdx.input.isKeyPressed(Keys.RIGHT)) dc =  1;
            if (Gdx.input.isKeyPressed(Keys.UP))    dr = -1;
            if (Gdx.input.isKeyPressed(Keys.DOWN))  dr =  1;

            if (dc != 0 || dr != 0) {
                if      (dr ==  1) facingRow = SpriteSheet.DIR_DOWN;
                else if (dr == -1) facingRow = SpriteSheet.DIR_UP;
                else if (dc == -1) facingRow = SpriteSheet.DIR_LEFT;
                else               facingRow = SpriteSheet.DIR_RIGHT;

                if (!isMoving) {
                    int nc = playerCol + dc;
                    int nr = playerRow + dr;
                    if (nr >= 0 && nr < MAP_ROWS && nc >= 0 && nc < MAP_COLS && isPassable(MAP.get(nc, nr))) {
                        boolean run = Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)
                                   || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
                        playerCol    = nc;
                        playerRow    = nr;
                        startVisX    = visualX;
                        startVisY    = visualY;
                        targetX      = nc * TILE - TILE;
                        targetY      = worldTileY(nr) - TILE;
                        moveTimer    = 0f;
                        isMoving     = true;
                        isRunning    = run;
                        moveDuration = run ? RUN_STEP : WALK_STEP;
                    }
                }
            }

            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                pauseSel = 0;
                state = OwState.PAUSED;
            }

        } else if (state == OwState.PAUSED) {
            if (Gdx.input.isKeyJustPressed(Keys.UP))     pauseSel = 0;
            if (Gdx.input.isKeyJustPressed(Keys.DOWN))   pauseSel = 1;
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) state = OwState.EXPLORING;
            if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                if (pauseSel == 0) state = OwState.EXPLORING;
                else               Gdx.app.exit();
            }

        } else if (state == OwState.COMMAND) {
            handleCommandInput();

        } else if (state == OwState.LOAD_MENU) {
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                state = OwState.EXPLORING;
                return;
            }
            for (int k = Keys.NUM_0; k <= Keys.NUM_9; k++) {
                if (Gdx.input.isKeyJustPressed(k)) {
                    int slot = k - Keys.NUM_0;
                    if (SaveManager.hasSave(slot)) {
                        SaveManager.SaveData d = SaveManager.load(slot);
                        game.setScreen(new OverworldScreen(game, d.player, d.col, d.row,
                                "Partida cargada desde slot " + slotLabel(slot) + ".", slot));
                    } else {
                        state = OwState.EXPLORING;
                        statusMsg = "El slot " + slot + " esta vacio.";
                    }
                    return;
                }
            }

        } else if (state == OwState.HELP) {
            if (Gdx.input.isKeyJustPressed(Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                state = OwState.EXPLORING;
            }
        }
    }

    private void handleCommandInput() {
        if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE) && !cmdBuffer.isEmpty()) {
            cmdBuffer = cmdBuffer.substring(0, cmdBuffer.length() - 1);
        } else if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
            String cmd = cmdBuffer.trim();
            cmdBuffer = "";
            state = OwState.EXPLORING;
            dispatchCommand(cmd);
        } else if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            cmdBuffer = "";
            state = OwState.EXPLORING;
        }
    }

    private void dispatchCommand(String cmd) {
        if (cmd.equals("wq") || cmd.equals("qs")) {
            SaveManager.save(player, playerCol, playerRow, activeSlot);
            statusMsg = "Guardado en slot " + slotLabel(activeSlot) + ".";
            return;
        }
        if (cmd.equals("q!")) {
            Gdx.app.exit();
            return;
        }
        if (cmd.length() == 2 && cmd.charAt(0) == 'q' && cmd.charAt(1) >= '1' && cmd.charAt(1) <= '9') {
            int slot = cmd.charAt(1) - '0';
            SaveManager.save(player, playerCol, playerRow, slot);
            activeSlot = slot;
            statusMsg = "Guardado en slot " + slot + ".";
            return;
        }
        if (cmd.equals("load")) {
            state = OwState.LOAD_MENU;
            return;
        }
        if (cmd.equals("help")) {
            state = OwState.HELP;
            return;
        }
        if (cmd.equals("cheats")) {
            statusMsg = ":cheats — proximamente...";
            return;
        }
        if (!cmd.isEmpty()) {
            statusMsg = "Comando desconocido: :" + cmd;
        }
    }

    private String slotLabel(int slot) {
        return slot == 0 ? "rapido" : String.valueOf(slot);
    }

    private void updateMovement(float delta) {
        if (!isMoving) return;
        moveTimer += delta;
        float p = Math.min(1f, moveTimer / moveDuration);
        visualX = startVisX + (targetX - startVisX) * p;
        visualY = startVisY + (targetY - startVisY) * p;
        if (p >= 1f) {
            isMoving = false;
            visualX = targetX;
            visualY = targetY;
            onStep();
        }
    }

    private boolean isPassable(int tile) {
        return tile == PATH || tile == GRASS || tile == INN;
    }

    private void onStep() {
        SaveManager.save(player, playerCol, playerRow);
        int tile = MAP.get(playerCol, playerRow);
        if (tile == GRASS && Math.random() < 0.20) {
            Enemy enemy = spawnEnemy();
            game.setScreen(new BattleScreen(game, player, enemy, this));
        } else if (tile == INN) {
            int healed = player.getMaxHp() - player.getHp();
            if (healed > 0) {
                player.heal(healed);
                SaveManager.save(player, playerCol, playerRow);
                statusMsg = "La posada restaura tu HP completamente.";
            } else {
                statusMsg = "Posada — aqui puedes descansar.";
            }
        } else {
            statusMsg = tile == GRASS
                    ? "Hierba alta... algo puede acechar aqui."
                    : "Camino seguro.";
        }
    }

    private Enemy spawnEnemy() {
        int      lvl   = player.getLevel();
        String[] names = { "Goblin", "Rata Gigante", "Bandido", "Esqueleto" };
        String   name  = names[(int)(Math.random() * names.length)];
        int      hp    = 25 + lvl * 8  + (int)(Math.random() * 15);
        int      dmg   =  5 + lvl * 2  + (int)(Math.random() * 5);
        Stats    s     = new Stats(
             5 + lvl * 2 + (int)(Math.random() * 4),
             4 + lvl     + (int)(Math.random() * 4),
             3           + (int)(Math.random() * 3),
             5 + lvl     + (int)(Math.random() * 4),
             3           + (int)(Math.random() * 3)
        );
        return new Enemy(name, hp, new ArrayList<>(), dmg, s);
    }

    // ── Drawing ───────────────────────────────────────────────────────────

    private void draw() {
        Gdx.gl.glClearColor(0.08f, 0.08f, 0.13f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // ── Pasada del mundo: scrollea, recortada al área de juego sobre la barra ──
        updateWorldCamera();
        computeVisibleRange();
        HdpiUtils.glViewport(0, STATUS_H, VIEW_W, VIEW_H);
        shapes.setProjectionMatrix(worldCam.combined);
        batch.setProjectionMatrix(worldCam.combined);
        drawTerrain();           // texturas de pasto/tierra (batch)
        drawWaterInn();          // agua + posada (shapes) sobre la base
        drawFences();            // cerca del borde (batch)
        drawTreeSprites(true);   // árboles al norte del jugador (detrás)
        drawPlayer();
        drawTreeSprites(false);  // árboles al sur del jugador (adelante)

        // ── Pasada de UI: fija, pantalla completa ──
        HdpiUtils.glViewport(0, 0, W + W_PANEL, H);
        uiCam.update();
        shapes.setProjectionMatrix(uiCam.combined);
        batch.setProjectionMatrix(uiCam.combined);
        drawStatusBar();
        drawSidePanel();

        if (state == OwState.PAUSED)     drawPauseOverlay();
        if (state == OwState.LOAD_MENU)  drawLoadMenuOverlay();
        if (state == OwState.HELP)       drawHelpOverlay();
    }

    // ── Cámara del mundo: sigue al jugador, clampeada a los bordes del mapa ──

    private int visC0, visC1, visR0, visR1;  // rango de tiles visibles (con margen)

    private void updateWorldCamera() {
        float halfW = VIEW_W / 2f, halfH = VIEW_H / 2f;
        float mapW  = MAP_COLS * TILE, mapH = MAP_ROWS * TILE;
        // centro del jugador en mundo (sprite de 3·TILE centrado sobre su tile)
        float px = visualX + TILE * 1.5f;
        float py = visualY + TILE * 1.5f;
        worldCam.position.set(clampCam(px, halfW, mapW), clampCam(py, halfH, mapH), 0);
        worldCam.update();
    }

    // Si el mapa es más chico que el viewport en un eje, lo centra
    private float clampCam(float v, float half, float mapSize) {
        if (mapSize <= half * 2f) return mapSize / 2f;
        return Math.max(half, Math.min(mapSize - half, v));
    }

    private void computeVisibleRange() {
        float halfW = VIEW_W / 2f, halfH = VIEW_H / 2f;
        float left   = worldCam.position.x - halfW;
        float right  = worldCam.position.x + halfW;
        float bottom = worldCam.position.y - halfH;
        float top    = worldCam.position.y + halfH;
        int margin = 3;  // holgura para árboles (sprites 4·TILE que sobresalen del tile)
        visC0 = clampInt((int) Math.floor(left  / TILE) - margin, 0, MAP_COLS - 1);
        visC1 = clampInt((int) Math.floor(right / TILE) + margin, 0, MAP_COLS - 1);
        // fila ↔ mundo: worldTileY(r) = (MAP_ROWS-1-r)·TILE  ⇒  r = MAP_ROWS-1 - y/TILE
        visR0 = clampInt(MAP_ROWS - 1 - (int) Math.floor(top    / TILE) - margin, 0, MAP_ROWS - 1);
        visR1 = clampInt(MAP_ROWS - 1 - (int) Math.floor(bottom / TILE) + margin, 0, MAP_ROWS - 1);
    }

    private static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // Base de terreno con texturas (pasto/tierra), dibujada a 2× (16→32px).
    // El agua se omite acá: la pinta drawWaterInn por encima.
    private void drawTerrain() {
        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            for (int c = visC0; c <= visC1; c++) {
                int t = MAP.get(c, r);
                if (t == WATER) continue;
                TextureRegion reg = (t == PATH || t == INN) ? dirtReg : grassReg;
                batch.draw(reg, c * TILE, worldTileY(r), TILE, TILE);
            }
        }
        batch.end();
    }

    // Agua y posada con ShapeRenderer, sobre la base de terreno.
    private void drawWaterInn() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = visR0; r <= visR1; r++) {
            for (int c = visC0; c <= visC1; c++) {
                int tile = MAP.get(c, r);
                if (tile != WATER && tile != INN) continue;
                int tx = c * TILE;
                int ty = worldTileY(r);

                if (tile == WATER) {
                    shapes.setColor(TILE_COLOR[WATER]);
                    shapes.rect(tx, ty, TILE, TILE);
                    shapes.setColor(0.45f, 0.68f, 0.92f, 1);
                    shapes.rect(tx + 3, ty + TILE / 2 - 2, TILE - 6, 4);
                } else {  // INN
                    shapes.setColor(0.65f, 0.22f, 0.18f, 1);
                    shapes.rect(tx + 2, ty + TILE - 8, TILE - 4, 7);
                    if (r == INN_ROW) {
                        shapes.setColor(0.35f, 0.20f, 0.08f, 1);
                        shapes.rect(tx + 11, ty + 2, 10, 14);
                    } else {
                        shapes.setColor(0.90f, 0.85f, 0.50f, 1);
                        shapes.rect(tx + 7,  ty + 8, 8, 7);
                        shapes.rect(tx + 18, ty + 8, 8, 7);
                    }
                }
            }
        }
        shapes.end();
    }

    private void drawPlayer() {
        int size = TILE * 3;
        // En movimiento: caminar/correr según Shift. Quieto: idle (mismo sprite del panel).
        SpriteSheet sprite = isMoving ? (isRunning ? runSprite : walkSprite) : idleSprite;
        batch.begin();
        sprite.draw(batch, facingRow, visualX, visualY, size, size);
        batch.end();
    }

    // beforePlayer=true  → árboles en filas <= playerRow (norte/detrás del jugador)
    // beforePlayer=false → árboles en filas >  playerRow (sur/delante del jugador)
    private void drawTreeSprites(boolean beforePlayer) {
        final int TREE_SIZE = TILE * 4;         // árboles 128px (4×TILE)
        final int TREE_XOFF = TILE + TILE / 2;  // (128-32)/2 = 48px para centrar
        final int ROCK_SIZE = TILE;             // roca 32px (16→2×), llena el tile

        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            boolean inPass = beforePlayer ? (r <= playerRow) : (r > playerRow);
            if (!inPass) continue;
            for (int c = visC0; c <= visC1; c++) {
                if (MAP.get(c, r) != TREE) continue;
                int h = scatterHash(r, c);
                if (h % 7 == 0) {  // ~1 de cada 7 → roca, para aflojar la densidad
                    batch.draw(rockReg, c * TILE, worldTileY(r), ROCK_SIZE, ROCK_SIZE);
                } else {           // resto → variante de árbol
                    Texture tex = treeTexs[h % treeTexs.length];
                    batch.draw(tex, c * TILE - TREE_XOFF, worldTileY(r), TREE_SIZE, TREE_SIZE);
                }
            }
        }
        batch.end();
    }

    // Cerca del borde (y de cualquier tile 'F'): tramo horizontal arriba/abajo, poste en laterales.
    private void drawFences() {
        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            for (int c = visC0; c <= visC1; c++) {
                if (MAP.get(c, r) != FENCE) continue;
                boolean horizontal = (r == 0 || r == MAP_ROWS - 1) && c > 0 && c < MAP_COLS - 1;
                batch.draw(horizontal ? hFenceReg : postReg, c * TILE, worldTileY(r), TILE, TILE);
            }
        }
        batch.end();
    }

    // Hash determinista por celda (mismo resultado cada frame, sin parpadeo)
    private static int scatterHash(int r, int c) {
        int h = (r * 73856093) ^ (c * 19349663);
        return (h >>> 1) % 1000;
    }

    // Coord. de mundo (Y hacia arriba, origen abajo-izquierda del mapa):
    // fila 0 = arriba del mapa → la Y más alta.
    private static int worldTileY(int row) {
        return (MAP_ROWS - 1 - row) * TILE;
    }

    private void drawStatusBar() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.05f, 0.05f, 0.09f, 1);
        shapes.rect(0, 0, W, STATUS_H);
        shapes.setColor(0.22f, 0.22f, 0.38f, 1);
        shapes.rect(0, STATUS_H - 1, W, 1);
        shapes.end();

        batch.begin();
        if (state == OwState.COMMAND) {
            font.setColor(Color.WHITE);
            font.draw(batch, ":" + cmdBuffer + "_", 10, STATUS_H - 7);
        } else {
            font.setColor(new Color(0.92f, 0.92f, 0.72f, 1));
            font.draw(batch, statusMsg, 10, STATUS_H - 7);
        }
        batch.end();
    }

    private void drawPauseOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int bx = W / 2 - 130, by = H / 2 - 80, bw = 260, bh = 160;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.08f, 0.14f, 1);
        shapes.rect(bx, by, bw, bh);
        shapes.setColor(0.35f, 0.35f, 0.60f, 1);
        shapes.rect(bx,          by + bh - 2, bw, 2);
        shapes.rect(bx,          by,          bw, 2);
        shapes.rect(bx,          by,          2,  bh);
        shapes.rect(bx + bw - 2, by,          2,  bh);
        shapes.end();

        batch.begin();
        font.setColor(new Color(0.85f, 0.85f, 1.0f, 1));
        font.draw(batch, "PAUSA", 0, by + bh - 18, W, Align.center, false);
        String[] opts = { "Continuar", "Salir del juego" };
        for (int i = 0; i < opts.length; i++) {
            font.setColor(i == pauseSel ? Color.WHITE : Color.GRAY);
            font.draw(batch, (i == pauseSel ? "> " : "  ") + opts[i],
                    0, by + bh - 58 - i * 36, W, Align.center, false);
        }
        font.setColor(new Color(0.40f, 0.40f, 0.60f, 1));
        font.draw(batch, "ESC para volver", 0, by + 20, W, Align.center, false);
        batch.end();
    }

    private void drawSidePanel() {
        playerPanel.draw(shapes, batch);
    }

    private static NinePatch buildPanelPatch(Texture tex) {
        NinePatch p = new NinePatch(new TextureRegion(tex, 0, 0, 48, 48), 16, 16, 16, 16);
        p.setLeftWidth(32);  p.setRightWidth(32);
        p.setTopHeight(32);  p.setBottomHeight(32);
        return p;
    }

    private void drawLoadMenuOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int bw = 320, bh = 290;
        int bx = W / 2 - bw / 2, by = H / 2 - bh / 2;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.06f, 0.04f, 1);
        shapes.rect(bx, by, bw, bh);
        shapes.setColor(0.45f, 0.32f, 0.15f, 1);
        shapes.rect(bx,          by + bh - 2, bw, 2);
        shapes.rect(bx,          by,          bw, 2);
        shapes.rect(bx,          by,          2,  bh);
        shapes.rect(bx + bw - 2, by,          2,  bh);
        shapes.end();

        batch.begin();
        fontLg.setColor(new Color(1.0f, 0.82f, 0.35f, 1));
        fontLg.draw(batch, "CARGAR PARTIDA", 0, by + bh - 10, W, Align.center, false);

        int y = by + bh - 46;
        for (int slot = 0; slot <= 9; slot++) {
            SaveManager.SlotInfo info = SaveManager.getSlotInfo(slot);
            String key = "[" + slot + "] ";
            if (info.exists) {
                font.setColor(Color.WHITE);
                font.draw(batch, key + info.name + "  Lv." + info.level, bx + 14, y);
            } else {
                font.setColor(Color.DARK_GRAY);
                font.draw(batch, key + "(vacio)", bx + 14, y);
            }
            y -= 22;
        }

        font.setColor(new Color(0.50f, 0.50f, 0.50f, 1));
        font.draw(batch, "[0-9] seleccionar    ESC cancelar", 0, by + 14, W, Align.center, false);
        batch.end();
    }

    private void drawHelpOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.72f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int bw = 360, bh = 240;
        int bx = W / 2 - bw / 2, by = H / 2 - bh / 2;

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.06f, 0.04f, 1);
        shapes.rect(bx, by, bw, bh);
        shapes.setColor(0.45f, 0.32f, 0.15f, 1);
        shapes.rect(bx,          by + bh - 2, bw, 2);
        shapes.rect(bx,          by,          bw, 2);
        shapes.rect(bx,          by,          2,  bh);
        shapes.rect(bx + bw - 2, by,          2,  bh);
        shapes.end();

        String[][] lines = {
            { ":wq  :qs",  "guardar (slot activo)" },
            { ":q1-:q9",   "guardar en slot 1-9"   },
            { ":q!",       "salir sin guardar"      },
            { ":load",     "cargar partida"         },
            { ":help",     "esta ayuda"             },
            { ":cheats",   "trucos (proximamente)"  },
        };

        batch.begin();
        fontLg.setColor(new Color(1.0f, 0.82f, 0.35f, 1));
        fontLg.draw(batch, "COMANDOS  ( Shift+; para abrir )", 0, by + bh - 10, W, Align.center, false);

        int y = by + bh - 50;
        int col2 = bx + 140;
        for (String[] line : lines) {
            font.setColor(new Color(0.72f, 0.88f, 1.0f, 1));
            font.draw(batch, line[0], bx + 16, y);
            font.setColor(Color.LIGHT_GRAY);
            font.draw(batch, line[1], col2, y);
            y -= 26;
        }

        font.setColor(new Color(0.50f, 0.50f, 0.50f, 1));
        font.draw(batch, "ESC o ENTER para cerrar", 0, by + 14, W, Align.center, false);
        batch.end();
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
        fontLg.dispose();
        walkSprite.dispose();
        runSprite.dispose();
        idleSprite.dispose();
        mainTilesTex.dispose();
        for (Texture t : treeTexs) t.dispose();
        grassTex.dispose();
        plainsTex.dispose();
        decorTex.dispose();
        fencesTex.dispose();
        playerPanel.dispose();
    }
}
