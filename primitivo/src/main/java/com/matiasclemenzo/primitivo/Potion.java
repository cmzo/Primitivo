package com.matiasclemenzo.primitivo;

public class Potion extends Item {
    static final int DEFAULT_ICON = 883;   // frasco (items/item883.png) — placeholder, cambialo a gusto

     private int healAmount;

    public Potion(String name, String description, int value, int healAmount) {
        this(name, description, value, healAmount, DEFAULT_ICON);
    }

    public Potion(String name, String description, int value, int healAmount, int iconIndex) {
        super(name, description, value, iconIndex);
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
