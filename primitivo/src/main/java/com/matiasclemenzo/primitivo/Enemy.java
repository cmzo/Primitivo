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
            boolean furia = hp <= maxHp * 0.4f;
            int raw = stats.getModifier("strength") * (furia ? 2 : 1);
            int dmg = Math.max(1, raw - character.getDefenseBonus());  // la armadura reduce
            character.takeDamage(dmg);
            System.out.println(name + (furia ? " ataca con furia!" : " ataca!"));
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
