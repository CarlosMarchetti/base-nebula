param([Parameter(Mandatory=$true)][string]$DependencyDirectory)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$lock=Get-Content -LiteralPath (Join-Path $root 'dependencies.lock.json') -Raw | ConvertFrom-Json
foreach ($artifact in $lock.artifacts) {
    $file=Join-Path $DependencyDirectory $artifact.file
    if (!(Test-Path -LiteralPath $file)) { throw "Missing dependency: $($artifact.file)" }
    $hasher=[Security.Cryptography.SHA256]::Create()
    try { $digest=[BitConverter]::ToString($hasher.ComputeHash([IO.File]::ReadAllBytes($file))).Replace('-','').ToLowerInvariant() } finally { $hasher.Dispose() }
    if ($digest -ne $artifact.sha256) { throw "Dependency checksum mismatch: $($artifact.file)" }
}
Write-Output 'PASS dependency lock: all declared artifacts verified'
