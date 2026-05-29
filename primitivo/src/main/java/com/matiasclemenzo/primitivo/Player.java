package com.matiasclemenzo.primitivo;

import java.util.ArrayList;

public class Player extends Character {

    public Player(String name, Race race, CharacterClass charClass) {
        super(name, 1, computeHp(computeStats(race)), 0, computeStats(race), race, charClass, new Inventory(new ArrayList<>()));
    }

    // Constructor for loading from save — stats are final values, HP is restored via takeDamage
    Player(String name, Race race, CharacterClass charClass, Stats stats, int level, int hp, int maxHp, int xp) {
        super(name, level, maxHp, xp, stats, race, charClass, new Inventory(new ArrayList<>()));
        if (hp < maxHp) takeDamage(maxHp - hp);
    }

    private static Stats computeStats(Race race) {
        Stats stats = new Stats(10, 10, 10, 10, 10);
        race.applyModifiers(stats);
        return stats;
    }

    private static int computeHp(Stats stats) {
        return stats.getModifier("constitution") * 4 + 10;
    }
}
