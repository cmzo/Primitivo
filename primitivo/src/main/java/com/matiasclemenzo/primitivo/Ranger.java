package com.matiasclemenzo.primitivo;

import java.util.Arrays;

public class Ranger extends CharacterClass {
    public Ranger() {
        super("Ranger", "dexterity", Arrays.asList(new FlechaCertera(), new LluviaFlechas()));
    }
}
