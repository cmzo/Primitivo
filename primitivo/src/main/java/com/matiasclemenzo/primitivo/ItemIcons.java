package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.HashMap;
import java.util.Map;

// Carga (perezosa) y cachea los íconos 16×16 de assets/items/item<idx>.png.
// El caché es estático: se comparte entre pantallas y se carga una sola vez por índice.
class ItemIcons {

    private static final Map<Integer, TextureRegion> cache = new HashMap<>();

    // Devuelve la región del ícono, o null si index <= 0 (sin ícono).
    static TextureRegion get(int index) {
        if (index <= 0) return null;
        TextureRegion reg = cache.get(index);
        if (reg == null) {
            Texture tex = new Texture(Gdx.files.internal("items/item" + index + ".png"));
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            reg = new TextureRegion(tex);
            cache.put(index, reg);
        }
        return reg;
    }

    static void disposeAll() {
        for (TextureRegion r : cache.values()) r.getTexture().dispose();
        cache.clear();
    }
}
