package com.matiasclemenzo.primitivo;

public class Human extends Race {

    public Human() {
        super("Human", "Adaptables y ambiciosos, los humanos prosperan donde otros fracasan");
    }

    @Override
    public void applyModifiers(Stats stats) {
        stats.applyModifier("strength", 2);
        stats.applyModifier("dexterity", 2);
        stats.applyModifier("intelligence", 1);
        stats.applyModifier("constitution", 1);
        stats.applyModifier("wisdom", 1);
    }
}