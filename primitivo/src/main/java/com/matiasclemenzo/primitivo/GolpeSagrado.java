package com.matiasclemenzo.primitivo;

public class GolpeSagrado extends Skill {

    private int usesLeft = 3;

    public GolpeSagrado() { super("Golpe Sagrado", 0, "holy"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = (int)(character.getStats().getModifier("wisdom") * 1.5f);
        enemy.takeDamage(dmg);
        usesLeft--;
        return "Golpe Sagrado! " + enemy.getName() + " recibe " + dmg + " de dano sagrado.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
