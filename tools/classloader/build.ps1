$ErrorActionPreference = 'Stop'

$toolRoot = $PSScriptRoot
$repoRoot = (Resolve-Path (Join-Path $toolRoot '..\..')).Path
$javaHome = $env:JAVA8_HOME
if ([string]::IsNullOrWhiteSpace($javaHome)) {
    $javaHome = $env:JAVA_HOME
}
$javac = Join-Path $javaHome 'bin\javac.exe'
$jarTool = Join-Path $javaHome 'bin\jar.exe'
if (!(Test-Path $javac) -or !(Test-Path $jarTool)) {
    throw "JDK 8 tools not found under '$javaHome'. Set JAVA8_HOME to a JDK 8 directory."
}

$stamp = Get-Date -Format 'yyyyMMdd-HHmmss-fff'
$packageRoot = Join-Path $repoRoot ("build\classloader-package-" + $stamp + '-' + $PID)
$buildRoot = Join-Path $packageRoot 'build'
$mainClasses = Join-Path $buildRoot 'classes'
$testClasses = Join-Path $buildRoot 'test-classes'
$distRoot = Join-Path $packageRoot 'dist'
$publishRoot = Join-Path $distRoot 'windows-x64'
New-Item -ItemType Directory -Force -Path $mainClasses, $testClasses, $publishRoot | Out-Null

$mainSources = Get-ChildItem (Join-Path $toolRoot 'java\src\main\java') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& $javac -source 1.8 -target 1.8 -encoding UTF-8 -d $mainClasses $mainSources
if ($LASTEXITCODE -ne 0) { throw "Java 8 compilation failed with exit code $LASTEXITCODE" }

$testSources = Get-ChildItem (Join-Path $toolRoot 'java\src\test\java') -Recurse -Filter '*.java' | Select-Object -ExpandProperty FullName
& $javac -source 1.8 -target 1.8 -encoding UTF-8 -cp $mainClasses -d $testClasses $testSources
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed with exit code $LASTEXITCODE" }

$java = Join-Path $javaHome 'bin\java.exe'
$testClasspath = $mainClasses + ';' + $testClasses
& $java -cp $testClasspath local.classloader.LoaderTests
if ($LASTEXITCODE -ne 0) { throw "Class-loader tests failed with exit code $LASTEXITCODE" }

$loaderJar = Join-Path $distRoot 'StarClassLoader.jar'
& $jarTool cfm $loaderJar (Join-Path $toolRoot 'java\MANIFEST.MF') -C $mainClasses .
if ($LASTEXITCODE -ne 0 -or !(Test-Path $loaderJar)) { throw 'Could not create StarClassLoader.jar' }

$project = Join-Path $toolRoot 'windows\StarClassLoaderLauncher.csproj'
& dotnet publish $project -c Release -r win-x64 --self-contained true -p:PublishSingleFile=true -p:IncludeNativeLibrariesForSelfExtract=true -o $publishRoot
if ($LASTEXITCODE -ne 0) { throw "Windows launcher publish failed with exit code $LASTEXITCODE" }

Copy-Item -LiteralPath $loaderJar -Destination (Join-Path $publishRoot 'StarClassLoader.jar')
Copy-Item -LiteralPath (Join-Path $toolRoot 'README.md') -Destination (Join-Path $publishRoot 'README.txt')
$exe = Join-Path $publishRoot 'StarClassLoader.exe'
if (!(Test-Path $exe) -or (Get-Item $exe).Length -eq 0) { throw 'Published Windows executable is missing or empty' }
if (!(Test-Path (Join-Path $publishRoot 'StarClassLoader.jar'))) { throw 'Published Java runner JAR is missing' }

Write-Output "PACKAGE: $publishRoot"
Write-Output "EXE: $exe"
Write-Output "JAR: $(Join-Path $publishRoot 'StarClassLoader.jar')"
