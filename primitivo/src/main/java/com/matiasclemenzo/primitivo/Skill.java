package com.matiasclemenzo.primitivo;

abstract class Skill {
    private String name;
    private int damage;
    private String effect;

    public Skill(String name, int damage, String effect) {
        this.name = name;
        this.damage = damage;
        this.effect = effect;
    }

    public String getName() { return name; }

    public String activate(Character character, Enemy enemy) {
        enemy.takeDamage(damage);
        return "¡" + name + "! " + enemy.getName() + " recibe " + damage + " de daño.";
    }

    public boolean isAvailable(Character character) { return true; }

    public String getUsesLabel() { return ""; }
}
