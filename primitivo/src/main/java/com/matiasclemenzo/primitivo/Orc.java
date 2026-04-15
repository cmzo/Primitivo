package com.matiasclemenzo.primitivo;

public class Orc extends Race {

    public Orc() {
        super("Orc", "Su furia es su filosofía, su cuerpo su arma\"");
    }

    @Override
    public void applyModifiers(Stats stats) {
        stats.applyModifier("strength", 5);
        stats.applyModifier("dexterity", 1);
        stats.applyModifier("intelligence", -2);
        stats.applyModifier("constitution", 2);
        stats.applyModifier("wisdom", 1);
    }
}