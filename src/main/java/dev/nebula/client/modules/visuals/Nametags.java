package dev.nebula.client.modules.visuals;

import dev.nebula.client.modules.*;

public final class Nametags extends GameModule {
    public Nametags() {
        super(
                "Nametags",
                "Nomes, vida, distancia e armaduras sobre os jogadores",
                Category.VISUALS);
    }

    public void setup() {
        keybind();
    }

    protected void tick() {
    }
}
