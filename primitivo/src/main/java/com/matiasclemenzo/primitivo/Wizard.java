package com.matiasclemenzo.primitivo;

import java.util.Arrays;

public class Wizard extends CharacterClass {
    public Wizard() {
        super("Wizard", "intelligence", Arrays.asList(new BolaFuego(), new DrenarVida()));
    }
}
