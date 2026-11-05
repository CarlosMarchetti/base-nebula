package dev.nebula.client.modules.visuals;

import dev.nebula.client.Main;
import dev.nebula.client.game.GameAccess.Actor;
import dev.nebula.client.modules.*;
import java.util.*;

public final class HideClans extends GameModule {
    private volatile Set<Integer> hidden = Collections.emptySet();
    private Set<String> locked = new HashSet<>();
    private String mode = "";
    private int count;
    private boolean shown;

    public HideClans() {
        super("HideClans", "Oculta aliados com selecao de jogadores visiveis", Category.VISUALS);
    }

    public void setup() {
        bool("Show Allies", false);
        choice("Show Allies Mode", "Nearest", "Nearest", "Semi Auto", "Manual");
        integer("Show Allies Count", 5, 1, 14);
        strings("Manual Players");
        keybind();
    }

    protected void tick() {
        List<Actor> allies = new ArrayList<>();
        for (Actor actor : Main.getContext().features().actors())
            if (actor.kind.equals("Players") && actor.ally) allies.add(actor);
        allies.sort(Comparator.comparingDouble(a -> a.distance));
        boolean show = getValueBoolean("Show Allies");
        String selected = getValueString("Show Allies Mode");
        int n = count("Show Allies Count");
        if (show != shown || !selected.equals(mode) || n != count) {
            locked.clear();
            shown = show;
            mode = selected;
            count = n;
        }
        Set<String> visible = new HashSet<>();
        if (show) {
            if (mode.equals("Manual")) {
                for (String value : stringsValue("Manual Players"))
                    visible.add(value.toLowerCase(Locale.ROOT));
            } else if (mode.equals("Semi Auto") && !locked.isEmpty()) visible.addAll(locked);
            else {
                for (int i = 0; i < Math.min(n, allies.size()); i++)
                    visible.add(allies.get(i).name.toLowerCase(Locale.ROOT));
                if (mode.equals("Semi Auto")) locked.addAll(visible);
            }
        }
        Set<Integer> ids = new HashSet<>();
        for (Actor ally : allies)
            if (!visible.contains(ally.name.toLowerCase(Locale.ROOT))) ids.add(ally.id);
        hidden = Collections.unmodifiableSet(ids);
    }

    public boolean hides(int id) {
        return isEnabled() && hidden.contains(id);
    }

    public void onDisable() {
        hidden = Collections.emptySet();
        locked.clear();
        shown = false;
        super.onDisable();
    }

    protected void worldLost() {
        hidden = Collections.emptySet();
        locked.clear();
        shown = false;
        super.worldLost();
    }
}
