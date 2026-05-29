package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Primitivo");
        config.setWindowedMode(1280, 720);
        new Lwjgl3Application(new PrimitivoGame(), config);
    }
}
