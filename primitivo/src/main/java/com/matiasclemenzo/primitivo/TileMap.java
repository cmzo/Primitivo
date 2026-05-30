package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.List;

// Mapa de tiles cargado desde un archivo externo en assets/maps/.
//
// Formato: una fila por línea. Cada celda es un tipo de tile, escrito como:
//   · carácter  → T=árbol  G=hierba  .=camino  W=agua  I=posada
//   · dígito     → 0=camino 1=hierba 2=árbol 3=agua 4=posada
//   · números separados por coma → "2,1,0,3,4"
// Líneas vacías o que empiezan con '#' se ignoran (comentarios). Las filas
// pueden tener distinto largo (se rellenan con camino) y el borde exterior
// se fuerza a árboles, cerrando el mapa.
class TileMap {

    // Tipos de tile — mismos índices que las paletas de color de OverworldScreen
    static final int PATH  = 0;
    static final int GRASS = 1;
    static final int TREE  = 2;
    static final int WATER = 3;
    static final int INN   = 4;

    final int rows;
    final int cols;
    private final int[][] tiles;

    TileMap(String internalPath) {
        this(Gdx.files.internal(internalPath));
    }

    TileMap(FileHandle file) {
        List<int[]> parsed = new ArrayList<>();
        int width = 0;
        for (String line : file.readString("UTF-8").split("\\r?\\n")) {
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            int[] row = parseRow(line);
            width = Math.max(width, row.length);
            parsed.add(row);
        }
        rows = parsed.size();
        cols = width;
        tiles = new int[rows][cols];
        for (int r = 0; r < rows; r++) {
            int[] row = parsed.get(r);
            for (int c = 0; c < cols; c++) {
                tiles[r][c] = (c < row.length) ? row[c] : PATH;
            }
        }
        // Borde de árboles cerrando el mapa
        for (int r = 0; r < rows; r++) { tiles[r][0] = TREE; tiles[r][cols - 1] = TREE; }
        for (int c = 0; c < cols; c++) { tiles[0][c] = TREE; tiles[rows - 1][c] = TREE; }
    }

    private static int[] parseRow(String line) {
        if (line.indexOf(',') >= 0) {
            String[] parts = line.split(",");
            int[] row = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try { row[i] = Integer.parseInt(parts[i].trim()); }
                catch (NumberFormatException e) { row[i] = PATH; }
            }
            return row;
        }
        int[] row = new int[line.length()];
        for (int i = 0; i < line.length(); i++) row[i] = charToTile(line.charAt(i));
        return row;
    }

    private static int charToTile(char ch) {
        switch (ch) {
            case 'G': case 'g': return GRASS;
            case 'T': case 't': return TREE;
            case 'W': case 'w': return WATER;
            case 'I': case 'i': return INN;
            default:
                if (ch >= '0' && ch <= '9') return ch - '0';
                return PATH;  // '.', espacio, cualquier otro
        }
    }

    int get(int col, int row) { return tiles[row][col]; }

    boolean inBounds(int col, int row) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }
}
