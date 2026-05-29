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

    // Layout — mismo esquema que BattleScreen
    private static final int W       = 800;
    private static final int W_PANEL = 480;
    private static final int H       = 720;
    private static final int TILE    = 32;
    private static final int COLS    = W / TILE;          // 25
    private static final int ROWS    = (H - 16) / TILE;   // 22, bottom 16px = barra de estado
    private static final int PX      = W;
    private static final int CX      = PX + 18;
    private static final int CW      = W_PANEL - 36;

    // Tipos de tile
    private static final int PATH  = 0;
    private static final int GRASS = 1;
    private static final int TREE  = 2;
    private static final int WATER = 3;
    private static final int INN   = 4;

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

    // Mapa 25×22 — T=árbol, G=hierba, .=camino, W=agua, I=posada
    private static final int[][] MAP;
    static {
        String[] src = {
            "TTTTTTTTTTTTTTTTTTTTTTTTT",  // 0
            "TIIGGG..........GGGGG...T",  // 1  ← posada (2×2)
            "TIIGGG..GGGG....GGGGG...T",  // 2  ← posada
            "T......GGGG.............T",  // 3
            "T......GGGG.............T",  // 4
            "T.......................T",  // 5
            "TGG.........TT..........T",  // 6
            "TGG.........TT..........T",  // 7
            "T...........TT..........T",  // 8
            "T...........TT..........T",  // 9
            "T...........TT..........T",  // 10
            "T.......................T",  // 11 ← punto de inicio
            "T.......................T",  // 12
            "T........TT.............T",  // 13
            "T........TT.....GGGGG...T",  // 14
            "TGGG.....TT.....GGGGG...T",  // 15
            "TGGG.....TT...........GGT",  // 16
            "TGGG.............WWWWW..T",  // 17
            "T................WWWWW..T",  // 18
            "T................WWWWW..T",  // 19
            "T.......................T",  // 20
            "TTTTTTTTTTTTTTTTTTTTTTTTT",  // 21
        };
        MAP = new int[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            String row = src[r];
            for (int c = 0; c < COLS; c++) {
                char ch = (c < row.length()) ? row.charAt(c) : '.';
                switch (ch) {
                    case 'G': MAP[r][c] = GRASS; break;
                    case 'T': MAP[r][c] = TREE;  break;
                    case 'W': MAP[r][c] = WATER; break;
                    case 'I': MAP[r][c] = INN;   break;
                    default:  MAP[r][c] = PATH;  break;
                }
            }
        }
    }

    private enum OwState { EXPLORING, PAUSED, COMMAND, LOAD_MENU, HELP }

    private static final String WALK_PATH = "sprites/characters/swordsman/lvl1/walk.png";
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

    // Smooth movement
    private static final float MOVE_DURATION = 0.12f;
    private float visualX, visualY;
    private float startVisX, startVisY;
    private float targetX, targetY;
    private float moveTimer;
    private boolean isMoving;

    private static final String IDLE_PATH    = "sprites/characters/swordsman/lvl1/idle.png";
    private static final int   IDLE_FRAME_W = 64;
    private static final int   IDLE_FRAME_H = 64;

    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         font;    // 10px  barra de estado + overlays
    private BitmapFont         fontLg;  // 16px  títulos de overlays
    private SpriteSheet        walkSprite;
    private SpriteSheet        idleSprite;
    private Texture            mainTilesTex;
    private NinePatch          panelPatch;
    private Texture            treeTex2;
    private Texture            treeTex3;
    private PlayerPanel        playerPanel;

    public OverworldScreen(PrimitivoGame game, Player player) {
        this(game, player, COLS / 2, ROWS / 2, null, 0);
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
        this.visualY    = H - (startRow + 1) * TILE - TILE;
        this.targetX    = this.visualX;
        this.targetY    = this.visualY;
        this.startVisX  = this.visualX;
        this.startVisY  = this.visualY;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera      = new OrthographicCamera();
        camera.setToOrtho(false, W + W_PANEL, H);
        batch       = new SpriteBatch();
        shapes      = new ShapeRenderer();
        font        = Fonts.build(10);
        fontLg      = Fonts.build(16);
        walkSprite   = new SpriteSheet(WALK_PATH, FRAME_W, FRAME_H, 0.12f);
        idleSprite   = new SpriteSheet(IDLE_PATH, IDLE_FRAME_W, IDLE_FRAME_H, 0.18f);

        playerPanel = new PlayerPanel(player, idleSprite);

        mainTilesTex = new Texture(Gdx.files.internal("ui/main_tiles.png"));
        panelPatch   = buildPanelPatch(mainTilesTex);
        treeTex2     = new Texture(Gdx.files.internal("tiles/overworld/trees/Trees_shadow/Tree1.png"));
        treeTex3     = new Texture(Gdx.files.internal("tiles/overworld/trees/Trees_shadow/Moss_tree1.png"));

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
        if (isMoving) walkSprite.update(delta);
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
                    if (nr >= 0 && nr < ROWS && nc >= 0 && nc < COLS && isPassable(MAP[nr][nc])) {
                        playerCol  = nc;
                        playerRow  = nr;
                        startVisX  = visualX;
                        startVisY  = visualY;
                        targetX    = nc * TILE - TILE;
                        targetY    = H - (nr + 1) * TILE - TILE;
                        moveTimer  = 0f;
                        isMoving   = true;
                        walkSprite.reset();
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
        float p = Math.min(1f, moveTimer / MOVE_DURATION);
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
        int tile = MAP[playerRow][playerCol];
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
        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawMap();
        drawTreeSprites(true);   // árboles al norte del jugador (detrás)
        drawPlayer();
        drawTreeSprites(false);  // árboles al sur del jugador (adelante)
        drawStatusBar();
        drawSidePanel();

        if (state == OwState.PAUSED)     drawPauseOverlay();
        if (state == OwState.LOAD_MENU)  drawLoadMenuOverlay();
        if (state == OwState.HELP)       drawHelpOverlay();
    }

    private void drawMap() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                int tile = MAP[r][c];
                int tx   = c * TILE;
                int ty   = tileY(r);

                // Base tile
                shapes.setColor(TILE_COLOR[tile]);
                shapes.rect(tx, ty, TILE, TILE);

                // Borde inferior y derecho (profundidad)
                shapes.setColor(TILE_SHADOW[tile]);
                shapes.rect(tx,              ty,          TILE, 1);
                shapes.rect(tx + TILE - 1,   ty,          1,    TILE);

                // Detalles por tipo
                if (tile == WATER) {
                    shapes.setColor(0.45f, 0.68f, 0.92f, 1);
                    shapes.rect(tx + 3, ty + TILE / 2 - 2, TILE - 6, 4);
                } else if (tile == INN) {
                    // Techo rojizo
                    shapes.setColor(0.65f, 0.22f, 0.18f, 1);
                    shapes.rect(tx + 2, ty + TILE - 8, TILE - 4, 7);
                    // Puerta (solo en la fila inferior del bloque INN = row 2)
                    if (MAP[r][c] == INN && r == INN_ROW) {
                        shapes.setColor(0.35f, 0.20f, 0.08f, 1);
                        shapes.rect(tx + 11, ty + 2, 10, 14);
                    } else {
                        // Ventana
                        shapes.setColor(0.90f, 0.85f, 0.50f, 1);
                        shapes.rect(tx + 7, ty + 8, 8, 7);
                        shapes.rect(tx + 18, ty + 8, 8, 7);
                    }
                }
            }
        }
        shapes.end();
    }

    private void drawPlayer() {
        int size = TILE * 3;
        batch.begin();
        walkSprite.draw(batch, facingRow, visualX, visualY, size, size);
        batch.end();
    }

    // beforePlayer=true  → árboles en filas <= playerRow (norte/detrás del jugador)
    // beforePlayer=false → árboles en filas >  playerRow (sur/delante del jugador)
    private void drawTreeSprites(boolean beforePlayer) {
        // Sprites 128×128 (4×TILE) a tamaño nativo; centrados sobre el tile de 32px
        final int SIZE = TILE * 4;
        final int XOFF = TILE + TILE / 2;  // (128-32)/2 = 48px para centrar

        batch.begin();
        for (int r = 0; r < ROWS; r++) {
            boolean inPass = beforePlayer ? (r <= playerRow) : (r > playerRow);
            if (!inPass) continue;
            for (int c = 0; c < COLS; c++) {
                if (MAP[r][c] != TREE) continue;
                int tx = c * TILE - XOFF;
                int ty = tileY(r);
                Texture tex = ((r + c) % 2 == 0) ? treeTex2 : treeTex3;
                batch.draw(tex, tx, ty, SIZE, SIZE);
            }
        }
        batch.end();
    }

    // y en LibGDX va hacia arriba; fila 0 del mapa = parte superior de pantalla
    private int tileY(int row) {
        return H - (row + 1) * TILE;
    }

    private static final int STATUS_H = 32;

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
        idleSprite.dispose();
        mainTilesTex.dispose();
        treeTex2.dispose();
        treeTex3.dispose();
        playerPanel.dispose();
    }
}
