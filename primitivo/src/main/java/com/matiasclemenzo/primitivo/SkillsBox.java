package com.matiasclemenzo.primitivo;

import com.badlogic.gdx.utils.Align;
import java.util.List;

class SkillsBox extends PanelBox {

    private static final int MAX_SKILLS = 3;
    private final Player player;

    SkillsBox(PanelTheme theme, Player player) {
        super(theme, "HABILIDADES");
        this.player = player;
    }

    @Override
    void layout(Pen pen) {
        List<Skill> skills = player.getAvailableSkills();
        int count = Math.min(skills.size(), MAX_SKILLS);
        for (int i = 0; i < count; i++) {
            Skill skill = skills.get(i);
            if (pen.mode == Pen.Mode.TEXT) {
                theme.font.setColor(skill.isAvailable(player) ? theme.TEXT : theme.TEXT_DIM);
                theme.font.draw(pen.batch, skill.getName(), pen.x, pen.y);
                theme.font.setColor(theme.TEXT_DIM);
                theme.font.draw(pen.batch, skill.getUsesLabel(),
                        pen.x, pen.y, pen.w, Align.right, false);
            }
            pen.gap(pen.lineH(theme.font));
        }
    }
}
