package dev.nebula.client.runtime;

import dev.nebula.client.bridge.NativeBridge;
import dev.nebula.client.gui.GuiRenderer;
import dev.nebula.client.gui.settings.SettingsManager;
import dev.nebula.client.managers.*;
import dev.nebula.client.mappings.Mappings;
import dev.nebula.client.modules.ModuleManager;
import dev.nebula.client.utils.SaveLoad;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class ClientContext {
    private final SettingsManager settings;
    private final ModuleManager modules = new ModuleManager();
    private final ClientThreadExecutor dispatcher = new ClientThreadExecutor();
    private final ClanManager clans;
    private final AttackStatsManager stats = new AttackStatsManager();
    private final GuiRenderer gui = new GuiRenderer();
    private final EventsHandler events = new EventsHandler();
    private final SaveLoad persistence;
    private final dev.nebula.client.features.FeatureRuntime features;
    private final dev.nebula.client.gui.GuiInputController guiInput;
    private int ticks;
    private final dev.nebula.client.game.GameAccess game;
    private final dev.nebula.client.game.InventoryLease inventoryLease = new dev.nebula.client.game.InventoryLease();

    public ClientContext() {
        this(
                new SettingsManager(),
                Paths.get(System.getProperty("user.home"), "NebulaClient", "config.txt"),
                null);
    }

    public ClientContext(SettingsManager settings, Path config, Path legacy) {
        this(settings, config, legacy, new dev.nebula.client.game.MinecraftAccess());
    }

    public ClientContext(
            SettingsManager settings, Path config, Path legacy, dev.nebula.client.game.GameAccess game) {
        this.game = game;
        inventoryLease.barrier(() -> game.inventorySync() != null && game.inventorySync().active());
        guiInput = new dev.nebula.client.gui.GuiInputController(game);
        features = new dev.nebula.client.features.FeatureRuntime(this);
        this.settings = settings;
        clans = new ClanManager(settings);
        persistence = new SaveLoad(this, config, legacy);
    }

    public void initialize() throws Exception {
        Mappings.init();
        modules.initializeModules();
        persistence.load();
        NativeBridge.init();
    }

    public void tick() {
        dispatcher.drain();
        guiInput.apply();
        dev.nebula.client.utils.RotationUtils.advanceRelease();
        features.beginTick();
        events.onClientTick();
        features.endTick();
        if (++ticks % 20 == 0 && NativeBridge.isGuiOpen()) NativeBridge.syncModules();
        persistence.flushIfDirty();
    }

    public dev.nebula.client.gui.GuiInputController guiInput() {
        return guiInput;
    }

    public dev.nebula.client.features.FeatureRuntime features() {
        return features;
    }

    public dev.nebula.client.game.GameAccess game() {
        return game;
    }

    public dev.nebula.client.game.InventoryLease inventoryLease() {
        return inventoryLease;
    }

    public SettingsManager settings() {
        return settings;
    }

    public ModuleManager modules() {
        return modules;
    }

    public ClanManager clans() {
        return clans;
    }

    public AttackStatsManager stats() {
        return stats;
    }

    public GuiRenderer gui() {
        return gui;
    }

    public SaveLoad persistence() {
        return persistence;
    }

    public ClientThreadExecutor dispatcher() {
        return dispatcher;
    }
}
