package local.classloader;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class LoaderMain {
    private LoaderMain() {}

    public static void main(String[] args) {
        Path target = null;
        Path logPath = null;
        try {
            for (int i = 0; i < args.length; i++) {
                if ("--target".equals(args[i]) && i + 1 < args.length) {
                    target = Paths.get(args[++i]);
                } else if ("--log".equals(args[i]) && i + 1 < args.length) {
                    logPath = Paths.get(args[++i]);
                } else {
                    throw new IllegalArgumentException("usage: --target <jar-or-classes-directory> [--log <file>]");
                }
            }
            if (target == null) {
                throw new IllegalArgumentException("--target is required");
            }
            if (logPath == null) {
                File logDirectory = new File(System.getProperty("user.home"), ".star-classloader/logs");
                logPath = new File(logDirectory, "classloader-" +
                        new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".log").toPath();
            }

            File logFile = logPath.toAbsolutePath().normalize().toFile();
            File parent = logFile.getParentFile();
            if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                throw new IllegalStateException("could not create log directory: " + parent);
            }
            try (PrintWriter log = new PrintWriter(new FileWriter(logFile, true))) {
                log.println("--- ClassLoader run " + new Date() + " ---");
                try {
                    LoaderEngine.Report report = LoaderEngine.load(target, log);
                    System.out.println("Loaded " + report.loaded + " classes; failed " + report.failed +
                            "; missing dependencies " + report.missingDependencies.size());
                    System.out.println("Log: " + logFile.getAbsolutePath());
                    if (report.failed != 0 || !report.missingDependencies.isEmpty()) {
                        System.exit(2);
                    }
                } catch (Exception error) {
                    log.println("FATAL: " + error.getClass().getSimpleName() + ": " + error.getMessage());
                    error.printStackTrace(log);
                    log.flush();
                    System.err.println("Load failed: " + error.getMessage());
                    System.err.println("Log: " + logFile.getAbsolutePath());
                    System.exit(1);
                }
            }
        } catch (Exception error) {
            System.err.println(error.getMessage());
            System.exit(1);
        }
    }
}
