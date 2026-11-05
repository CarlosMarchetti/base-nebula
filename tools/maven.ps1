param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArguments)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$lock = Get-Content -LiteralPath (Join-Path $projectRoot '.mvn/wrapper.json') -Raw | ConvertFrom-Json
$cache = Join-Path $projectRoot '.cache/maven'
$binary = Join-Path $cache ("apache-maven-" + $lock.version + '/bin/mvn.cmd')
if (!(Test-Path -LiteralPath $binary)) {
    New-Item -ItemType Directory -Force -Path $cache | Out-Null
    $archive = Join-Path $cache ('apache-maven-' + $lock.version + '.zip')
    if (!(Test-Path -LiteralPath $archive)) {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        $client = New-Object Net.WebClient
        try { $client.DownloadFile($lock.url, $archive) } finally { $client.Dispose() }
    }
    $hasher = [Security.Cryptography.SHA512]::Create()
    try { $digest = [BitConverter]::ToString($hasher.ComputeHash([IO.File]::ReadAllBytes($archive))).Replace('-', '').ToLowerInvariant() } finally { $hasher.Dispose() }
    if ($digest -ne $lock.sha512) { throw 'Maven archive checksum mismatch' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [IO.Compression.ZipFile]::ExtractToDirectory($archive, $cache)
}
$revision=(Get-Content -LiteralPath (Join-Path $projectRoot 'VERSION') -Raw).Trim()
& $binary "-Drevision=$revision" @MavenArguments
exit $LASTEXITCODE
