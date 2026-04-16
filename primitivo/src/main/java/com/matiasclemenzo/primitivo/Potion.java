package com.matiasclemenzo.primitivo;

public class Potion extends Item {
     private int healAmount;

    public Potion(String name, String description, int value, int healAmount) {
        super(name, description, value);
        this.healAmount = healAmount;
    }

    @Override
    public void use(Character character) {
        character.heal(healAmount);
        System.out.println("Eso se siente bien! Recuperas " + healAmount + " puntos de vida.");
    }

    // TODO: Hacer seguimiento. Tal vez se pueda quitar
    public int getHealAmount() {
        return healAmount;
}

}
