package com.matiasclemenzo.primitivo;

import java.util.Arrays;

public class Fighter extends CharacterClass {

    public Fighter() {
        super("Fighter", "strength", Arrays.asList(new PowerStrike(), new BattleCry()));
    }
}
