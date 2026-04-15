package com.matiasclemenzo.primitivo;

public class Skill {
    private String name;
    private int manaCost;
    private int damage;
    private String effect;
    
    public Skill(String name, int manaCost, int damage, String effect) {
        this.name = name;
        this.manaCost = manaCost;
        this.damage = damage;
        this.effect = effect;
    }

    public void activate(Character character, Enemy enemy){
        //Problema para el yo del futuro
    }

    public boolean isAvailable(Character character) {

    }
}
