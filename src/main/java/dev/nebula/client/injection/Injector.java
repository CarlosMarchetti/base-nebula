package dev.nebula.client.injection;

import dev.nebula.client.injection.transformers.EntityPlayerSPTransformer;
import dev.nebula.client.injection.transformers.HookTransformer;
import dev.nebula.client.injection.transformers.MinecraftTickTransformer;
import dev.nebula.client.injection.transformers.MotionTransformer;
import dev.nebula.client.injection.transformers.NetHandlerPlayClientTransformer;
import dev.nebula.client.injection.transformers.NetworkTransformer;
import dev.nebula.client.injection.transformers.PlayerMotionTransformer;

public final class Injector {

    private Injector() {}

    public static void inject() {
        HookTransformer[] transformers = {
            new MinecraftTickTransformer(),
            new dev.nebula.client.injection.transformers.MovementInputTransformer(),
            new dev.nebula.client.injection.transformers.ClickDelayTransformer(),
            new MotionTransformer(),
            new dev.nebula.client.injection.transformers.GuiCameraTransformer(),
            new dev.nebula.client.injection.transformers.ServerRotationTransformer(),
            new dev.nebula.client.injection.transformers.InventorySyncTransformer(),
            new dev.nebula.client.injection.transformers.JumpRotationTransformer(),
            new PlayerMotionTransformer(),
            new EntityPlayerSPTransformer(),
            new NetHandlerPlayClientTransformer(),
            new NetworkTransformer(),
            new dev.nebula.client.injection.transformers.FeatureTransformer(
                    dev.nebula.client.injection.transformers.FeatureTransformer.Kind.WORLD),
            new dev.nebula.client.injection.transformers.FeatureTransformer(
                    dev.nebula.client.injection.transformers.FeatureTransformer.Kind.ENTITY),
            new dev.nebula.client.injection.transformers.FeatureTransformer(
                    dev.nebula.client.injection.transformers.FeatureTransformer.Kind.NAME),
            new dev.nebula.client.injection.transformers.FeatureTransformer(
                    dev.nebula.client.injection.transformers.FeatureTransformer.Kind.PLAYER_NAME),
            new dev.nebula.client.injection.transformers.FeatureTransformer(
                    dev.nebula.client.injection.transformers.FeatureTransformer.Kind.ATTACK)
        };
        for (HookTransformer transformer : transformers) {
            HookManager.register(transformer);
        }
        HookManager.commit();
        System.out.println("[Injector] hooks instalados");
    }
}
