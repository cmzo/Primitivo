package com.matiasclemenzo.primitivo;

import java.util.Arrays;

public class Rogue extends CharacterClass {
    public Rogue() {
        super("Rogue", "dexterity", Arrays.asList(new PunaladaTrapera(), new AtaqueDoble()));
    }
}
