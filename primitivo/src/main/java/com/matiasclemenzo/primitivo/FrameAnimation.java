package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FrameAnimation {

    private final Texture[] frames;
    private final float     frameDur;
    private float stateTime = 0;

    public FrameAnimation(float frameDur, String... paths) {
        frames = new Texture[paths.length];
        for (int i = 0; i < paths.length; i++) {
            frames[i] = new Texture(Gdx.files.internal(paths[i]));
        }
        this.frameDur = frameDur;
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void draw(SpriteBatch batch, float x, float y, float w, float h) {
        int idx = ((int)(stateTime / frameDur)) % frames.length;
        batch.draw(frames[idx], x, y, w, h);
    }

    public void dispose() {
        for (Texture t : frames) t.dispose();
    }

    // Helper: genera paths para frames numerados (frame_000.png … frame_NNN.png)
    public static String[] elfPaths(int from, int to) {
        String[] paths = new String[to - from + 1];
        String base = "sprites/characters/elf/sprite-max-px-frames-36-rows-6-cols-6-frames/frame_%03d.png";
        for (int i = 0; i <= to - from; i++) {
            paths[i] = String.format(base, from + i);
        }
        return paths;
    }
}
