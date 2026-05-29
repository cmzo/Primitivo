package com.matiasclemenzo.primitivo;

public class DrenarVida extends Skill {

    private int usesLeft = 2;

    public DrenarVida() { super("Drenar Vida", 0, "drain"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = character.getStats().getModifier("intelligence");
        enemy.takeDamage(dmg);
        character.heal(dmg);
        usesLeft--;
        return "Drenar Vida! " + enemy.getName() + " pierde " + dmg + " HP, vos recuperas " + dmg + ".";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
