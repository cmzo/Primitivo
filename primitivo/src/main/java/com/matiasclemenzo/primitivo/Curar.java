package com.matiasclemenzo.primitivo;

public class Curar extends Skill {

    private int usesLeft = 3;

    public Curar() { super("Curar", 0, "heal"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int amount = character.getStats().getModifier("wisdom") * 3;
        int before = character.getHp();
        character.heal(amount);
        int healed = character.getHp() - before;
        usesLeft--;
        return "Curar! Recuperas " + healed + " HP.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0 && c.getHp() < c.getMaxHp(); }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
