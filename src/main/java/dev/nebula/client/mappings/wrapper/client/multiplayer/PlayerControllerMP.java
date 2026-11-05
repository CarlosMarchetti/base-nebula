package dev.nebula.client.mappings.wrapper.client.multiplayer;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.mappings.wrapper.client.Minecraft;
import dev.nebula.client.mappings.wrapper.client.entity.EntityPlayerSP;
import dev.nebula.client.mappings.wrapper.entity.Entity;
import dev.nebula.client.utils.ReflectionUtils;

public final class PlayerControllerMP {
    private final Object handle;
    public PlayerControllerMP(Object handle) { this.handle = handle; }
    public Object getHandle() { return handle; }
    public void attackEntity(Entity target) {
        EntityPlayerSP player = Minecraft.getPlayer();
        if (handle == null || player == null || target == null || target.getHandle() == null)
            throw new IllegalStateException("Vanilla attack controller/player/target unavailable");
        if (Mappings.PLAYER_CONTROLLER_ATTACK_ENTITY == null)
            throw new IllegalStateException("PLAYER_CONTROLLER_ATTACK_ENTITY mapping unavailable");
        try {
            ReflectionUtils.invokeMethod(Mappings.PLAYER_CONTROLLER_ATTACK_ENTITY, handle, player.getHandle(), target.getHandle());
        } catch (Exception e) { throw new IllegalStateException("Vanilla attack failed", e); }
    }
}
