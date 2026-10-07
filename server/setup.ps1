# Installs everything the server needs. start.bat runs this for you.
# It only downloads things that are missing, so later starts are quick.
param([string]$McVersion = '26.3')

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # makes downloads much faster
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Set-Location $PSScriptRoot
$ua = 'arzmods-server-setup/1.0'

# --- 1. Java 25 (a private copy inside this folder, nothing installed on Windows) ---
if (-not (Test-Path 'java\bin\java.exe')) {
    Write-Host 'Downloading Java 25 (one time only, about 50 MB)...'
    $zip = 'java-download.zip'
    Invoke-WebRequest 'https://api.adoptium.net/v3/binary/latest/25/ga/windows/x64/jre/hotspot/normal/eclipse' -OutFile $zip -UserAgent $ua
    if (Test-Path 'java-unzip') { Remove-Item 'java-unzip' -Recurse -Force }
    Expand-Archive $zip 'java-unzip'
    $inner = Get-ChildItem 'java-unzip' -Directory | Select-Object -First 1
    Move-Item $inner.FullName 'java'
    Remove-Item 'java-unzip' -Recurse -Force
    Remove-Item $zip -Force
}

# --- 2. Fabric server launcher for this Minecraft version ---
$marker = 'fabric-version.txt'
$installed = if (Test-Path $marker) { (Get-Content $marker -Raw).Trim() } else { '' }
if (-not (Test-Path 'fabric-server-launch.jar') -or $installed -ne $McVersion) {
    Write-Host "Downloading the Fabric server for Minecraft $McVersion..."
    $loaders = Invoke-RestMethod "https://meta.fabricmc.net/v2/versions/loader/$McVersion" -UserAgent $ua
    if (-not $loaders) {
        throw "Fabric does not support Minecraft $McVersion yet. Try again later, or change MC_VERSION in start.bat."
    }
    $loader = ($loaders | Where-Object { $_.loader.stable } | Select-Object -First 1).loader.version
    if (-not $loader) { $loader = $loaders[0].loader.version }
    $installer = (Invoke-RestMethod 'https://meta.fabricmc.net/v2/versions/installer' -UserAgent $ua |
        Where-Object stable | Select-Object -First 1).version
    Invoke-WebRequest "https://meta.fabricmc.net/v2/versions/loader/$McVersion/$loader/$installer/server/jar" -OutFile 'fabric-server-launch.jar' -UserAgent $ua
    Set-Content $marker $McVersion
    # Old Fabric API would not work with the new version, so fetch a fresh one below
    Remove-Item 'mods\fabric-api-*.jar' -ErrorAction SilentlyContinue
}

# --- 3. Fabric API mod (almost every Fabric mod needs it) ---
New-Item 'mods' -ItemType Directory -Force | Out-Null
if (-not (Get-ChildItem 'mods\fabric-api-*.jar' -ErrorAction SilentlyContinue)) {
    Write-Host "Downloading Fabric API for Minecraft $McVersion..."
    $query = '?loaders=' + [uri]::EscapeDataString('["fabric"]') + '&game_versions=' + [uri]::EscapeDataString("[`"$McVersion`"]")
    $versions = Invoke-RestMethod ("https://api.modrinth.com/v2/project/fabric-api/version" + $query) -UserAgent $ua
    if ($versions) {
        $file = $versions[0].files | Where-Object primary | Select-Object -First 1
        if (-not $file) { $file = $versions[0].files[0] }
        Invoke-WebRequest $file.url -OutFile (Join-Path 'mods' $file.filename) -UserAgent $ua
    } else {
        Write-Host "Note: Fabric API for $McVersion is not out yet. The server will still start without it."
    }
}

Write-Host 'Everything is installed.'
