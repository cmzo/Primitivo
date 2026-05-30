package com.matiasclemenzo.primitivo;

/**
 * Representa cualquier objeto que un personaje puede llevar en su inventario.
 * Es la clase base de la que heredan Weapon, Armor y Potion.
 */
abstract class Item {
    private String name;
    private String description;
    private int value;
    private int iconIndex;   // índice de items/item<idx>.png (0 = sin ícono)

    public Item(String name, String description, int value, int iconIndex) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.iconIndex = iconIndex;
    }

    public int getIconIndex() {
        return iconIndex;
    }

    /**
     * Usa el item sobre el personaje indicado.
     * El efecto concreto depende del tipo de item.
     * @param character el personaje sobre el que se aplica el item
     */
    public void use( Character character) {

    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getValue() {
        return value;
    }
}
