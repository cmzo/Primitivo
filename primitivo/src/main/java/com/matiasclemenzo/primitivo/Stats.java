package com.matiasclemenzo.primitivo;

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
