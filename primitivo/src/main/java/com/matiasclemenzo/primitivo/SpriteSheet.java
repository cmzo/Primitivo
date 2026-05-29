package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SpriteSheet {

    // Direction rows — craftpix top-down swordsman (verified from runtime)
    public static final int DIR_DOWN  = 0;
    public static final int DIR_LEFT  = 1;
    public static final int DIR_RIGHT = 2;
    public static final int DIR_UP    = 3;

    private final Texture texture;
    private final int     frameW;
    private final int     frameH;
    private final int     cols;
    private final float   frameDur;

    private float stateTime = 0;

    public SpriteSheet(String path, int frameW, int frameH, float frameDur) {
        this.texture  = new Texture(Gdx.files.internal(path));
        this.frameW   = frameW;
        this.frameH   = frameH;
        this.cols     = texture.getWidth() / frameW;
        this.frameDur = frameDur;
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void reset() {
        stateTime = 0;
    }

    public void draw(SpriteBatch batch, int dirRow, float x, float y, float w, float h) {
        int col = ((int) (stateTime / frameDur)) % cols;
        TextureRegion frame = new TextureRegion(texture, col * frameW, dirRow * frameH, frameW, frameH);
        batch.draw(frame, x, y, w, h);
    }

    public void dispose() {
        texture.dispose();
    }
}
