package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class Fonts {

    private static final String PATH  = "ui/fonts/PressStart2P-Regular.ttf";
    private static final String CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "áéíóúüñÁÉÍÓÚÜÑ¡¿àèìòùâêîôûäëïöü";

    // sm=12  cuerpo pequeño (ej. items en slots)
    // md=16  cuerpo normal
    // lg=24  títulos y texto de impacto
    public static BitmapFont build(int size) {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(Gdx.files.internal(PATH));
        FreeTypeFontParameter p  = new FreeTypeFontParameter();
        p.size       = size;
        p.characters = CHARS;
        p.minFilter  = Texture.TextureFilter.Nearest;
        p.magFilter  = Texture.TextureFilter.Nearest;
        BitmapFont f = gen.generateFont(p);
        gen.dispose();
        f.setUseIntegerPositions(true);
        return f;
    }
}
