package com.matiasclemenzo.primitivo;

public class Dwarf extends Race {

    public Dwarf() {
        super("Dwarf", "Forjados en piedra y fuego, inamovibles como la montaña");
    }

    @Override
    public void applyModifiers(Stats stats) {
        stats.applyModifier("strength", 3);
        stats.applyModifier("dexterity", -1);
        stats.applyModifier("intelligence", 1);
        stats.applyModifier("constitution", 3);
        stats.applyModifier("wisdom", 1);
    }
}