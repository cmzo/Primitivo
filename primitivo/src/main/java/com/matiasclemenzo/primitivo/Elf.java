package com.matiasclemenzo.primitivo;

public class Elf extends Race {

    public Elf() {
        super("Elf", "Antiguos como los bosques, agudos como sus flechas");
    }

    @Override
    public void applyModifiers(Stats stats) {
        stats.applyModifier("strength", -1);
        stats.applyModifier("dexterity", 3);
        stats.applyModifier("intelligence", 2);
        stats.applyModifier("constitution", -1);
        stats.applyModifier("wisdom", 2);
    }
}