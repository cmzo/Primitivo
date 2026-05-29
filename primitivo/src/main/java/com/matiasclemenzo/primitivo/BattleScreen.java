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
import java.util.List;

public class BattleScreen implements Screen {

    // Layout
    private static final int W       = 800;  // game area width
    private static final int W_PANEL = 480;  // side panel width
    private static final int H       = 720;  // total height
    private static final int MENU_H  = 160;  // bottom menu area height

    // Panel content area
    private static final int PX = W;
    private static final int CX = PX + 18;
    private static final int CW = W_PANEL - 36;

    private static final String[] ACTIONS = { "Atacar", "Habilidad", "Ítem", "Huir" };

    private enum BattleState { PLAYER_MENU, SKILL_MENU, AFTER_PLAYER_ACTION, BATTLE_END, PAUSED }

    private final Player player;
    private final Enemy  enemy;

    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         font;

    private BattleState state = BattleState.PLAYER_MENU;
    private int selectedAction      = 0;
    private int selectedSkill       = 0;
    private int selectedPauseOption = 0;  // 0=continuar, 1=salir
    private String message;

    public BattleScreen(Player player, Enemy enemy) {
        this.player  = player;
        this.enemy   = enemy;
        this.message = "¿Qué hará " + player.getName() + "?";
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, W + W_PANEL, H);
        batch  = new SpriteBatch();
        shapes = new ShapeRenderer();
        font   = new BitmapFont();
        // default scale (1.0) reads well at 1280×720
    }

    @Override
    public void render(float delta) {
        handleInput();
        draw();
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput() {
        switch (state) {
            case PLAYER_MENU:
                if (Gdx.input.isKeyJustPressed(Keys.LEFT)   && selectedAction % 2 == 1) selectedAction--;
                if (Gdx.input.isKeyJustPressed(Keys.RIGHT)  && selectedAction % 2 == 0) selectedAction++;
                if (Gdx.input.isKeyJustPressed(Keys.UP)     && selectedAction >= 2)     selectedAction -= 2;
                if (Gdx.input.isKeyJustPressed(Keys.DOWN)   && selectedAction < 2)      selectedAction += 2;
                if (Gdx.input.isKeyJustPressed(Keys.ENTER)) executePlayerAction();
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) {
                    selectedPauseOption = 0;
                    state = BattleState.PAUSED;
                }
                break;

            case PAUSED:
                if (Gdx.input.isKeyJustPressed(Keys.UP))   selectedPauseOption = 0;
                if (Gdx.input.isKeyJustPressed(Keys.DOWN)) selectedPauseOption = 1;
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) state = BattleState.PLAYER_MENU;
                if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                    if (selectedPauseOption == 0) state = BattleState.PLAYER_MENU;
                    else                          Gdx.app.exit();
                }
                break;

            case SKILL_MENU:
                List<Skill> skills = player.getAvailableSkills();
                int menuSize = skills.size() + 1;
                if (Gdx.input.isKeyJustPressed(Keys.UP))     selectedSkill = Math.max(0, selectedSkill - 1);
                if (Gdx.input.isKeyJustPressed(Keys.DOWN))   selectedSkill = Math.min(menuSize - 1, selectedSkill + 1);
                if (Gdx.input.isKeyJustPressed(Keys.ESCAPE)) backToPlayerMenu();
                if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                    if (selectedSkill == skills.size()) {
                        backToPlayerMenu();
                    } else {
                        Skill skill = skills.get(selectedSkill);
                        if (!skill.isAvailable(player)) {
                            message = skill.getName() + " ya no está disponible.";
                        } else {
                            message = player.useSkill(skill, enemy);
                            state = BattleState.AFTER_PLAYER_ACTION;
                        }
                    }
                }
                break;

            case AFTER_PLAYER_ACTION:
                if (Gdx.input.isKeyJustPressed(Keys.ENTER) || Gdx.input.isKeyJustPressed(Keys.SPACE)) {
                    if (!enemy.isAlive()) {
                        message = "¡Ganaste! " + enemy.getName() + " fue derrotado.";
                        state = BattleState.BATTLE_END;
                    } else {
                        executeEnemyAction();
                        if (!player.isAlive()) {
                            message = "Fuiste derrotado... Fin del juego.";
                            state = BattleState.BATTLE_END;
                        } else {
                            state = BattleState.PLAYER_MENU;
                        }
                    }
                }
                break;

            case BATTLE_END:
                break;
        }
    }

    private void backToPlayerMenu() {
        state = BattleState.PLAYER_MENU;
        message = "¿Qué hará " + player.getName() + "?";
    }

    private void executePlayerAction() {
        switch (selectedAction) {
            case 0:
                int dmg = player.getStats().getModifier("strength");
                enemy.takeDamage(dmg);
                message = player.getName() + " ataca! " + enemy.getName() + " recibe " + dmg + " de daño.";
                state = BattleState.AFTER_PLAYER_ACTION;
                break;
            case 1:
                selectedSkill = 0;
                state = BattleState.SKILL_MENU;
                message = "Elegí una habilidad:";
                break;
            case 2:
                message = "El inventario está vacío.";
                break;
            case 3:
                message = "Huiste de la batalla!";
                state = BattleState.BATTLE_END;
                break;
        }
    }

    private void executeEnemyAction() {
        int hpBefore = player.getHp();
        enemy.chooseAction(player);
        int dmg = Math.max(0, hpBefore - player.getHp());
        message = enemy.getName() + " ataca a " + player.getName() + " por " + dmg + " de daño!";
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    private void draw() {
        Gdx.gl.glClearColor(0.12f, 0.12f, 0.18f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        shapes.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        drawBattleField();
        drawMenuArea();
        drawSidePanel();

        if (state == BattleState.PAUSED) drawPauseOverlay();
    }

    private void drawPauseOverlay() {
        // Semi-transparent backdrop over the game area only
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0f, 0f, 0f, 0.65f);
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // Modal box
        int bx = W / 2 - 130;
        int by = H / 2 - 80;
        int bw = 260;
        int bh = 160;

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
            boolean sel = (i == selectedPauseOption);
            font.setColor(sel ? Color.WHITE : Color.GRAY);
            font.draw(batch, (sel ? "> " : "  ") + opts[i], 0, by + bh - 58 - i * 36, W, Align.center, false);
        }
        font.setColor(new Color(0.40f, 0.40f, 0.60f, 1));
        font.draw(batch, "ESC para volver", 0, by + 20, W, Align.center, false);
        batch.end();
    }

    private void drawBattleField() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Sky
        shapes.setColor(0.38f, 0.62f, 0.88f, 1);
        shapes.rect(0, MENU_H, W, H - MENU_H);
        // Ground strip
        shapes.setColor(0.28f, 0.52f, 0.28f, 1);
        shapes.rect(0, MENU_H, W, 50);

        // Enemy sprite placeholder (top-right)
        if (enemy.isAlive()) {
            shapes.setColor(0.75f, 0.25f, 0.25f, 1);
            shapes.rect(490, 375, 210, 195);
        }

        // Player sprite placeholder (bottom-left)
        if (player.isAlive()) {
            shapes.setColor(0.25f, 0.25f, 0.75f, 1);
            shapes.rect(80, 215, 185, 170);
        }

        // Enemy HP bar
        shapes.setColor(0.22f, 0.22f, 0.22f, 1);
        shapes.rect(30, 683, 200, 10);
        float enemyPct = Math.max(0f, (float) enemy.getHp() / enemy.getMaxHp());
        shapes.setColor(hpColor(enemyPct));
        shapes.rect(30, 683, 200 * enemyPct, 10);

        // Player HP bar
        shapes.setColor(0.22f, 0.22f, 0.22f, 1);
        shapes.rect(430, 308, 200, 10);
        float playerPct = Math.max(0f, (float) player.getHp() / player.getMaxHp());
        shapes.setColor(hpColor(playerPct));
        shapes.rect(430, 308, 200 * playerPct, 10);

        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, enemy.getName(), 30, 710);
        font.draw(batch, "HP: " + Math.max(0, enemy.getHp()) + "/" + enemy.getMaxHp(), 30, 670);
        font.draw(batch, player.getName(), 430, 338);
        font.draw(batch, "HP: " + Math.max(0, player.getHp()) + "/" + player.getMaxHp(), 430, 292);
        batch.end();
    }

    private void drawMenuArea() {
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        shapes.setColor(0.07f, 0.07f, 0.11f, 1);
        shapes.rect(0, 0, W, MENU_H);
        shapes.setColor(0.16f, 0.16f, 0.26f, 1);
        shapes.rect(5, 5, W - 10, MENU_H - 10);

        if (state == BattleState.PLAYER_MENU || state == BattleState.SKILL_MENU) {
            shapes.setColor(0.32f, 0.32f, 0.48f, 1);
            shapes.rect(390, 5, 2, MENU_H - 10);
        }

        shapes.end();

        batch.begin();
        font.setColor(Color.WHITE);

        if (state == BattleState.PLAYER_MENU) {
            font.draw(batch, message, 18, MENU_H - 15, 365, Align.left, true);
            for (int i = 0; i < ACTIONS.length; i++) {
                int col = i % 2;
                int row = i / 2;
                float x = 405 + col * 185;
                float y = MENU_H - 28 - row * 55;
                String prefix = (i == selectedAction) ? "> " : "  ";
                font.draw(batch, prefix + ACTIONS[i], x, y);
            }
        } else if (state == BattleState.SKILL_MENU) {
            font.draw(batch, message, 18, MENU_H - 15, 365, Align.left, true);
            font.setColor(0.6f, 0.6f, 0.6f, 1);
            font.draw(batch, "ESC para volver", 18, 25);
            font.setColor(Color.WHITE);
            List<Skill> skills = player.getAvailableSkills();
            for (int i = 0; i < skills.size(); i++) {
                Skill skill = skills.get(i);
                String prefix = (i == selectedSkill) ? "> " : "  ";
                font.setColor(skill.isAvailable(player) ? Color.WHITE : Color.GRAY);
                font.draw(batch, prefix + skill.getName() + "  " + skill.getUsesLabel(),
                        405, MENU_H - 25 - i * 42);
            }
            font.setColor(Color.WHITE);
            String prefix = (selectedSkill == skills.size()) ? "> " : "  ";
            font.draw(batch, prefix + "Volver", 405, MENU_H - 25 - skills.size() * 42);
        } else {
            font.draw(batch, message, 18, MENU_H - 15, W - 30, Align.left, true);
            if (state != BattleState.BATTLE_END) {
                font.setColor(0.6f, 0.6f, 0.6f, 1);
                font.draw(batch, "Presioná ENTER para continuar...", 18, 28);
            }
        }

        batch.end();
    }

    private void drawSidePanel() {
        Stats       s      = player.getStats();
        List<Skill> skills = player.getAvailableSkills();
        List<Item>  items  = player.getInventoryItems();

        // Portrait: top-right of stats section, changes with equipment/damage state later
        final int portX = PX + W_PANEL - 162;  // x=1118
        final int portY = 517;
        final int portW = 144;
        final int portH = 155;

        shapes.begin(ShapeRenderer.ShapeType.Filled);

        // Background
        shapes.setColor(0.06f, 0.06f, 0.10f, 1);
        shapes.rect(PX, 0, W_PANEL, H);

        // Left border accent
        shapes.setColor(0.30f, 0.30f, 0.50f, 1);
        shapes.rect(PX, 0, 2, H);

        // Section dividers
        shapes.setColor(0.18f, 0.18f, 0.32f, 1);
        for (int dy : new int[]{ 680, 450, 360, 215 }) {
            shapes.rect(PX + 8, dy, W_PANEL - 16, 1);
        }

        // Portrait background + border
        shapes.setColor(0.14f, 0.14f, 0.24f, 1);
        shapes.rect(portX, portY, portW, portH);
        shapes.setColor(0.35f, 0.35f, 0.60f, 1);
        shapes.rect(portX,             portY + portH - 2, portW, 2);
        shapes.rect(portX,             portY,             portW, 2);
        shapes.rect(portX,             portY,             2,     portH);
        shapes.rect(portX + portW - 2, portY,             2,     portH);

        // Pixel-art silhouette placeholder
        int pcx = portX + portW / 2;
        shapes.setColor(0.55f, 0.60f, 0.80f, 1);
        shapes.rect(pcx - 16, portY + 105, 32, 32); // head
        shapes.rect(pcx - 18, portY +  55, 36, 50); // body
        shapes.rect(pcx - 30, portY +  58, 12, 36); // left arm
        shapes.rect(pcx + 18, portY +  58, 12, 36); // right arm
        shapes.rect(pcx - 20, portY +  10, 16, 46); // left leg
        shapes.rect(pcx +  4, portY +  10, 16, 46); // right leg

        // HP bar (full panel width)
        float hpPct = Math.max(0f, (float) player.getHp() / player.getMaxHp());
        shapes.setColor(0.20f, 0.20f, 0.20f, 1);
        shapes.rect(CX, 492, CW, 10);
        shapes.setColor(hpColor(hpPct));
        shapes.rect(CX, 492, CW * hpPct, 10);

        // XP bar
        float xpPct = player.getXpToNextLevel() > 0
                ? Math.min(1f, (float) player.getXp() / player.getXpToNextLevel()) : 0f;
        shapes.setColor(0.20f, 0.20f, 0.20f, 1);
        shapes.rect(CX, 462, CW, 10);
        shapes.setColor(0.22f, 0.52f, 0.92f, 1);
        shapes.rect(CX, 462, CW * xpPct, 10);

        shapes.end();

        batch.begin();

        // ── Header ──
        font.setColor(Color.WHITE);
        font.draw(batch, player.getName(), CX, 712);
        font.draw(batch, "Lv." + player.getLevel(), PX + W_PANEL - 60, 712);
        font.setColor(0.72f, 0.72f, 0.88f, 1);
        font.draw(batch, player.getClassName(), CX, 688);

        // ── Stats (compact 2-col left, portrait on right) ──
        font.setColor(0.62f, 0.62f, 0.88f, 1);
        font.draw(batch, "STATS", CX, 672);
        font.setColor(Color.WHITE);
        int sc2 = CX + 132;
        font.draw(batch, "STR  " + s.getModifier("strength"),     CX,  648);
        font.draw(batch, "INT  " + s.getModifier("intelligence"), sc2, 648);
        font.draw(batch, "DEX  " + s.getModifier("dexterity"),    CX,  622);
        font.draw(batch, "CON  " + s.getModifier("constitution"), sc2, 622);
        font.draw(batch, "WIS  " + s.getModifier("wisdom"),       CX,  596);

        // ── HP / XP ──
        font.setColor(Color.WHITE);
        font.draw(batch, "HP   " + Math.max(0, player.getHp()) + " / " + player.getMaxHp(), CX, 510);
        font.draw(batch, "XP   " + player.getXp() + " / " + player.getXpToNextLevel(),      CX, 480);

        // ── Equipamiento ──
        font.setColor(0.62f, 0.62f, 0.88f, 1);
        font.draw(batch, "EQUIPAMIENTO", CX, 442);
        font.setColor(Color.GRAY);
        font.draw(batch, "Arma:     (sin equipar)", CX, 416);
        font.draw(batch, "Armadura: (sin equipar)", CX, 390);

        // ── Habilidades ──
        font.setColor(0.62f, 0.62f, 0.88f, 1);
        font.draw(batch, "HABILIDADES", CX, 352);
        for (int i = 0; i < skills.size(); i++) {
            Skill skill = skills.get(i);
            font.setColor(skill.isAvailable(player) ? Color.WHITE : Color.GRAY);
            font.draw(batch, skill.getName(), CX, 326 - i * 28);
            font.draw(batch, skill.getUsesLabel(), PX + W_PANEL - 70, 326 - i * 28);
        }

        // ── Inventario ──
        font.setColor(0.62f, 0.62f, 0.88f, 1);
        font.draw(batch, "INVENTARIO", CX, 207);
        if (items.isEmpty()) {
            font.setColor(Color.GRAY);
            font.draw(batch, "(vacío)", CX, 180);
        } else {
            for (int i = 0; i < Math.min(items.size(), 6); i++) {
                font.setColor(Color.WHITE);
                font.draw(batch, items.get(i).getName(), CX, 180 - i * 26);
            }
        }

        batch.end();
    }

    private Color hpColor(float pct) {
        if (pct > 0.5f) return Color.GREEN;
        if (pct > 0.25f) return Color.YELLOW;
        return Color.RED;
    }

    @Override public void resize(int width, int height) {}
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
