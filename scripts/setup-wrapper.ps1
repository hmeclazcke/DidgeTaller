# Downloads only the official Gradle 8.11.1 wrapper JAR and verifies SHA-256.
# The wrapper downloads the Gradle distribution separately.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
$expected = '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
$uri = 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar'
if (Test-Path $jar) {
    $actual = (Get-FileHash -Path $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -eq $expected) { Write-Host 'Wrapper verified.'; exit 0 }
    throw "The existing wrapper differs from the official SHA-256. Investigate before removing it: $jar"
}
Write-Host 'Downloading the official Gradle Wrapper (about 46 KB)...'
try {
    Invoke-WebRequest -Uri $uri -OutFile $jar -UseBasicParsing -TimeoutSec 60
    $actual = (Get-FileHash -Path $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        Remove-Item $jar -Force
        throw "Invalid SHA-256. Expected: $expected; received: $actual"
    }
    Write-Host 'Gradle Wrapper downloaded and verified.'
} catch {
    if (Test-Path $jar) { Remove-Item $jar -Force }
    throw
}
