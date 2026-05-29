package com.matiasclemenzo.primitivo;

public class Armor extends Item {
    private int defenseBonus;

    public Armor(String name, String description, int value, int defenseBonus) {
        super(name, description, value);
        this.defenseBonus = defenseBonus;
    }

    @Override
    public void use(Character character) {
        // Equipar la armadura
    }

    public int getDefenseBonus() {
        return defenseBonus;
    }
}
