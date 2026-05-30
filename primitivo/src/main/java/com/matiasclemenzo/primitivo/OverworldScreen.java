package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
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

    // Mapa + tileset recargables en caliente (ver maybeHotReload).
    // El tileset (tiles/tileset.cfg) define png/región/flags por letra; el mapa
    // (overworld.txt) guarda índices del tileset. Editás cualquiera y recarga.
    private static final String MAP_PATH     = "maps/overworld.txt";
    private static final String TILESET_PATH = "tiles/tileset.cfg";
    private static Tileset TILESET;
    private static TileMap MAP;
    private static int     MAP_ROWS;
    private static int     MAP_COLS;

    // Posada — la puerta es la 'I' del mapa: ese tile cura y es el respawn.
    // La casa (sprite 3×4 tiles) se dibuja anclada ahí y su footprint bloquea.
    // Mové la 'I' en overworld.txt y la casa la sigue (dejá fila ≥ 4 para que
    // el techo no se solape con el borde, y columna entre 1 y ancho-2).
    public static int INN_COL;
    public static int INN_ROW;

    static { loadMapStatic(); }

    private static void loadMapStatic() {
        if (TILESET != null) TILESET.dispose();
        TILESET  = new Tileset(TILESET_PATH);
        MAP      = new TileMap(MAP_PATH, TILESET);
        MAP_ROWS = MAP.rows;
        MAP_COLS = MAP.cols;
        int[] inn = findInn();
        INN_COL = inn[0];
        INN_ROW = inn[1];
    }

    private static int[] findInn() {
        for (int r = 0; r < MAP_ROWS; r++)
            for (int c = 0; c < MAP_COLS; c++)
                if ("inn".equals(TILESET.def(MAP.get(c, r)).kind)) return new int[]{ c, r };
        return new int[]{ MAP_COLS / 2, MAP_ROWS / 2 };
    }

    private enum OwState { EXPLORING, PAUSED, COMMAND, LOAD_MENU, HELP, INVENTORY, REVEAL }

    // Cofre en el mapa: posición + contenido + estado.
    private static class Chest {
        final int  col, row;
        final Item item;
        boolean    opened;
        Chest(int col, int row, Item item) { this.col = col; this.row = row; this.item = item; }
    }

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
    private int     invCursor  = 0;  // 0..11 inventario · 12 arma · 13 armadura

    private final List<Chest> chests = new ArrayList<>();
    private Item    revealItem;      // ítem mostrado en el cuadro de reveal
    private long    mapMtime;        // última mod. de overworld.txt (hot-reload)
    private long    cfgMtime;        // última mod. de tileset.cfg (hot-reload)

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
    // Referencias derivadas del Tileset (se rearman en rebuildTileRefs)
    private TextureRegion      groundReg;  // base de pasto bajo objetos/especiales
    private TextureRegion      chestReg;   // sprite de cofre (kind chest)
    private TextureRegion      houseReg;   // sprite de posada (kind inn)
    private TextureRegion      fencePostReg;   // poste suelto / vertical
    private TextureRegion      fenceLeftReg;   // punta izquierda (riel a la derecha)
    private TextureRegion      fenceMidReg;    // tramo del medio (rieles a ambos lados)
    private TextureRegion      fenceRightReg;  // punta derecha (riel a la izquierda)
    private Texture            whitePixel; // 1×1 blanco para floors de color (agua)
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
        initChests();
    }

    // Cofres del mapa: se generan en cada tile 'C' del txt (como todo lo demás).
    // El contenido se asigna en orden de aparición (ver lootForChest). El estado
    // "abierto" vive en la instancia (todavía no se guarda en el save).
    private void initChests() {
        int i = 0;
        for (int r = 0; r < MAP_ROWS; r++)
            for (int c = 0; c < MAP_COLS; c++)
                if ("chest".equals(TILESET.def(MAP.get(c, r)).kind))
                    chests.add(new Chest(c, r, lootForChest(i++)));
    }

    // Botín por cofre (en orden de aparición). Devolvé una instancia NUEVA por
    // cofre. Cambiá/ampliá esta lista para curar el loot con su lore.
    private Item lootForChest(int i) {
        switch (i % 3) {
            case 0:  return new Weapon("Espada del caido",
                        "Una hoja fina, demasiado buena para estar olvidada aqui. Alguien la perdio... o la escondio.",
                        50, 5, "cortante", 1);
            case 1:  return new Armor("Cota raida",
                        "Maltrecha pero solida. Aun huele a humo.", 30, 3);
            default: return new Potion("Pocion mayor",
                        "Un brebaje denso y carmesi.", 20, 40);
        }
    }

    private Chest chestAt(int col, int row) {
        for (Chest c : chests) if (c.col == col && c.row == row) return c;
        return null;
    }

    // Hot-reload: si overworld.txt cambió en disco, recarga el mapa en vivo
    // (terreno, posada, cofres). Editás el txt en el editor y el juego se
    // actualiza al guardar, sin reiniciar.
    private void maybeHotReload() {
        long m  = Gdx.files.internal(MAP_PATH).lastModified();
        long cm = Gdx.files.internal(TILESET_PATH).lastModified();
        if ((m != 0L && m != mapMtime) || (cm != 0L && cm != cfgMtime)) {
            mapMtime = m;
            cfgMtime = cm;
            loadMapStatic();      // recarga tileset + mapa
            rebuildTileRefs();    // refresca regiones derivadas (pasto, cerca, cofre, casa)
            chests.clear();
            initChests();
            clampPlayerToMap();
            statusMsg = "Mapa recargado.";
        }
    }

    private void clampPlayerToMap() {
        playerCol = Math.min(playerCol, MAP_COLS - 1);
        playerRow = Math.min(playerRow, MAP_ROWS - 1);
        visualX   = playerCol * TILE - TILE;
        visualY   = worldTileY(playerRow) - TILE;
        targetX   = visualX;  targetY   = visualY;
        startVisX = visualX;  startVisY = visualY;
        isMoving  = false;
    }

    // Regiones derivadas del Tileset (pasto base, cofre, posada, cerca).
    private void rebuildTileRefs() {
        groundReg = TILESET.ground();
        Tileset.TileDef chestDef = TILESET.firstOfKind("chest");
        chestReg  = (chestDef != null) ? chestDef.region : null;
        Tileset.TileDef innDef = TILESET.firstOfKind("inn");
        houseReg  = (innDef != null) ? innDef.region : null;
        Tileset.TileDef fenceDef = TILESET.firstOfKind("fence");
        if (fenceDef != null && fenceDef.region != null) {
            Texture ft = fenceDef.region.getTexture();
            fencePostReg  = new TextureRegion(ft,  0, 0, 16, 16);
            fenceLeftReg  = new TextureRegion(ft, 16, 0, 16, 16);
            fenceMidReg   = new TextureRegion(ft, 32, 0, 16, 16);
            fenceRightReg = new TextureRegion(ft, 48, 0, 16, 16);
        }
    }

    // Abre el cofre que el jugador tiene enfrente (según hacia dónde mira).
    private void tryInteract() {
        int fc = playerCol, fr = playerRow;
        switch (facingRow) {
            case SpriteSheet.DIR_DOWN:  fr++; break;
            case SpriteSheet.DIR_UP:    fr--; break;
            case SpriteSheet.DIR_LEFT:  fc--; break;
            case SpriteSheet.DIR_RIGHT: fc++; break;
        }
        Chest c = chestAt(fc, fr);
        if (c != null && !c.opened) {
            c.opened = true;
            player.addItem(c.item);
            ItemIcons.get(c.item.getIconIndex());  // precargá el ícono fuera del batch
            revealItem = c.item;
            state = OwState.REVEAL;
        }
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

        Pixmap wp = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        wp.setColor(Color.WHITE); wp.fill();
        whitePixel = new Texture(wp);
        wp.dispose();
        rebuildTileRefs();

        mapMtime = Gdx.files.internal(MAP_PATH).lastModified();
        cfgMtime = Gdx.files.internal(TILESET_PATH).lastModified();
        MusicManager.play("audio/music/overworld.ogg");

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
        maybeHotReload();
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
                    if (nr >= 0 && nr < MAP_ROWS && nc >= 0 && nc < MAP_COLS
                            && isPassable(MAP.get(nc, nr)) && chestAt(nc, nr) == null
                            && !houseBlocks(nc, nr)) {
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
            if (Gdx.input.isKeyJustPressed(Keys.TAB)) enterInventory();
            if (Gdx.input.isKeyJustPressed(Keys.E))   tryInteract();
            if (Gdx.input.isKeyJustPressed(Keys.M))
                statusMsg = MusicManager.toggleMute() ? "Musica silenciada (M)." : "Musica activada (M).";

        } else if (state == OwState.INVENTORY) {
            handleInventoryInput();

        } else if (state == OwState.REVEAL) {
            if (Gdx.input.isKeyJustPressed(Keys.E) || Gdx.input.isKeyJustPressed(Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                state = OwState.EXPLORING;
                revealItem = null;
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
        if (cmd.equals("i")) {
            enterInventory();
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

    // ── Inventario (modo panel) ─────────────────────────────────────────

    private void enterInventory() {
        invCursor = 0;
        state = OwState.INVENTORY;
        statusMsg = "Inventario  |  flechas: mover   E: equipar/usar/quitar   Tab/ESC: salir";
    }

    private void handleInventoryInput() {
        if (Gdx.input.isKeyJustPressed(Keys.TAB) || Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
            state = OwState.EXPLORING;
            statusMsg = "Usa las flechas para moverte  |  ESC: pausa";
            return;
        }
        if (invCursor <= 11) {                     // grilla del inventario
            int row = invCursor / 6, col = invCursor % 6;
            if (Gdx.input.isKeyJustPressed(Keys.LEFT)  && col > 0) invCursor--;
            if (Gdx.input.isKeyJustPressed(Keys.RIGHT) && col < 5) invCursor++;
            if (Gdx.input.isKeyJustPressed(Keys.DOWN)  && row == 0) invCursor += 6;
            if (Gdx.input.isKeyJustPressed(Keys.UP))   invCursor = (row == 1) ? invCursor - 6 : 12;
        } else if (invCursor == 12) {              // slot de arma
            if (Gdx.input.isKeyJustPressed(Keys.DOWN)) invCursor = 13;
        } else if (invCursor == 13) {              // slot de armadura
            if (Gdx.input.isKeyJustPressed(Keys.UP))   invCursor = 12;
            if (Gdx.input.isKeyJustPressed(Keys.DOWN)) invCursor = 0;
        }
        if (Gdx.input.isKeyJustPressed(Keys.E)) doInventoryAction();
    }

    private void doInventoryAction() {
        Inventory inv = player.getInventory();
        if (invCursor == 12) {                      // quitar arma
            if (inv.getEquippedWeapon() != null) {
                statusMsg = "Quitaste " + inv.getEquippedWeapon().getName() + ".";
                inv.unequipWeapon();
            }
            return;
        }
        if (invCursor == 13) {                      // quitar armadura
            if (inv.getEquippedArmor() != null) {
                statusMsg = "Quitaste " + inv.getEquippedArmor().getName() + ".";
                inv.unequipArmor();
            }
            return;
        }
        List<Item> items = player.getInventoryItems();
        if (invCursor >= items.size()) return;      // slot vacío
        Item item = items.get(invCursor);
        if (item instanceof Weapon || item instanceof Armor) {
            inv.equip(item);
            statusMsg = "Equipaste " + item.getName() + ".";
        } else if (item instanceof Potion) {
            if (player.getHp() >= player.getMaxHp()) {
                statusMsg = "Ya tenés la vida llena.";
            } else {
                int before = player.getHp();
                item.use(player);
                inv.removeItem(item);
                statusMsg = "Usaste " + item.getName() + " (+" + (player.getHp() - before) + " HP).";
            }
        }
        int last = Math.min(11, Math.max(0, player.getInventoryItems().size() - 1));
        if (invCursor > last) invCursor = last;
    }

    // -1 nada · 0..11 inventario · 100 arma · 101 armadura
    private int panelSelection() {
        if (state != OwState.INVENTORY) return -1;
        if (invCursor == 12) return 100;
        if (invCursor == 13) return 101;
        return invCursor;
    }

    // Arma/armadura del inventario bajo el cursor (para la comparación en EquipBox).
    private Item previewItem() {
        if (state != OwState.INVENTORY || invCursor < 0 || invCursor > 11) return null;
        List<Item> items = player.getInventoryItems();
        if (invCursor >= items.size()) return null;
        Item it = items.get(invCursor);
        return (it instanceof Weapon || it instanceof Armor) ? it : null;
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
        return !TILESET.def(tile).solid;
    }

    private void onStep() {
        SaveManager.save(player, playerCol, playerRow);
        Tileset.TileDef d = TILESET.def(MAP.get(playerCol, playerRow));
        if (d.encounter && Math.random() < 0.20) {
            Enemy enemy = spawnEnemy();
            game.setScreen(new BattleScreen(game, player, enemy, this));
        } else if ("inn".equals(d.kind)) {
            int healed = player.getMaxHp() - player.getHp();
            if (healed > 0) {
                player.heal(healed);
                SaveManager.save(player, playerCol, playerRow);
                statusMsg = "La posada restaura tu HP completamente.";
            } else {
                statusMsg = "Posada — aqui puedes descansar.";
            }
        } else {
            statusMsg = d.encounter
                    ? "Hierba alta... algo puede acechar aqui."
                    : "Todo tranquilo.";
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
        drawFloors();            // pisos (texturas/color) desde el tileset
        drawFences();            // cerca (autotile borde)
        drawChests();            // cofres (sprite)
        drawHouse();             // posada (sprite)
        drawObjects(true);       // objetos (árboles/rocas) detrás del jugador
        drawPlayer();
        drawObjects(false);      // objetos delante del jugador

        // ── Pasada de UI: fija, pantalla completa ──
        HdpiUtils.glViewport(0, 0, W + W_PANEL, H);
        uiCam.update();
        shapes.setProjectionMatrix(uiCam.combined);
        batch.setProjectionMatrix(uiCam.combined);
        drawStatusBar();
        playerPanel.setSelection(panelSelection());
        playerPanel.setPreview(previewItem());
        drawSidePanel();

        if (state == OwState.PAUSED)     drawPauseOverlay();
        if (state == OwState.LOAD_MENU)  drawLoadMenuOverlay();
        if (state == OwState.HELP)       drawHelpOverlay();
        if (state == OwState.REVEAL)     drawRevealOverlay();
    }

    // Cofres con sprite (16→32px). Abierto: atenuado (placeholder hasta tener
    // un sprite de cofre abierto).
    private void drawChests() {
        if (chestReg == null) return;
        batch.begin();
        for (Chest c : chests) {
            if (c.col < visC0 || c.col > visC1 || c.row < visR0 || c.row > visR1) continue;
            int tx = c.col * TILE, ty = worldTileY(c.row);
            if (c.opened) batch.setColor(0.5f, 0.5f, 0.5f, 1f);
            batch.draw(chestReg, tx, ty, TILE, TILE);
            if (c.opened) batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void drawRevealOverlay() {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.62f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        int bw = 480, bh = 250, bx = W / 2 - bw / 2, by = H / 2 - bh / 2;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.08f, 0.06f, 0.04f, 1);
        shapes.rect(bx, by, bw, bh);
        shapes.setColor(0.45f, 0.32f, 0.15f, 1);
        shapes.rect(bx,          by + bh - 2, bw, 2);
        shapes.rect(bx,          by,          bw, 2);
        shapes.rect(bx,          by,          2,  bh);
        shapes.rect(bx + bw - 2, by,          2,  bh);
        // marco del ícono
        int isz = 72, ix = W / 2 - isz / 2, iy = by + bh - 40 - isz;
        shapes.setColor(0.69f, 0.57f, 0.41f, 1);
        shapes.rect(ix - 4, iy - 4, isz + 8, isz + 8);
        shapes.end();

        batch.begin();
        fontLg.setColor(new Color(1.0f, 0.82f, 0.35f, 1));
        fontLg.draw(batch, "¡Obtuviste!", 0, by + bh - 14, W, Align.center, false);
        if (revealItem != null) {
            TextureRegion icon = ItemIcons.get(revealItem.getIconIndex());
            if (icon != null) batch.draw(icon, ix, iy, isz, isz);
            font.setColor(Color.WHITE);
            font.draw(batch, revealItem.getName(), 0, iy - 14, W, Align.center, false);
            font.setColor(new Color(0.82f, 0.74f, 0.55f, 1));
            font.draw(batch, revealItem.getDescription(), bx + 26, iy - 40, bw - 52, Align.center, true);
        }
        font.setColor(new Color(0.55f, 0.55f, 0.55f, 1));
        font.draw(batch, "[E] continuar", 0, by + 18, W, Align.center, false);
        batch.end();
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

    // Pisos desde el tileset: floors dibujan su textura/región (con tinte) o un
    // color sólido (agua). Los no-floor (objetos/cofre/posada/cerca) llevan una
    // base de pasto debajo.
    private void drawFloors() {
        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            for (int c = visC0; c <= visC1; c++) {
                Tileset.TileDef d = TILESET.def(MAP.get(c, r));
                int tx = c * TILE, ty = worldTileY(r);
                if ("floor".equals(d.kind)) {
                    if (d.region != null) {
                        if (d.tint != null) batch.setColor(d.tint);
                        batch.draw(d.region, tx, ty, TILE, TILE);
                        if (d.tint != null) batch.setColor(Color.WHITE);
                    } else if (d.color != null) {
                        batch.setColor(d.color);
                        batch.draw(whitePixel, tx, ty, TILE, TILE);
                        batch.setColor(Color.WHITE);
                    }
                } else if (groundReg != null) {
                    batch.draw(groundReg, tx, ty, TILE, TILE);  // base bajo el objeto
                }
            }
        }
        batch.end();
    }

    // Posada: sprite 3×4 tiles anclado con la puerta en INN_COL/INN_ROW.
    private void drawHouse() {
        if (houseReg == null) return;
        int x = (INN_COL - 1) * TILE;   // 3 tiles de ancho, puerta al centro
        int y = worldTileY(INN_ROW);    // base apoyada en la fila de la puerta
        batch.begin();
        batch.draw(houseReg, x, y, 96, 128);
        batch.end();
    }

    // El footprint de la casa (3×4) bloquea el paso, salvo la puerta.
    private boolean houseBlocks(int col, int row) {
        boolean inFootprint = col >= INN_COL - 1 && col <= INN_COL + 1
                           && row >= INN_ROW - 3 && row <= INN_ROW;
        return inFootprint && !(col == INN_COL && row == INN_ROW);
    }

    private void drawPlayer() {
        int size = TILE * 3;
        // En movimiento: caminar/correr según Shift. Quieto: idle (mismo sprite del panel).
        SpriteSheet sprite = isMoving ? (isRunning ? runSprite : walkSprite) : idleSprite;
        batch.begin();
        sprite.draw(batch, facingRow, visualX, visualY, size, size);
        batch.end();
    }

    // Objetos del tileset (kind=object): sprite sobre la base, centrado y anclado
    // al tile. Depth-sort por fila respecto del jugador (árboles altos tapan bien).
    // beforePlayer=true → filas <= playerRow (detrás); false → filas > playerRow.
    private void drawObjects(boolean beforePlayer) {
        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            boolean inPass = beforePlayer ? (r <= playerRow) : (r > playerRow);
            if (!inPass) continue;
            for (int c = visC0; c <= visC1; c++) {
                Tileset.TileDef d = TILESET.def(MAP.get(c, r));
                if (!"object".equals(d.kind) || d.region == null) continue;
                int off = (d.drawSize - TILE) / 2;   // centrar el sprite sobre el tile
                batch.draw(d.region, c * TILE - off, worldTileY(r), d.drawSize, d.drawSize);
            }
        }
        batch.end();
    }

    // Cerca (kind=fence): autotile según vecinos horizontales. Con vecino a
    // ambos lados → tramo del medio; solo a un lado → punta; sin vecinos
    // horizontales → poste (las corridas verticales quedan en postes, que es
    // como está armado este sheet).
    private void drawFences() {
        if (fencePostReg == null) return;
        batch.begin();
        for (int r = visR0; r <= visR1; r++) {
            for (int c = visC0; c <= visC1; c++) {
                if (!isFence(c, r)) continue;
                boolean left  = isFence(c - 1, r);
                boolean right = isFence(c + 1, r);
                TextureRegion reg = (left && right) ? fenceMidReg
                                  : right           ? fenceLeftReg
                                  : left            ? fenceRightReg
                                  :                   fencePostReg;
                batch.draw(reg, c * TILE, worldTileY(r), TILE, TILE);
            }
        }
        batch.end();
    }

    private boolean isFence(int col, int row) {
        return col >= 0 && col < MAP_COLS && row >= 0 && row < MAP_ROWS
            && "fence".equals(TILESET.def(MAP.get(col, row)).kind);
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
        whitePixel.dispose();
        playerPanel.dispose();
    }
}
