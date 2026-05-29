package com.matiasclemenzo.primitivo;

public class AtaqueDoble extends Skill {

    private int usesLeft = 3;

    public AtaqueDoble() { super("Ataque Doble", 0, "double"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int hit = character.getStats().getModifier("dexterity");
        enemy.takeDamage(hit * 2);
        usesLeft--;
        return "Ataque Doble! Dos golpes de " + hit + " = " + (hit * 2) + " de dano total.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
