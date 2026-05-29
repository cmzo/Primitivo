package com.matiasclemenzo.primitivo;

class StatsBox extends PanelBox {

    private final Player player;

    StatsBox(PanelTheme theme, Player player) {
        super(theme, "STATS");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        Stats s    = player.getStats();
        int   col2 = pen.x + pen.w / 2;

        pen.text2col(theme.font, theme.TEXT,
                "STR  " + s.getModifier("strength"),
                "INT  " + s.getModifier("intelligence"), col2);
        pen.text2col(theme.font, theme.TEXT,
                "DEX  " + s.getModifier("dexterity"),
                "CON  " + s.getModifier("constitution"), col2);
        pen.text(theme.font, theme.TEXT,
                "WIS  " + s.getModifier("wisdom"));

        float hpPct = Math.max(0f, (float) player.getHp() / player.getMaxHp());
        pen.text(theme.font, theme.TEXT,
                "HP  " + Math.max(0, player.getHp()) + " / " + player.getMaxHp());
        pen.bar(theme.SLOT_BG, theme.hpColor(hpPct), hpPct);
        pen.gap(PanelTheme.GAP_LINE);

        float xpPct = player.getXpToNextLevel() > 0
                ? Math.min(1f, (float) player.getXp() / player.getXpToNextLevel()) : 0f;
        pen.text(theme.font, theme.TEXT,
                "XP  " + player.getXp() + " / " + player.getXpToNextLevel());
        pen.bar(theme.SLOT_BG, theme.XP_BLUE, xpPct);
    }
}
