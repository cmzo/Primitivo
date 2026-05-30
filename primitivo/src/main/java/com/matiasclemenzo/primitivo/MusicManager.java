package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;

// Música de fondo en streaming (no carga el archivo entero en memoria).
// Tolera que el archivo no exista (no rompe si todavía no agregaste el .ogg).
// LibGDX reproduce OGG/MP3/WAV; preferí OGG: pesa ~10-20× menos que WAV.
class MusicManager {

    private static Music   current;
    private static String  currentPath;
    private static float   volume = 0.5f;
    private static boolean muted  = false;

    static void play(String internalPath) {
        FileHandle f = Gdx.files.internal(internalPath);
        if (!f.exists()) return;                       // todavía no hay archivo: no pasa nada
        if (internalPath.equals(currentPath) && current != null) return;  // ya sonando
        stop();
        current = Gdx.audio.newMusic(f);
        current.setLooping(true);
        current.setVolume(effectiveVolume());
        current.play();
        currentPath = internalPath;
    }

    static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        applyVolume();
    }

    static float getVolume() { return volume; }

    // Silencia/reactiva la música; devuelve el nuevo estado (true = silenciada).
    static boolean toggleMute() {
        muted = !muted;
        applyVolume();
        return muted;
    }

    static boolean isMuted() { return muted; }

    static void stop() {
        if (current != null) {
            current.stop();
            current.dispose();
            current = null;
            currentPath = null;
        }
    }

    private static float effectiveVolume() { return muted ? 0f : volume; }

    private static void applyVolume() {
        if (current != null) current.setVolume(effectiveVolume());
    }
}
