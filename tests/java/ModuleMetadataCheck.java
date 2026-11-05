import dev.nebula.client.Main;
import dev.nebula.client.config.SettingCodec;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.gui.settings.SettingsManager;
import dev.nebula.client.modules.Module;
import dev.nebula.client.runtime.ClientContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ModuleMetadataCheck {
    static void seedProfile() throws Exception {
        if (dev.nebula.client.injection.Resources.open("/mappings/mcp-1.8.9.json") != null) return;
        try (java.io.InputStream input =
                ModuleMetadataCheck.class.getResourceAsStream("/mappings/mcp-1.8.9.json")) {
            if (input == null) throw new AssertionError("Canonical mappings absent from built JAR");
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            dev.nebula.client.injection.Resources.put("mappings/mcp-1.8.9.json", bytes.toByteArray());
        }
    }

    static ClientContext context(Path dir) throws Exception {
        seedProfile();
        ClientContext c =
                new ClientContext(
                        new SettingsManager(),
                        dir.resolve("config.txt"),
                        dir.resolve("legacy.txt"));
        java.lang.reflect.Field field = Main.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(null, c);
        return c;
    }

    public static void main(String[] args) throws Exception {
        Path target = Paths.get(args[0]);
        Files.createDirectories(target.toAbsolutePath().getParent());
        ClientContext c = context(target.getParent());
        c.modules().initializeModules();
        List<String> lines = new ArrayList<>();
        for (Module m : c.modules().getAll()) {
            lines.add(
                    "M\t"
                            + m.getName()
                            + "\t"
                            + m.getDescription()
                            + "\t"
                            + m.getCategory()
                            + "\t"
                            + m.isEnabled()
                            + "\t"
                            + m.getKey());
            for (Setting setting : c.settings().getSettingsByModule(m))
                lines.add("S\t" + String.join("\t", SettingCodec.describe(setting)));
        }
        if (c.modules().getAll().size() != 13)
            throw new AssertionError("Esperava os treze módulos da base");
        Files.write(target, lines, StandardCharsets.UTF_8);
        System.out.println("PASS real module metadata exported: " + c.modules().getAll().size());
    }
}
