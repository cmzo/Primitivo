package com.matiasclemenzo.primitivo;

public class PowerStrike extends Skill {

    private int usesLeft = 3;

    public PowerStrike() {
        super("Golpe Certero", 0, "guaranteed");
    }

    @Override
    public String activate(Character character, Enemy enemy) {
        int dmg = (int)(character.getStats().getModifier("strength") * 1.5f);
        enemy.takeDamage(dmg);
        usesLeft--;
        return "¡Golpe Certero! " + enemy.getName() + " recibe " + dmg + " de daño.";
    }

    @Override
    public boolean isAvailable(Character character) { return usesLeft > 0; }

    @Override
    public String getUsesLabel() { return "(" + usesLeft + ")"; }
}
