package com.matiasclemenzo.primitivo;

import java.util.List;

abstract class Character {
    private String name;
    private int level;
    private int hp;
    private int maxHp;
    private int xp;
    private Race race;
    private CharacterClass charClass;
    private Inventory inventory;
    private Stats stats;

    public Character(String name, int level, int hp, int xp, Stats stats, Race race, CharacterClass charClass, Inventory inventory) {
        this.name = name;
        this.level = level;
        this.hp = hp;
        this.maxHp = hp;
        this.xp = xp;
        this.stats = stats;
        this.race = race;
        this.charClass = charClass;
        this.inventory = inventory;
    }

    public String getName()     { return name; }
    public String getRaceName() { return race.getName(); }
    public int getLevel() { return level; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getXp() { return xp; }
    public int getXpToNextLevel() { return level * 100; }
    public Stats getStats() { return stats; }
    public String getClassName() { return charClass.getClassName(); }
    public List<Item> getInventoryItems() { return inventory.getItems(); }
    public void addItem(Item item) { inventory.addItem(item); }

    public boolean isAlive() {
        return hp > 0;
    }

    public void takeDamage(int damage) {
        hp = Math.max(0, hp - damage);
    }

    public void heal(int amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    public void setHp(int value) {
        hp = Math.max(0, Math.min(maxHp, value));
    }

    public List<Skill> getAvailableSkills() {
        return charClass.getAvailableSkills();
    }

    public String useSkill(Skill skill, Enemy enemy) {
        return skill.activate(this, enemy);
    }

    public void levelUp() {
        level++;
        charClass.levelUp(stats);
        maxHp += Math.max(5, stats.getModifier("constitution") / 2);
        hp = maxHp;
    }

    public boolean gainXp(int amount) {
        xp += amount;
        boolean leveled = false;
        while (xp >= getXpToNextLevel()) {
            xp -= getXpToNextLevel();
            levelUp();
            leveled = true;
        }
        return leveled;
    }
}
