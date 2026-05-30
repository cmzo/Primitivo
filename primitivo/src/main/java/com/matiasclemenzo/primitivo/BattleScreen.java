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

    private final PrimitivoGame game;
    private final Player        player;
    private final Enemy         enemy;
    private final Screen        returnScreen;

    private static final String IDLE_PATH     = "sprites/characters/swordsman/lvl1/idle.png";
    private static final int   IDLE_FRAME_W  = 64;
    private static final int   IDLE_FRAME_H  = 64;

    private OrthographicCamera camera;
    private SpriteBatch        batch;
    private ShapeRenderer      shapes;
    private BitmapFont         font;
    private BitmapFont         fontLg;
    private SpriteSheet        battleSprite;
    private PlayerPanel        playerPanel;

    private BattleState state = BattleState.PLAYER_MENU;
    private int     selectedAction      = 0;
    private int     selectedSkill       = 0;
    private int     selectedPauseOption = 0;
    private String  message;
    private int     gainedXp  = 0;
    private boolean leveledUp = false;
    private boolean fled      = false;

    public BattleScreen(PrimitivoGame game, Player player, Enemy enemy, Screen returnScreen) {
        this.game         = game;
        this.player       = player;
        this.enemy        = enemy;
        this.returnScreen = returnScreen;
        this.message      = "¿Qué hará " + player.getName() + "?";
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void show() {
        camera       = new OrthographicCamera();
        camera.setToOrtho(false, W + W_PANEL, H);
        batch        = new SpriteBatch();
        shapes       = new ShapeRenderer();
        font         = Fonts.build(16);
        fontLg       = Fonts.build(24);
        battleSprite = new SpriteSheet(IDLE_PATH, IDLE_FRAME_W, IDLE_FRAME_H, 0.14f);
        playerPanel  = new PlayerPanel(player, battleSprite);
        MusicManager.play("audio/music/battle.ogg");
    }

    @Override
    public void render(float delta) {
        battleSprite.update(delta);
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
                        int xp = enemy.getMaxHp() / 2 + 5;
                        gainedXp  = xp;
                        leveledUp = player.gainXp(xp);
                        message = "¡Ganaste! " + enemy.getName() + " fue derrotado.";
                        state = BattleState.BATTLE_END;
                    } else {
                        executeEnemyAction();
                        if (!player.isAlive()) {
                            state = BattleState.BATTLE_END;
                        } else {
                            state = BattleState.PLAYER_MENU;
                        }
                    }
                }
                break;

            case BATTLE_END:
                if (Gdx.input.isKeyJustPressed(Keys.ENTER)) {
                    if (player.isAlive()) {
                        SaveManager.savePlayerState(player);
                        game.setScreen(returnScreen);
                    } else {
                        // Derrota: resucitar con HP mínimo en la posada, sin borrar el save
                        player.setHp(Math.max(5, player.getMaxHp() / 5));
                        SaveManager.save(player, OverworldScreen.INN_COL, OverworldScreen.INN_ROW);
                        game.setScreen(new OverworldScreen(game, player,
                                OverworldScreen.INN_COL, OverworldScreen.INN_ROW,
                                "Despertaste en la posada con HP crítico..."));
                    }
                }
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
                int dmg = player.getStats().getModifier("strength") + player.getAttackBonus();
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
                fled    = true;
                message = "Huiste de la batalla!";
                state   = BattleState.BATTLE_END;
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

        if (state == BattleState.BATTLE_END) drawBattleEndOverlay();
        if (state == BattleState.PAUSED)     drawPauseOverlay();
    }

    private void drawBattleEndOverlay() {
        boolean won = player.isAlive() && !fled;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (won) {
            shapes.setColor(0.05f, 0.12f, 0.05f, 0.82f);
        } else if (fled) {
            shapes.setColor(0.05f, 0.08f, 0.14f, 0.85f);
        } else {
            shapes.setColor(0.08f, 0.02f, 0.02f, 0.88f);
        }
        shapes.rect(0, 0, W, H);
        shapes.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.begin();

        if (won) {
            fontLg.setColor(new Color(0.85f, 0.95f, 0.55f, 1));
            fontLg.draw(batch, "¡Victoria!", 0, H / 2f + 90, W, Align.center, false);

            fontLg.setColor(new Color(0.75f, 0.88f, 0.50f, 1));
            fontLg.draw(batch, enemy.getName() + " fue derrotado.", 0, H / 2f + 40, W, Align.center, false);

            if (gainedXp > 0) {
                font.setColor(new Color(0.55f, 0.90f, 0.55f, 1));
                font.draw(batch, "+ " + gainedXp + " XP", 0, H / 2f - 10, W, Align.center, false);
            }
            if (leveledUp) {
                fontLg.setColor(new Color(1.0f, 0.85f, 0.20f, 1));
                fontLg.draw(batch, "¡SUBISTE DE NIVEL!", 0, H / 2f - 60, W, Align.center, false);
                font.setColor(new Color(0.92f, 0.75f, 0.15f, 1));
                font.draw(batch, "Nivel " + player.getLevel(), 0, H / 2f - 100, W, Align.center, false);
            }
        } else if (fled) {
            fontLg.setColor(new Color(0.75f, 0.88f, 1.0f, 1));
            fontLg.draw(batch, "¡Eso estuvo cerca!", 0, H / 2f + 70, W, Align.center, false);

            font.setColor(new Color(0.65f, 0.78f, 0.92f, 1));
            font.draw(batch, "Esta vez pudiste huir...", 0, H / 2f + 20, W, Align.center, false);
        } else {
            font.setColor(new Color(0.90f, 0.78f, 0.78f, 1));
            font.draw(batch,
                "Tu cerebro da órdenes que tu cuerpo no obedece.",
                0, H / 2f + 100, W, Align.center, false);
            font.draw(batch,
                "Las piernas dejan de responder.",
                0, H / 2f + 70, W, Align.center, false);

            fontLg.setColor(new Color(0.70f, 0.60f, 0.60f, 1));
            fontLg.draw(batch, "Todo se vuelve oscuro...", 0, H / 2f + 10, W, Align.center, false);

            font.setColor(new Color(0.55f, 0.45f, 0.45f, 1));
            font.draw(batch, "Despiertas en la posada.", 0, H / 2f - 40, W, Align.center, false);
        }

        font.setColor(new Color(0.55f, 0.55f, 0.55f, 1));
        font.draw(batch, "[ ENTER ]  continuar", 0, MENU_H + 28, W, Align.center, false);

        batch.end();
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
            shapes.setColor(0.22f, 0.20f, 0.28f, 1);
            shapes.rect(490, 375, 210, 195);
            shapes.setColor(0.30f, 0.28f, 0.38f, 1);
            shapes.rect(530, 410, 130, 130);
            shapes.setColor(0.38f, 0.35f, 0.48f, 1);
            shapes.rect(565, 445, 60, 90);
            shapes.rect(550, 500, 90, 35);
        }

        if (!player.isAlive()) {
            shapes.setColor(0.18f, 0.18f, 0.30f, 1);
            shapes.rect(50, 175, 160, 160);
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
        if (player.isAlive()) {
            battleSprite.draw(batch, SpriteSheet.DIR_DOWN, 50, 175, 160, 160);
        }
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
        } else if (state != BattleState.BATTLE_END) {
            font.draw(batch, message, 18, MENU_H - 15, W - 30, Align.left, true);
            font.setColor(0.6f, 0.6f, 0.6f, 1);
            font.draw(batch, "Presiona ENTER para continuar...", 18, 28);
        }

        batch.end();
    }

    private void drawSidePanel() {
        playerPanel.draw(shapes, batch);
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
        fontLg.dispose();
        battleSprite.dispose();
        playerPanel.dispose();
    }
}
