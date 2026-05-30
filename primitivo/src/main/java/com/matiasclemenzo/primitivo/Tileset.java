package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Tileset data-driven: cada letra del mapa se define en tiles/tileset.cfg,
// con su PNG, región, tipo (kind) y flags. Editás la config (y el txt) y el
// juego recarga en caliente — sin tocar Java.
//
// Formato por línea:  letra  kind  png  x  y  sz  flags...
//   kind:  floor | object | inn | fence | chest
//   png:   ruta relativa a assets/, o - si no usa textura (floor de color)
//   x y sz: región en el png (px); usá - para textura completa
//   flags: solid  encounter  draw=N  tint=RRGGBB  color=RRGGBB
class Tileset {

    private static final int TILE = 32;

    static final class TileDef {
        char          letter;
        String        kind = "floor";
        TextureRegion region;    // textura/región (null si es color o no aplica)
        int           drawSize = TILE;
        Color         tint;      // tinte opcional del floor
        Color         color;     // floor de color sólido (sin textura), p.ej. agua
        boolean       solid;
        boolean       encounter;
    }

    private final List<TileDef> defs = new ArrayList<>();
    private final Map<Integer, Integer> byLetter = new HashMap<>();   // código de char → índice
    private final Map<String, Texture>  texCache = new HashMap<>();
    private int defaultIndex = 0;
    private int fenceIdx = -1;
    private TextureRegion groundReg;

    Tileset(String cfgPath) {
        for (String line : Gdx.files.internal(cfgPath).readString("UTF-8").split("\\r?\\n")) {
            String s = line.trim();
            if (s.isEmpty() || s.charAt(0) == '#') continue;
            parseLine(s);
        }
        if (byLetter.containsKey((int) '.')) defaultIndex = byLetter.get((int) '.');
        for (int i = 0; i < defs.size(); i++)
            if ("fence".equals(defs.get(i).kind)) { fenceIdx = i; break; }
        groundReg = byLetter.containsKey((int) 'G')
                ? defs.get(byLetter.get((int) 'G')).region
                : (defs.isEmpty() ? null : defs.get(defaultIndex).region);
    }

    private void parseLine(String line) {
        String[] t = line.split("\\s+");
        if (t.length < 2) return;
        TileDef d = new TileDef();
        d.letter = t[0].charAt(0);
        d.kind   = t[1];
        String png = (t.length > 2) ? t[2] : "-";

        for (int i = 6; i < t.length; i++) {
            String f = t[i];
            if (f.equals("solid"))           d.solid = true;
            else if (f.equals("encounter"))  d.encounter = true;
            else if (f.startsWith("draw="))  d.drawSize = parseInt(f.substring(5), TILE);
            else if (f.startsWith("tint="))  d.tint  = hexColor(f.substring(5));
            else if (f.startsWith("color=")) d.color = hexColor(f.substring(6));
        }

        if (!png.equals("-")) {
            Texture tex = texCache.get(png);
            if (tex == null) {
                tex = new Texture(Gdx.files.internal(png));
                tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
                texCache.put(png, tex);
            }
            boolean hasRegion = t.length > 5 && !t[3].equals("-");
            d.region = hasRegion
                    ? new TextureRegion(tex, parseInt(t[3], 0), parseInt(t[4], 0), parseInt(t[5], 16), parseInt(t[5], 16))
                    : new TextureRegion(tex);
        }

        int idx = defs.size();
        defs.add(d);
        byLetter.put((int) d.letter, idx);
    }

    int indexFor(char ch) {
        Integer i = byLetter.get((int) ch);
        return (i != null) ? i : defaultIndex;
    }

    TileDef def(int idx)       { return defs.get(idx); }
    int     fenceIndex()       { return fenceIdx; }
    TextureRegion ground()     { return groundReg; }

    TileDef firstOfKind(String kind) {
        for (TileDef d : defs) if (kind.equals(d.kind)) return d;
        return null;
    }

    void dispose() {
        for (Texture tex : texCache.values()) tex.dispose();
        texCache.clear();
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static Color hexColor(String hex) {
        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4, 6), 16);
            return new Color(r / 255f, g / 255f, b / 255f, 1f);
        } catch (Exception e) {
            return Color.WHITE;
        }
    }
}
