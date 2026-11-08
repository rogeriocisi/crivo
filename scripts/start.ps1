<#
.SYNOPSIS
    Crivo (AIR Lab) — script unico de inicializacao, Windows.

.DESCRIPTION
    Sobe todos os componentes necessarios:
      - PostgreSQL: no perfil local, embarcado pela propria aplicacao (binarios reais do
        PostgreSQL, sem Docker); no perfil prod, o PostgreSQL do sistema operacional;
      - migracoes Flyway dos esquemas research, identity e audit;
      - a aplicacao Spring Boot, com servidor web embutido.

    Pre-requisito unico: um JDK 21 ou mais novo. O Maven vem pelo wrapper do repositorio.

    Ctrl+C encerra a aplicacao e, com ela, o PostgreSQL embarcado.

.EXAMPLE
    .\scripts\start.ps1
.EXAMPLE
    .\scripts\start.ps1 -AppProfile prod
.EXAMPLE
    .\scripts\start.ps1 -Port 9090 -Rebuild
#>
[CmdletBinding()]
param(
    [ValidateSet('local', 'prod')]
    [string] $AppProfile = 'local',

    [int] $Port = 0,

    [switch] $SkipBuild,

    [switch] $Rebuild,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $ExtraArgs
)

$ErrorActionPreference = 'Stop'

$projectDir = Split-Path -Parent $PSScriptRoot
Set-Location $projectDir
$jar = Join-Path $projectDir 'target\airlab.jar'

function Write-Step([string] $message) { Write-Host "==> $message" -ForegroundColor Green }
function Write-Note([string] $message) { Write-Host "==> $message" -ForegroundColor Yellow }
function Stop-WithError([string] $message) { Write-Host "erro: $message" -ForegroundColor Red; exit 1 }

# 1. JDK ----------------------------------------------------------------------
$java = $null
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
}
else {
    $command = Get-Command java -ErrorAction SilentlyContinue
    if ($command) { $java = $command.Source }
}
if (-not $java) {
    Stop-WithError 'nenhum JDK encontrado. Instale um JDK 21 (ou mais novo) ou defina JAVA_HOME.'
}

$versionLine = (& $java -version 2>&1 | Select-Object -First 1)
$major = 0
if ($versionLine -match 'version "(\d+)') { $major = [int]$Matches[1] }
if ($major -lt 21) {
    Stop-WithError "e necessario um JDK 21 ou mais novo (encontrado: $versionLine)."
}
Write-Step "JDK $major em $java"

# 2. Segredos e pre-condicoes do perfil ---------------------------------------
if (Test-Path '.env') {
    Write-Step 'Lendo variaveis de .env'
    foreach ($line in Get-Content '.env') {
        $trimmed = $line.Trim()
        if ($trimmed -and -not $trimmed.StartsWith('#') -and $trimmed.Contains('=')) {
            $name, $value = $trimmed.Split('=', 2)
            Set-Item -Path ("Env:" + $name.Trim()) -Value $value.Trim('"').Trim()
        }
    }
}

if ($AppProfile -eq 'prod') {
    foreach ($required in @('AIRLAB_DB_URL', 'AIRLAB_DB_USER', 'AIRLAB_DB_PASSWORD',
                            'AIRLAB_IDENTITY_KEY', 'AIRLAB_RESEARCHER_PASSWORD_HASH')) {
        if (-not (Get-Item -Path ("Env:" + $required) -ErrorAction SilentlyContinue)) {
            Stop-WithError "$required nao definida. No perfil prod os segredos vem do ambiente ou do .env; a aplicacao nao inicia sem eles."
        }
    }
    Write-Step 'Perfil prod: PostgreSQL do sistema operacional'
}
else {
    New-Item -ItemType Directory -Force -Path 'data' | Out-Null
    $dbDir = if ($env:AIRLAB_LOCAL_DB_DIR) { $env:AIRLAB_LOCAL_DB_DIR } else { Join-Path $HOME ".airlab\pgdata" }
    Write-Step "Perfil $AppProfile`: PostgreSQL embarcado em $dbDir (somente dados ficticios)"
}

# 3. Build --------------------------------------------------------------------
$mvnw = Join-Path $projectDir 'mvnw.cmd'

if ($Rebuild) {
    Write-Step 'Construindo o JAR com os testes'
    & $mvnw -B clean package
    if ($LASTEXITCODE -ne 0) { Stop-WithError 'o build falhou.' }
}
elseif ($SkipBuild) {
    if (-not (Test-Path $jar)) { Stop-WithError "$jar nao existe; rode sem -SkipBuild." }
    Write-Step 'Usando o JAR existente'
}
else {
    $needsBuild = $true
    if (Test-Path $jar) {
        $jarTime = (Get-Item $jar).LastWriteTimeUtc
        $newest = Get-ChildItem -Path 'src', 'pom.xml' -Recurse -File |
            Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
        if ($newest -and $newest.LastWriteTimeUtc -le $jarTime) { $needsBuild = $false }
    }
    if ($needsBuild) {
        Write-Step 'Construindo o JAR (sem testes; use -Rebuild para rodar a suite)'
        & $mvnw -B -q package -DskipTests
        if ($LASTEXITCODE -ne 0) { Stop-WithError 'o build falhou.' }
    }
    else {
        Write-Step 'JAR atualizado'
    }
}

# 4. Aplicacao ----------------------------------------------------------------
$appArgs = @("--spring.profiles.active=$AppProfile")
if ($Port -gt 0) { $appArgs += "--server.port=$Port" }
if ($ExtraArgs) { $appArgs += $ExtraArgs }

Write-Step "Iniciando o Crivo (perfil $AppProfile). Ctrl+C encerra tudo."
& $java '-jar' $jar @appArgs
exit $LASTEXITCODE
