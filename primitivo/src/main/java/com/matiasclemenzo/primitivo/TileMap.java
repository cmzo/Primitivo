package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import java.util.ArrayList;
import java.util.List;

// Mapa de tiles cargado desde un archivo externo en assets/maps/. Cada carácter
// se traduce a un índice del Tileset (que define png/región/flags por letra).
// Líneas vacías o que empiezan con '#' se ignoran. Filas de distinto largo se
// rellenan con el tile por defecto ('.'), y el borde se fuerza a cerca.
class TileMap {

    final int rows;
    final int cols;
    private final int[][] tiles;   // índices en el Tileset

    TileMap(String internalPath, Tileset tileset) {
        List<int[]> parsed = new ArrayList<>();
        int width = 0;
        for (String line : Gdx.files.internal(internalPath).readString("UTF-8").split("\\r?\\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int[] row = new int[line.length()];
            for (int i = 0; i < line.length(); i++) row[i] = tileset.indexFor(line.charAt(i));
            width = Math.max(width, row.length);
            parsed.add(row);
        }
        rows = parsed.size();
        cols = width;

        int def = tileset.indexFor('.');
        tiles = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            int[] row = parsed.get(r);
            for (int c = 0; c < cols; c++) tiles[r][c] = (c < row.length) ? row[c] : def;
        }

        // Borde de cerca cerrando el mapa
        int fence = tileset.fenceIndex();
        if (fence >= 0) {
            for (int r = 0; r < rows; r++) { tiles[r][0] = fence; tiles[r][cols - 1] = fence; }
            for (int c = 0; c < cols; c++) { tiles[0][c] = fence; tiles[rows - 1][c] = fence; }
        }
    }

    int get(int col, int row) { return tiles[row][col]; }

    boolean inBounds(int col, int row) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
