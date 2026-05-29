package com.matiasclemenzo.primitivo;

public class LluviaFlechas extends Skill {

    private int usesLeft = 2;

    public LluviaFlechas() { super("Lluvia de Flechas", 0, "multi_arrow"); }

    @Override
    public String activate(Character character, Enemy enemy) {
        int hits       = 2 + (Math.random() < 0.4 ? 1 : 0);
        int dmgPerHit  = (int)(character.getStats().getModifier("dexterity") * 0.75f);
        int total      = hits * dmgPerHit;
        enemy.takeDamage(total);
        usesLeft--;
        return "Lluvia de Flechas! " + hits + " impactos x" + dmgPerHit + " = " + total + " de dano.";
    }

    @Override public boolean isAvailable(Character c) { return usesLeft > 0; }
    @Override public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
