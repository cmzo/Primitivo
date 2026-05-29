package com.matiasclemenzo.primitivo;

import java.util.Arrays;

public class Healer extends CharacterClass {
    public Healer() {
        super("Healer", "wisdom", Arrays.asList(new Curar(), new GolpeSagrado()));
    }
}
