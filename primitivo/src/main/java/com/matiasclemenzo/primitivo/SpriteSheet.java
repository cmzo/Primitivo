package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
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
    private final int[]   dirFrames;   // frames reales (no vacíos) por fila/dirección
    private final float   frameDur;

    private float stateTime = 0;

    public SpriteSheet(String path, int frameW, int frameH, float frameDur) {
        FileHandle file = Gdx.files.internal(path);
        this.texture   = new Texture(file);
        this.frameW    = frameW;
        this.frameH    = frameH;
        this.cols      = texture.getWidth() / frameW;
        this.frameDur  = frameDur;
        this.dirFrames = detectFrames(file, frameW, frameH, cols, texture.getHeight() / frameH);
    }

    // Cuenta los frames con algún pixel opaco por fila, para no ciclar columnas
    // transparentes (p.ej. el idle "arriba" del swordsman solo tiene 4 frames).
    private static int[] detectFrames(FileHandle file, int fw, int fh, int cols, int rows) {
        Pixmap pm = new Pixmap(file);
        int[] counts = new int[rows];
        for (int r = 0; r < rows; r++) {
            int last = 0;
            for (int c = 0; c < cols; c++) {
                if (frameHasOpaquePixel(pm, c * fw, r * fh, fw, fh)) last = c + 1;
            }
            counts[r] = Math.max(1, last);
        }
        pm.dispose();
        return counts;
    }

    private static boolean frameHasOpaquePixel(Pixmap pm, int x0, int y0, int w, int h) {
        int xEnd = Math.min(x0 + w, pm.getWidth());
        int yEnd = Math.min(y0 + h, pm.getHeight());
        for (int y = y0; y < yEnd; y++) {
            for (int x = x0; x < xEnd; x++) {
                if ((pm.getPixel(x, y) & 0xFF) != 0) return true;  // alpha != 0
            }
        }
        return false;
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void reset() {
        stateTime = 0;
    }

    public void draw(SpriteBatch batch, int dirRow, float x, float y, float w, float h) {
        int n = (dirRow >= 0 && dirRow < dirFrames.length) ? dirFrames[dirRow] : cols;
        int col = ((int) (stateTime / frameDur)) % n;
        TextureRegion frame = new TextureRegion(texture, col * frameW, dirRow * frameH, frameW, frameH);
        batch.draw(frame, x, y, w, h);
    }

    public void dispose() {
        texture.dispose();
    }
}
