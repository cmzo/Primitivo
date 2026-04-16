package com.matiasclemenzo.primitivo;

import java.util.Random;

abstract class Skill {
    private String name;
    private int damage;
    private String effect;
    
    public Skill(String name, int damage, String effect) {
        this.name = name;
        this.damage = damage;
        this.effect = effect;
    }

   public void activate(Character character, Enemy enemy) {
    Random random = new Random();
    int roll = random.nextInt(20) + 1;
    
    if (roll >= 10) {
        enemy.takeDamage(damage);
        System.out.println("¡La habilidad tuvo éxito! " + enemy.getName() + " recibe " + damage + " de daño.");
    } else {
        System.out.println("Nada por aquí.. Más suerte la próxima!");
    }
}

    public boolean isAvailable(Character character) {
        return true;
    }
}
