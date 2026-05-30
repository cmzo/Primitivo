package com.matiasclemenzo.primitivo;

public class Armor extends Item {
    static final int DEFAULT_ICON = 199;   // peto (items/item199.png)

    private int defenseBonus;

    public Armor(String name, String description, int value, int defenseBonus) {
        this(name, description, value, defenseBonus, DEFAULT_ICON);
    }

    public Armor(String name, String description, int value, int defenseBonus, int iconIndex) {
        super(name, description, value, iconIndex);
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
