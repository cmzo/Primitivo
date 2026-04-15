package com.matiasclemenzo.primitivo;

public class Weapon extends Item {
    private int attackBonus;
    private String damageType;

    public Weapon(String name, String description, int value, int attackBonus, String damageType) {
        super(name, description, value);
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
