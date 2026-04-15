package com.matiasclemenzo.primitivo;

import java.util.List;

/**
 * Muestra la mochila del personaje
 * 
 * 
 */
public class Inventory {
    private List<Item> items;
    private Weapon equippedWeapon;
    private Armor equippedArmor;
    
    public Inventory(List<Item> items) {
        this.items = items;

    }

    /**
    * Añade un item al listado de items
    * @param item es el objeto que deseo añadir a la lista
    * 
    */    
    public void addItem(Item item) {
        items.add(item);
    }

    /**
    * Quita un item del listado de items
    * @param item es el objeto que deseo quitar de la lista
    * 
    */
    public void removeItem(Item item) {
        items.remove(item);
    }

    /**
    * Verifica si el item es un arma o una armadura y la equipa según corresponda
    * @param item es el objeto que deseo equipar
    * 
    */
    public void equipItem(Item item) {
        if (item instanceof Weapon) {
            equippedWeapon = (Weapon) item;
        } else if (item instanceof Armor) {
            equippedArmor = (Armor) item;
        } else {
            System.out.println("Este item no se puede equipar");
        }
}

    public void listItems() {
        for (Item item : items) {
            System.out.println(item.getName());
        }
}
}

