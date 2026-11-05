# Star Class Loader — isolated Java 8 test runner

This tool inspects and loads classes from one selected JAR or classes directory in a new Java 8 process. Its custom `URLClassLoader` is never attached to Lunar Client, Minecraft, or another running application. Classes are loaded with initialization disabled; static initializers and `main` methods are not invoked.

The Windows launcher is a self-contained .NET 8 WinForms executable. It validates that the selected `java.exe` reports Java 8, provides JAR and directory pickers, streams diagnostics to the window, and writes a detailed log under `%LOCALAPPDATA%\StarClassLoader\logs`.

## Build on Windows

Requirements:

- JDK 8 (`javac.exe`, `jar.exe`); set `JAVA8_HOME` if it is not installed in a standard location.
- .NET SDK 9.0.200 with the .NET 8 Windows Desktop targeting pack (this combination was used for the verified build; .NET 8 SDK also supports the project target).
- PowerShell 7 or Windows PowerShell 5.1.

From the repository root:

```powershell
$env:JAVA8_HOME = 'C:\Program Files\Java\jdk1.8.0_202'
powershell -ExecutionPolicy Bypass -File .\tools\classloader\build.ps1
```

The script compiles main and test sources with Java 8 bytecode, runs the class-loader tests, creates the Java runner JAR, publishes the self-contained Windows executable, and checks both package files. It writes to a new timestamped directory under `target\classloader-package-*` and does not overwrite an existing package.

## Run

Launch `StarClassLoader.exe`, choose a Java 8 `java.exe`, then select a JAR or the classpath root directory containing `.class` files. The Java runner validates class-file versions (up to major 52), archive paths, class names, duplicates, and size limits before loading. It reports loaded classes, failed classes, and missing dependencies in the UI and log file.

CLI form for the Java runner:

```powershell
& "$env:JAVA8_HOME\bin\java.exe" -jar .\StarClassLoader.jar --target 'C:\path\to\authorized.jar' --log '.\classloader.log'
```

## Relationship to Nebula Client

This is an independent archive-inspection tool. Its Java sources and .NET launcher are not dependencies of the Nebula JAR or DLL and are not included in their build.

The client sources live under `src/main/java/dev/nebula/client` and use a version-specific mapping profile and JNI integration. The client no longer uses a mod-loader entry point. This tool does not run that integration or validate its behavior inside Lunar.

The `StarClassLoader` executable, JAR name and persisted log directory are existing contracts of this separate tool. Their migration must update the launcher, Java manifest, packaging script and documentation together.

## Diagnostics

- UI output includes the Java version, selected target, final counts, and log path.
- Detailed logs include every class load/failure and missing non-JDK dependencies.
- Java 8 class files have major version 52 or lower. Newer class files are rejected before loading.
- `ClassNotFoundException` and linkage failures identify unresolved dependencies. The loader does not execute application entry points to discover runtime-only dependencies.
