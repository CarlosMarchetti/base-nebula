package local.classloader;

import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

final class LoaderEngine {
    static final class Report {
        final int loaded;
        final int failed;
        final Set<String> missingDependencies;

        Report(int loaded, int failed, Set<String> missingDependencies) {
            this.loaded = loaded;
            this.failed = failed;
            this.missingDependencies = missingDependencies;
        }
    }

    static Report load(Path target, PrintWriter log) throws Exception {
        TargetArchive archive = TargetArchive.inspect(target);
        log.println("Target: " + archive.path);
        log.println("Validated class files: " + archive.classes.size());

        URL url = archive.path.toUri().toURL();
        ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
        Set<String> missing = new HashSet<String>();
        int loaded = 0;
        int failed = 0;

        try (IsolatedClassLoader classLoader = new IsolatedClassLoader(url, parent)) {
            for (ClassFileInfo info : archive.classes) {
                try {
                    Class.forName(info.binaryName, false, classLoader);
                    log.println("CLASS LOADED: " + info.binaryName);
                    loaded++;
                } catch (ClassNotFoundException | LinkageError error) {
                    log.println("CLASS FAILED: " + info.binaryName + " :: " + describe(error));
                    error.printStackTrace(log);
                    failed++;
                    String dependency = missingName(error);
                    if (dependency != null && missing.add(dependency)) {
                        log.println("MISSING DEPENDENCY: " + dependency + " referenced by " + info.binaryName);
                    }
                }

                for (String dependency : info.referencedClasses) {
                    if (dependency.startsWith("java.") || dependency.startsWith("javax.") ||
                            dependency.startsWith("sun.") || dependency.startsWith("com.sun.")) {
                        continue;
                    }
                    try {
                        Class.forName(dependency, false, classLoader);
                    } catch (ClassNotFoundException | LinkageError error) {
                        if (missing.add(dependency)) {
                            log.println("MISSING DEPENDENCY: " + dependency + " referenced by " + info.binaryName);
                            error.printStackTrace(log);
                        }
                    }
                }
            }
        }

        log.println("SUMMARY: loaded=" + loaded + ", failed=" + failed +
                ", missingDependencies=" + missing.size());
        log.flush();
        return new Report(loaded, failed, missing);
    }

    private static String missingName(Throwable error) {
        if (error instanceof NoClassDefFoundError) {
            String value = error.getMessage();
            return value == null ? "unknown" : value.replace('/', '.');
        }
        return null;
    }

    private static String describe(Throwable error) {
        String message = error.getMessage();
        return error.getClass().getSimpleName() + (message == null ? "" : ": " + message);
    }
}
