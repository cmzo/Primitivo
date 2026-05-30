package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

// Música de fondo en streaming (no carga el archivo entero en memoria).
// Tolera que el archivo no exista (no rompe si todavía no agregaste el .ogg).
// LibGDX reproduce OGG/MP3/WAV; preferí OGG: pesa ~10-20× menos que WAV.
class MusicManager {

    private static Music  current;
    private static String currentPath;
    private static float  volume = 0.5f;

    static void play(String internalPath) {
        FileHandle f = Gdx.files.internal(internalPath);
        if (!f.exists()) return;                       // todavía no hay archivo: no pasa nada
        if (internalPath.equals(currentPath) && current != null) return;  // ya sonando
        stop();
        current = Gdx.audio.newMusic(f);
        current.setLooping(true);
        current.setVolume(volume);
        current.play();
        currentPath = internalPath;
    }

    static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        if (current != null) current.setVolume(volume);
    }

    static float getVolume() { return volume; }

    static void stop() {
        if (current != null) {
            current.stop();
            current.dispose();
            current = null;
            currentPath = null;
        }
    }
}
