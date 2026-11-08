<#
.SYNOPSIS
    Crivo (AIR Lab) — CLI de verificacao, Windows.

.DESCRIPTION
    Roda o mesmo artefato da aplicacao, sem servidor web, para conferir:
      - a cadeia de hashes da trilha de auditoria (P4);
      - o hash de cada versao do pre-registro (P2);
      - a reproducao do sorteio a partir da semente revelada (P5).

    Sai com codigo 0 se tudo confere e 1 caso contrario.

.EXAMPLE
    .\scripts\airlab.ps1 verify
.EXAMPLE
    .\scripts\airlab.ps1 verify --study=STD-7Q4K2M9B
#>
[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $CliArgs = @('verify')
)

$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir
$jar = Join-Path $projectDir 'target\airlab.jar'

if (Test-Path '.env') {
    foreach ($line in Get-Content '.env') {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
            $name, $value = $trimmed.Split('=', 2)
            Set-Item -Path ("Env:" + $name.Trim()) -Value $value.Trim('"').Trim()
        }
    }
}

if (-not (Test-Path $jar)) {
    Write-Host '==> Construindo o JAR' -ForegroundColor Green
    & (Join-Path $projectDir 'mvnw.cmd') -B -q package -DskipTests
    if ($LASTEXITCODE -ne 0) { exit 1 }
}

$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { 'java' }
$profileName = if ($env:AIRLAB_PROFILE) { $env:AIRLAB_PROFILE } else { 'local' }

& $java '-jar' $jar @CliArgs `
    "--spring.profiles.active=$profileName" `
    '--spring.main.web-application-type=none' `
    '--spring.main.banner-mode=off' `
    '--logging.level.root=WARN' `
    '--logging.level.io.github.rogeriocisi.airlab=WARN'

exit $LASTEXITCODE
