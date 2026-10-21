# Descarga únicamente el JAR oficial del wrapper Gradle 8.11.1 y verifica SHA-256.
# La distribución Gradle se descargará después por el propio wrapper.
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'gradle\wrapper\gradle-wrapper.jar'
$expected = '2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046'
$uri = 'https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar'
if (Test-Path $jar) {
    $actual = (Get-FileHash -Path $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -eq $expected) { Write-Host 'Wrapper verificado.'; exit 0 }
    throw "El wrapper existente no coincide con el SHA-256 oficial. Eliminá el archivo solamente después de investigarlo: $jar"
}
Write-Host 'Descargando Gradle Wrapper oficial (aprox. 46 KB)...'
try {
    Invoke-WebRequest -Uri $uri -OutFile $jar -UseBasicParsing -TimeoutSec 60
    $actual = (Get-FileHash -Path $jar -Algorithm SHA256).Hash.ToLowerInvariant()
    if ($actual -ne $expected) {
        Remove-Item $jar -Force
        throw "SHA-256 inválido. Esperado: $expected; recibido: $actual"
    }
    Write-Host 'Gradle Wrapper descargado y verificado.'
} catch {
    if (Test-Path $jar) { Remove-Item $jar -Force }
    throw
}
