package com.matiasclemenzo;

abstract class Character {
    private String name;
    private int level;
    private int hp;
    private int xp;
    private Race race;
    private CharacterClass charClass;
    private Inventory inventory;
    private Stats stats;

    public Character(String name, int level, int hp, int xp) {
        this.name = name;
        this.level = level;
        this.hp = hp;
        this.xp = xp;
    }

    public int getHp() {return hp; }
    public int getXp() {return xp; }

    public boolean isAlive() {
        return hp > 0;
    }

    public void takeDamage(int damage) {
    hp = hp - damage;
    }

    public void levelUp() {
        level ++;
//         stats.applyModifier(...)
    }
}
