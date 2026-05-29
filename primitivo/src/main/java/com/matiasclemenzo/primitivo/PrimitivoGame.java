package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Game;

public class PrimitivoGame extends Game {

    @Override
    public void create() {
        if (SaveManager.hasSave()) {
            SaveManager.SaveData data = SaveManager.load();
            setScreen(new OverworldScreen(this, data.player, data.col, data.row));
        } else {
            setScreen(new CharacterCreationScreen(this));
        }
    }
}
