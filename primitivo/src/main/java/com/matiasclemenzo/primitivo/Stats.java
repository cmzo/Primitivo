package com.matiasclemenzo.primitivo;

/**
 * Representa los atributos base de un personaje.
 * Permite consultar y modificar cada stat individualmente.
 */

public class Stats {
    private int strength;
    private int dexterity;
    private int intelligence;
    private int constitution;
    private int wisdom;

    public Stats(int strength, int dexterity, int intelligence, int constitution, int wisdom) {
        this.strength = strength;
        this.dexterity =dexterity;
        this.intelligence = intelligence;
        this.constitution = constitution;
        this.wisdom = wisdom;
    }

    /**
     * Muestra el valor del stat ingresado
     * @param stat el nombre del stat a consultar (ej: "strength", "dexterity")
     * @return el valor del stat, o -1 si el stat no existe
     */
    public int getModifier(String stat) {
        switch (stat) {
            case "strength": return strength;
            case "dexterity": return dexterity;
            case "intelligence": return intelligence;
            case "constitution": return constitution;
            case "wisdom": return wisdom;
            default: 
                System.out.println("El stat ingresado no existe");
                return -1;
        }
    }

    /**
     * Modifica el valor del stat ingresado
     * @param stat el nombre del stat a modificar (ej: "strength", "dexterity")
     * @param value la cantidad en que el stat va a incrementarse o decrementarse 
     */
    public void applyModifier(String stat, int value) {
        switch (stat) {
            case "strength": strength += value; break;
            case "dexterity": dexterity += value; break;
            case "intelligence": intelligence += value; break;
            case "constitution": constitution += value; break;
            case "wisdom": wisdom += value; break;
            default:
                System.out.println("El stat ingresado no existe");
    }
}


}
