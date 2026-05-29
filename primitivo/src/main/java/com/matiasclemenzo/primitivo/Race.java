package com.matiasclemenzo.primitivo;

abstract class Race {
    private String name;
    private String description;
    
    public Race(String name, String description){
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }

    public void applyModifiers(Stats stats){

    }
}
