package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SaveManager {

    private static final String PREFS_BASE = "primitivo_save";

    // ── Slot helpers ─────────────────────────────────────────────────────────

    private static String prefsKey(int slot) {
        return slot == 0 ? PREFS_BASE : PREFS_BASE + "_" + slot;
    }

    // ── Single-slot API (slot 0, backward-compat) ─────────────────────────

    public static boolean hasSave()                                     { return hasSave(0); }
    public static void    save(Player p, int col, int row)              { save(p, col, row, 0); }
    public static void    savePlayerState(Player player)                { savePlayerState(player, 0); }
    public static SaveData load()                                       { return load(0); }
    public static void    deleteSave()                                  { deleteSave(0); }

    // ── Multi-slot API ────────────────────────────────────────────────────

    public static boolean hasSave(int slot) {
        return Gdx.app.getPreferences(prefsKey(slot)).getBoolean("exists", false);
    }

    public static void save(Player player, int col, int row, int slot) {
        Preferences p = Gdx.app.getPreferences(prefsKey(slot));
        writePlayer(p, player);
        p.putInteger("col", col);
        p.putInteger("row", row);
        p.flush();
    }

    public static void savePlayerState(Player player, int slot) {
        Preferences p = Gdx.app.getPreferences(prefsKey(slot));
        if (!p.getBoolean("exists", false)) return;
        writePlayer(p, player);
        p.flush();
    }

    public static SaveData load(int slot) {
        Preferences p = Gdx.app.getPreferences(prefsKey(slot));

        String raceName  = p.getString("race",  "Human");
        String className = p.getString("class", "Fighter");
        String name      = p.getString("name",  "Heroe");
        int    level     = p.getInteger("level", 1);
        int    hp        = p.getInteger("hp",    50);
        int    maxHp     = p.getInteger("maxHp", 50);
        int    xp        = p.getInteger("xp",    0);
        int    col       = p.getInteger("col",   12);
        int    row       = p.getInteger("row",   11);

        Stats stats = new Stats(
            p.getInteger("str", 10),
            p.getInteger("dex", 10),
            p.getInteger("int", 10),
            p.getInteger("con", 10),
            p.getInteger("wis", 10)
        );

        Race          race      = buildRace(raceName);
        CharacterClass charClass = buildClass(className);
        Player         player   = new Player(name, race, charClass, stats, level, hp, maxHp, xp);
        readInventory(p, player);
        return new SaveData(player, col, row);
    }

    public static void deleteSave(int slot) {
        Preferences p = Gdx.app.getPreferences(prefsKey(slot));
        p.clear();
        p.flush();
    }

    public static SlotInfo getSlotInfo(int slot) {
        Preferences p = Gdx.app.getPreferences(prefsKey(slot));
        if (!p.getBoolean("exists", false)) return new SlotInfo(slot, null, 0, false);
        return new SlotInfo(slot, p.getString("name", "?"), p.getInteger("level", 1), true);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void writePlayer(Preferences p, Player player) {
        Stats s = player.getStats();
        p.putBoolean("exists",  true);
        p.putString("name",     player.getName());
        p.putString("race",     player.getRaceName());
        p.putString("class",    player.getClassName());
        p.putInteger("level",   player.getLevel());
        p.putInteger("hp",      Math.max(0, player.getHp()));
        p.putInteger("maxHp",   player.getMaxHp());
        p.putInteger("xp",      player.getXp());
        p.putInteger("str",     s.getModifier("strength"));
        p.putInteger("dex",     s.getModifier("dexterity"));
        p.putInteger("int",     s.getModifier("intelligence"));
        p.putInteger("con",     s.getModifier("constitution"));
        p.putInteger("wis",     s.getModifier("wisdom"));
        writeInventory(p, player);
    }

    // ── Inventario (serialización) ─────────────────────────────────────────

    private static void writeInventory(Preferences p, Player player) {
        Inventory inv = player.getInventory();
        StringBuilder bag = new StringBuilder();
        for (Item it : inv.getItems()) {
            if (bag.length() > 0) bag.append('\n');
            bag.append(serializeItem(it));
        }
        p.putString("inv",    bag.toString());
        p.putString("equipW", inv.getEquippedWeapon() != null ? serializeItem(inv.getEquippedWeapon()) : "");
        p.putString("equipA", inv.getEquippedArmor()  != null ? serializeItem(inv.getEquippedArmor())  : "");
    }

    private static void readInventory(Preferences p, Player player) {
        String bag = p.getString("inv", "");
        if (!bag.isEmpty()) {
            for (String line : bag.split("\n")) {
                Item it = deserializeItem(line);
                if (it != null) player.addItem(it);
            }
        }
        Item w = deserializeItem(p.getString("equipW", ""));
        if (w != null) player.getInventory().equip(w);
        Item a = deserializeItem(p.getString("equipA", ""));
        if (a != null) player.getInventory().equip(a);
    }

    // Formato por ítem (campos separados por '|'):
    //   W|name|desc|value|icon|attackBonus|damageType
    //   A|name|desc|value|icon|defenseBonus
    //   P|name|desc|value|icon|healAmount
    private static String serializeItem(Item it) {
        String common = it.getName() + "|" + it.getDescription() + "|" + it.getValue() + "|" + it.getIconIndex();
        if (it instanceof Weapon) {
            Weapon w = (Weapon) it;
            return "W|" + common + "|" + w.getAttackBonus() + "|" + w.getDamageType();
        }
        if (it instanceof Armor)  return "A|" + common + "|" + ((Armor) it).getDefenseBonus();
        if (it instanceof Potion) return "P|" + common + "|" + ((Potion) it).getHealAmount();
        return "";
    }

    private static Item deserializeItem(String s) {
        if (s == null || s.isEmpty()) return null;
        String[] f = s.split("\\|", -1);
        if (f.length < 6) return null;
        String name = f[1], desc = f[2];
        int value = parseInt(f[3], 0), icon = parseInt(f[4], 0);
        switch (f[0]) {
            case "W": return new Weapon(name, desc, value, parseInt(f[5], 0), f.length > 6 ? f[6] : "", icon);
            case "A": return new Armor (name, desc, value, parseInt(f[5], 0), icon);
            case "P": return new Potion(name, desc, value, parseInt(f[5], 0), icon);
            default:  return null;
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static Race buildRace(String name) {
        switch (name) {
            case "Elf":   return new Elf();
            case "Dwarf": return new Dwarf();
            case "Orc":   return new Orc();
            default:      return new Human();
        }
    }

    private static CharacterClass buildClass(String name) {
        switch (name) {
            case "Wizard":  return new Wizard();
            case "Rogue":   return new Rogue();
            case "Healer":  return new Healer();
            case "Ranger":  return new Ranger();
            default:        return new Fighter();
        }
    }

    // ── Data classes ──────────────────────────────────────────────────────────

    public static class SaveData {
        public final Player player;
        public final int    col;
        public final int    row;

        SaveData(Player player, int col, int row) {
            this.player = player;
            this.col    = col;
            this.row    = row;
        }
    }

    public static class SlotInfo {
        public final int     slot;
        public final String  name;
        public final int     level;
        public final boolean exists;

        SlotInfo(int slot, String name, int level, boolean exists) {
            this.slot   = slot;
            this.name   = name;
            this.level  = level;
            this.exists = exists;
        }
    }
}
