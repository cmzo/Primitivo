package com.matiasclemenzo.primitivo;

public class PunaladaTrapera extends Skill {

    private int usesLeft = 3;

    public PunaladaTrapera() { super("Punalada Trapera", 0, "dex_dmg"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = (int)(character.getStats().getModifier("dexterity") * 1.5f);
        enemy.takeDamage(dmg);
        usesLeft--;
        return "Punalada Trapera! " + enemy.getName() + " recibe " + dmg + " de dano.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
