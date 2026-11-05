package local.classloader;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public final class LoaderTests {
    private LoaderTests() {}

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("star-classloader-tests-");
        try {
            Path classes = root.resolve("classes");
            compile(classes,
                    "demo.SafeClass", "package demo; public class SafeClass { " +
                            "static { System.setProperty(\"classloader.test.initialized\", \"yes\"); } " +
                            "public String value() { return \"loaded\"; } }");
            System.clearProperty("classloader.test.initialized");
            LoaderEngine.Report directoryReport = load(classes);
            check(directoryReport.loaded == 1, "classes directory loads expected class");
            check(System.getProperty("classloader.test.initialized") == null,
                    "class load does not run static initializer");

            Path jarClasses = root.resolve("jar-classes");
            compile(jarClasses, "demo.JarClass", "package demo; public class JarClass {}");
            Path jar = root.resolve("valid.jar");
            createJar(jar, jarClasses.resolve("demo/JarClass.class"), "demo/JarClass.class");
            LoaderEngine.Report jarReport = load(jar);
            check(jarReport.loaded == 1, "JAR loads expected class");

            Path missingClasses = root.resolve("missing-classes");
            compile(missingClasses,
                    "missing.Api", "package missing; public interface Api {}",
                    "demo.RefersToMissing", "package demo; public class RefersToMissing { " +
                            "public missing.Api value; }");
            Files.delete(missingClasses.resolve("missing/Api.class"));
            LoaderEngine.Report missingReport = load(missingClasses);
            check(missingReport.missingDependencies.contains("missing.Api"),
                    "missing dependency is reported");

            Path tooNew = root.resolve("too-new");
            Path tooNewClass = compile(tooNew, "demo.TooNew", "package demo; public class TooNew {}");
            byte[] bytes = Files.readAllBytes(tooNewClass);
            bytes[6] = 0;
            bytes[7] = 53;
            Files.write(tooNewClass, bytes);
            expectIOException(tooNew, "Java 9 class version is rejected");

            Path invalidJar = root.resolve("invalid.jar");
            Files.write(invalidJar, "not a zip".getBytes(Charset.forName("UTF-8")));
            expectIOException(invalidJar, "corrupt archive is rejected");

            System.out.println("PASS: 5 class-loader checks");
        } finally {
            deleteTree(root);
        }
    }

    private static LoaderEngine.Report load(Path target) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(output, true)) {
            return LoaderEngine.load(target, writer);
        }
    }

    private static Path compile(Path output, String className, String source,
                                String... additionalNameAndSource) throws Exception {
        Files.createDirectories(output);
        Path sourceRoot = output.resolveSibling(output.getFileName() + "-src");
        Files.createDirectories(sourceRoot);
        compileSource(sourceRoot, className, source);
        for (int i = 0; i < additionalNameAndSource.length; i += 2) {
            compileSource(sourceRoot, additionalNameAndSource[i], additionalNameAndSource[i + 1]);
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("tests require a JDK, not a JRE");
        java.util.List<String> options = new java.util.ArrayList<String>();
        Collections.addAll(options, "-source", "1.8", "-target", "1.8", "-encoding", "UTF-8", "-d", output.toString());
        try (java.util.stream.Stream<Path> sourceFiles = Files.walk(sourceRoot)) {
            sourceFiles.filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> options.add(path.toString()));
        }
        int result = compiler.run(null, null, null, options.toArray(new String[options.size()]));
        if (result != 0) throw new IllegalStateException("fixture javac failed with exit code " + result);
        return output.resolve(className.replace('.', '/') + ".class");
    }

    private static void compileSource(Path sourceRoot, String className, String source) throws Exception {
        Path file = sourceRoot.resolve(className.replace('.', '/') + ".java");
        Files.createDirectories(file.getParent());
        Files.write(file, source.getBytes(Charset.forName("UTF-8")));
    }

    private static void createJar(Path jar, Path classFile, String entryName) throws Exception {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(entryName));
            Files.copy(classFile, output);
            output.closeEntry();
        }
    }

    private static void expectIOException(Path target, String message) throws Exception {
        try {
            TargetArchive.inspect(target);
            throw new AssertionError(message);
        } catch (java.io.IOException expected) {
            System.out.println("PASS: " + message);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }

    private static void deleteTree(Path root) throws Exception {
        try (java.util.stream.Stream<Path> paths = Files.walk(root)) {
            Path[] ordered = paths.sorted(java.util.Comparator.reverseOrder()).toArray(Path[]::new);
            for (Path path : ordered) Files.deleteIfExists(path);
        }
    }
}
