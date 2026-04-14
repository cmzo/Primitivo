package com.matiasclemenzo.primitivo;

import java.util.List;

abstract class CharacterClass {
    private String className;
    private String primaryStat;
    private List<Skill> skills;

    public CharacterClass(String className, String primaryStat, List<Skill> skills) {
        this.className = className;
        this.primaryStat = primaryStat;
        this.skills = skills;
    }

    public void useSkill(Skill skill, Enemy enemy) {
        //Problema para el yo del futuro
    }

    public void levelUp(Stats stats) {
        stats.applyModifier(primaryStat, 1);

    }

    public List<Skill> getAvailableSkills() {
        return skills;
    }
}

// LISTA
//List<Animal> animales = new ArrayList<>();
//animales.add(new Animal("Tigre", 5, 220));
