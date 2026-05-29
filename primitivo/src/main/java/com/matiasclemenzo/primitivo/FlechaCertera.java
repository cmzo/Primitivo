package com.matiasclemenzo.primitivo;

public class FlechaCertera extends Skill {

    private int usesLeft = 4;

    public FlechaCertera() { super("Flecha Certera", 0, "dex_arrow"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = (int)(character.getStats().getModifier("dexterity") * 1.5f);
        enemy.takeDamage(dmg);
        usesLeft--;
        return "Flecha Certera! " + enemy.getName() + " recibe " + dmg + " de dano.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
