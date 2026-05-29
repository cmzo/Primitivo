package com.matiasclemenzo.primitivo;

public class BolaFuego extends Skill {

    private int usesLeft = 3;

    public BolaFuego() { super("Bola de Fuego", 0, "int_dmg"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = character.getStats().getModifier("intelligence") * 2;
        enemy.takeDamage(dmg);
        usesLeft--;
        return "Bola de Fuego! " + enemy.getName() + " recibe " + dmg + " de dano arcano.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
