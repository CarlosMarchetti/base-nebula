package dev.nebula.client.utils;

import dev.nebula.client.config.SettingCodec;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.gui.settings.impl.KeyBindSetting;
import dev.nebula.client.modules.Module;
import dev.nebula.client.runtime.ClientContext;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SaveLoad {
    private final ClientContext context;
    private final Path destination, legacy;
    private final AtomicBoolean dirty = new AtomicBoolean();
    private volatile boolean loading;

    public SaveLoad(ClientContext context, Path destination, Path legacy) {
        this.context = context;
        this.destination = destination.toAbsolutePath().normalize();
        this.legacy = legacy;
    }

    public void requestSave() {
        if (!loading) dirty.set(true);
    }

    public void flushIfDirty() {
        if (dirty.get()) save();
    }

    public void save() {
        saveConfig(destination.toFile());
    }

    public void load() {
        Path selected = Files.isRegularFile(destination) ? destination : legacy;
        if (selected == null || !Files.isRegularFile(selected)) return;
        loadConfig(selected.toFile());
        if (!selected.equals(destination) && !Files.exists(destination)) {
            try {
                writeAtomic(destination, Files.readAllLines(selected, StandardCharsets.UTF_8));
            } catch (IOException e) {
                Log.error("Legacy configuration import failed: " + e);
            }
        }
    }

    public synchronized void saveConfig(File file) {
        if (loading) return;
        List<String> lines = new ArrayList<>();
        for (Module module : context.modules().getAll())
            lines.add(
                    "MOD:"
                            + module.getName()
                            + ":"
                            + (!module.getName().equalsIgnoreCase("ClickGui") && module.isEnabled())
                            + ":"
                            + module.getKey());
        for (Setting setting : context.settings().getSettings())
            if (setting.getParentModule() != null)
                lines.add(
                        "SET:"
                                + setting.getName()
                                + ":"
                                + setting.getParentModule().getName()
                                + ":"
                                + SettingCodec.encode(setting));
        lines.add("CLANS:ALLIED:" + SettingCodec.encodeStrings(context.clans().getAlliedClans()));
        lines.add("CLANS:ENEMY:" + SettingCodec.encodeStrings(context.clans().getEnemyClans()));
        lines.add(
                "PLAYERS:ALLIED:" + SettingCodec.encodeStrings(context.clans().getAlliedPlayers()));
        try {
            writeAtomic(file.toPath().toAbsolutePath(), lines);
            dirty.set(false);
        } catch (IOException e) {
            dirty.set(true);
            Log.error("Configuration write failed: " + e);
        }
    }

    private static void writeAtomic(Path path, List<String> lines) throws IOException {
        Files.createDirectories(path.getParent());
        Path temporary = Files.createTempFile(path.getParent(), ".nebula-config-", ".tmp");
        try {
            Files.write(temporary, lines, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public void loadConfig(File file) {
        try {
            apply(Files.readAllLines(file.toPath(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.error("Configuration read failed: " + e);
        }
    }

    public void loadConfig(InputStream stream) {
        if (stream == null) return;
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            apply(lines);
        } catch (IOException e) {
            Log.error("Configuration stream failed: " + e);
        }
    }

    private synchronized void apply(List<String> lines) {
        Map<String, String[]> modules = new LinkedHashMap<>();
        List<String[]> settings = new ArrayList<>();
        List<String> allies = new ArrayList<>(context.clans().getAlliedClans()),
                enemies = new ArrayList<>(context.clans().getEnemyClans()),
                players = new ArrayList<>(context.clans().getAlliedPlayers());
        for (String line : lines) {
            try {
                String[] parts = line.split(":", 4);
                if (parts.length < 3) continue;
                if (parts[0].equals("MOD") && parts.length == 4) modules.put(parts[1], parts);
                else if (parts[0].equals("SET") && parts.length == 4) settings.add(parts);
                else if (parts[0].equals("CLANS")) {
                    if (parts[1].equals("ALLIED")) replaceNames(allies, line.split(":", 3)[2]);
                    else if (parts[1].equals("ENEMY")) replaceNames(enemies, line.split(":", 3)[2]);
                } else if (parts[0].equals("PLAYERS") && parts[1].equals("ALLIED"))
                    replaceNames(players, line.split(":", 3)[2]);
            } catch (RuntimeException e) {
                Log.warn("Ignored malformed configuration record: " + e.getMessage());
            }
        }
        loading = true;
        try {
            context.clans().replaceAll(allies, enemies, players);
            for (String[] row : settings) {
                String owner = row[2];
                if (owner.equalsIgnoreCase("Target") && row[1].equals("Target Mode")) owner = "KillAura";
                Module module = context.modules().getModule(owner);
                if (module == null) continue;
                Setting setting = context.settings().getSettingByName(module, row[1]);
                if (setting == null) continue;
                if (setting instanceof KeyBindSetting && modules.containsKey(module.getName()))
                    continue;
                try {
                    SettingCodec.apply(setting, row[3]);
                } catch (RuntimeException e) {
                    Log.warn("Ignored invalid setting " + row[1] + ": " + e.getMessage());
                }
            }
            for (String[] row : modules.values()) {
                Module module = context.modules().getModule(row[1]);
                if (module == null) continue;
                try {
                    if (!row[2].equals("true") && !row[2].equals("false"))
                        throw new IllegalArgumentException("Invalid module boolean");
                    int key = Integer.parseInt(row[3]);
                    module.setKey(key);
                    boolean enabled =
                            Boolean.parseBoolean(row[2])
                                    && !module.getName().equalsIgnoreCase("ClickGui");
                    context.dispatcher().execute(() -> module.setEnabled(enabled));
                } catch (RuntimeException e) {
                    Log.warn("Ignored invalid module record " + row[1] + ": " + e.getMessage());
                }
            }
            dirty.set(false);
        } finally {
            loading = false;
        }
    }

    private static void replaceNames(List<String> target, String raw) {
        List<String> parsed = SettingCodec.decodeStrings(raw);
        target.clear();
        for (String value : parsed) if (!target.contains(value)) target.add(value);
    }
}
