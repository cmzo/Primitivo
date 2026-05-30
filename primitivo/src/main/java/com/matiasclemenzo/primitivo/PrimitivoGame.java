package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Game;

public class PrimitivoGame extends Game {

    @Override
    public void create() {
        setScreen(new TitleScreen(this));
    }
}
