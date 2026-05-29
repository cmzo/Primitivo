package com.matiasclemenzo.primitivo;

public class BattleCry extends Skill {

    private boolean used = false;

    public BattleCry() {
        super("Grito de Guerra", 0, "str_boost");
    }

    @Override
    public String activate(Character character, Enemy enemy) {
        character.getStats().applyModifier("strength", 3);
        used = true;
        return "¡Grito de Guerra! STR +3 hasta el fin del combate.";
    }

    @Override
    public boolean isAvailable(Character character) { return !used; }

    @Override
    public String getUsesLabel() { return used ? "(usado)" : "(1)"; }
}
