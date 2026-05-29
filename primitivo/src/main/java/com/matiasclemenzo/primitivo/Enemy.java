    package com.matiasclemenzo.primitivo;

    import java.util.List;


    public class Enemy {
        private String name;
        private int hp;
        private int maxHp;
        private Stats stats;
        private List<Item> lootTable;
        private int xpReward;

        public Enemy(String name, int hp, List<Item> lootTable, int xpReward, Stats stats) {
            this.name = name;
            this.hp = hp;
            this.maxHp = hp;
            this.lootTable = lootTable;
            this.xpReward = xpReward;
            this.stats = stats;
        }

        public void takeDamage(int damage) {
            hp -= damage;
        }

        public boolean isAlive() {
            return hp > 0;
        }

        public List<Item> dropLoot() {
            return lootTable;
        }

        public void chooseAction(Character character) {
            if (hp > maxHp * 0.4f) {
                character.takeDamage(stats.getModifier("strength"));
                System.out.println(name + " ataca!");
            } else {
                character.takeDamage(stats.getModifier("strength") * 2);
                System.out.println(name + " ataca con furia!");
            }
        }

        public int getHp() { return hp; }
        public int getMaxHp() { return maxHp; }

        public String getName() {
            return name;
        }

        public int xpReward() {
            return xpReward;
        }
    }
