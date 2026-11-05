package local.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class TargetArchive {
    private static final int MAX_CLASSES = 20000;
    private static final long MAX_CLASS_BYTES = 256L * 1024L * 1024L;

    final Path path;
    final List<ClassFileInfo> classes;

    private TargetArchive(Path path, List<ClassFileInfo> classes) {
        this.path = path;
        this.classes = Collections.unmodifiableList(classes);
    }

    static TargetArchive inspect(Path input) throws IOException {
        if (input == null) {
            throw new IOException("no target path provided");
        }
        Path target = input.toAbsolutePath().normalize();
        if (!Files.exists(target)) {
            throw new IOException("target does not exist: " + target);
        }
        if (Files.isDirectory(target)) {
            return inspectDirectory(target);
        }
        if (!Files.isRegularFile(target) || !target.getFileName().toString().toLowerCase().endsWith(".jar")) {
            throw new IOException("target must be a .jar file or a classes directory");
        }
        return inspectJar(target);
    }

    private static TargetArchive inspectDirectory(Path root) throws IOException {
        final Map<String, ClassFileInfo> classes = new LinkedHashMap<String, ClassFileInfo>();
        final long[] totalBytes = {0L};
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            java.util.Iterator<Path> iterator = paths.filter(Files::isRegularFile).iterator();
            while (iterator.hasNext()) {
                Path file = iterator.next();
                if (!file.getFileName().toString().endsWith(".class")) {
                    continue;
                }
                byte[] bytes = readLimited(Files.newInputStream(file), MAX_CLASS_BYTES);
                totalBytes[0] += bytes.length;
                if (totalBytes[0] > MAX_CLASS_BYTES) {
                    throw new IOException("class files exceed the 256 MiB inspection limit");
                }
                String relative = root.relativize(file).toString().replace(File.separatorChar, '/');
                addClass(classes, relative, bytes);
            }
        }
        return finish(root, classes);
    }

    private static TargetArchive inspectJar(Path jar) throws IOException {
        Map<String, ClassFileInfo> classes = new LinkedHashMap<String, ClassFileInfo>();
        long totalBytes = 0L;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                validateEntryName(entry.getName());
                if (entry.isDirectory() || !entry.getName().endsWith(".class") ||
                        entry.getName().startsWith("META-INF/versions/")) {
                    continue;
                }
                byte[] bytes = readLimited(zip.getInputStream(entry), MAX_CLASS_BYTES);
                totalBytes += bytes.length;
                if (totalBytes > MAX_CLASS_BYTES) {
                    throw new IOException("class files exceed the 256 MiB inspection limit");
                }
                addClass(classes, entry.getName(), bytes);
            }
        }
        return finish(jar, classes);
    }

    private static void validateEntryName(String name) throws IOException {
        if (name.startsWith("/") || name.indexOf('\\') >= 0) {
            throw new IOException("unsafe archive entry path: " + name);
        }
        for (String segment : name.split("/")) {
            if ("..".equals(segment)) {
                throw new IOException("unsafe archive entry path: " + name);
            }
        }
    }

    private static byte[] readLimited(InputStream input, long limit) throws IOException {
        try (InputStream source = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            long total = 0L;
            while ((read = source.read(buffer)) != -1) {
                total += read;
                if (total > limit) {
                    throw new IOException("class file exceeds the inspection limit");
                }
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        }
    }

    private static void addClass(Map<String, ClassFileInfo> classes, String entryName, byte[] bytes) throws IOException {
        ClassFileInfo info = ClassFileInfo.parse(bytes);
        String expectedName = entryName.substring(0, entryName.length() - ".class".length()).replace('/', '.');
        if (!expectedName.equals(info.binaryName)) {
            throw new IOException("class path/name mismatch: " + entryName + " contains " + info.binaryName);
        }
        if (classes.put(info.binaryName, info) != null) {
            throw new IOException("duplicate class entry: " + info.binaryName);
        }
        if (classes.size() > MAX_CLASSES) {
            throw new IOException("target exceeds the 20,000 class inspection limit");
        }
    }

    private static TargetArchive finish(Path path, Map<String, ClassFileInfo> classes) throws IOException {
        if (classes.isEmpty()) {
            throw new IOException("target contains no Java 8-compatible class files");
        }
        return new TargetArchive(path, new ArrayList<ClassFileInfo>(classes.values()));
    }
}
