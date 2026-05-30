package com.matiasclemenzo.primitivo;

public class Weapon extends Item {
    static final int DEFAULT_ICON = 2;   // espada (items/item2.png)

    private int attackBonus;
    private String damageType;

    public Weapon(String name, String description, int value, int attackBonus, String damageType) {
        this(name, description, value, attackBonus, damageType, DEFAULT_ICON);
    }

    public Weapon(String name, String description, int value, int attackBonus, String damageType, int iconIndex) {
        super(name, description, value, iconIndex);
        this.attackBonus = attackBonus;
        this.damageType = damageType;
    }

    @Override
    public void use(Character character) {
        // Equipar el arma
    }

    public int getAttackBonus() {
        return attackBonus;
    }
}
