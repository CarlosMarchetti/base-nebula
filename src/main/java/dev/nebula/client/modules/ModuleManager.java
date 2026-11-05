package dev.nebula.client.modules;

import dev.nebula.client.modules.combat.*;
import dev.nebula.client.modules.movement.Sprint;
import dev.nebula.client.modules.render.*;
import dev.nebula.client.utils.Log;
import java.util.*;
import java.util.ArrayList;

public class ModuleManager {

    private final List<Module> modules = Collections.synchronizedList(new ArrayList<>());
    private volatile List<Module> snapshot = Collections.emptyList();
    private final Map<String, Module> instances = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Class<? extends Module>> registry = new LinkedHashMap<>();

    public ModuleManager() {
        registerDefaultModules();
    }


    
    // Registre aqui apenas os módulos que fazem parte da base.
    public void registerDefaultModules() {
        register("killaura", KillAura.class);
        register("sprint", Sprint.class);
        register("jumpreset", dev.nebula.client.modules.movement.JumpReset.class);
        register("clickgui", ClickGui.class);
        register("arraylist", dev.nebula.client.modules.render.ArrayList.class);
        register("damageindicator", DamageIndicator.class);
        register("notifications", Notifications.class);
        register("armorfilter", ArmorFilter.class);
        register("autoclicker", AutoClicker.class);
        register("nohitdelay", NoHitDelay.class);
        register("nojumpdelay", dev.nebula.client.modules.movement.NoJumpDelay.class);
        register("hideclans", dev.nebula.client.modules.visuals.HideClans.class);
        register("nametags", dev.nebula.client.modules.visuals.Nametags.class);

        Log.info("Todos os módulos padrão registrados.");
    }

    public void register(String name, Class<? extends Module> moduleClass) {
        registry.put(name.toLowerCase(), moduleClass);
    }


    public Module createModule(String name) {
        try {
            Class<? extends Module> clazz = registry.get(name.toLowerCase());
            if (clazz == null) {
                Log.warn("Módulo desconhecido: " + name);
                return null;
            }
            Module instance = clazz.getDeclaredConstructor().newInstance();
            instance.initializeSettings();
            return instance;
        } catch (Exception e) {
            Log.error("Falha ao criar módulo " + name + ": " + e.getMessage());
            return null;
        }
    }

    public void registerModuleInstance(Module module) {
        synchronized (modules) {
            module.initializeSettings();
            if (instances.putIfAbsent(module.getName().toLowerCase(Locale.ROOT), module) != null)
                throw new IllegalArgumentException("Duplicate module: " + module.getName());
            modules.add(module);
            snapshot = Collections.unmodifiableList(new ArrayList<>(modules));
        }
    }


    public List<Module> getAll() {
        return snapshot;
    }

    public Module getModule(String name) {
        return name == null ? null : instances.get(name.toLowerCase(Locale.ROOT));
    }

    public ArrayList<Module> getModulesByCategory(String category) {
        try {
            Category categoryEnum = Category.valueOf(category);
            ArrayList<Module> modsByCategory = new ArrayList<>();
            for (Module mod : getAll()) {
                if (mod.getCategory() == categoryEnum) {
                    modsByCategory.add(mod);
                }
            }
            return modsByCategory;
        } catch (IllegalArgumentException e) {
            return new ArrayList<>();
        }
    }

    public void initializeModules() {
        for (String name : registry.keySet()) {
            Module instance = createModule(name); 
            if (instance != null) {
                registerModuleInstance(instance);
                if (instance instanceof Notifications
                        || instance instanceof dev.nebula.client.modules.render.ArrayList) instance.enable();
            }
        }
    }
}
