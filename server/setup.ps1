# Installs everything the server needs. start.bat runs this for you.
# It only downloads things that are missing, so later starts are quick.
param([string]$McVersion = '26.3')

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # makes downloads much faster
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Set-Location $PSScriptRoot
$ua = 'paper-server-setup/1.0'

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

# --- 2. Paper server for this Minecraft version ---
$marker = 'paper-version.txt'
$installed = if (Test-Path $marker) { (Get-Content $marker -Raw).Trim() } else { '' }
if (-not (Test-Path 'paper.jar') -or $installed -ne $McVersion) {
    Write-Host "Downloading the Paper server for Minecraft $McVersion..."
    try {
        $build = Invoke-RestMethod "https://fill.papermc.io/v3/projects/paper/versions/$McVersion/builds/latest" -UserAgent $ua
    } catch {
        throw "Paper does not have Minecraft $McVersion yet. Try again later, or change MC_VERSION in start.bat."
    }
    $url = $build.downloads.'server:default'.url
    Invoke-WebRequest $url -OutFile 'paper.jar' -UserAgent $ua
    Set-Content $marker $McVersion
}

New-Item 'plugins' -ItemType Directory -Force | Out-Null

Write-Host 'Everything is installed.'

# --- 3. Show the addresses people can use to join ---
$ips = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object { $_.IPAddress -notlike '127.*' -and $_.IPAddress -notlike '169.254.*' } |
    Select-Object -ExpandProperty IPAddress
Write-Host ''
Write-Host '=================== HOW TO JOIN ===================' -ForegroundColor Green
Write-Host '  On this PC:            localhost' -ForegroundColor Green
foreach ($ip in $ips) {
    Write-Host "  Friends on your Wi-Fi: $ip" -ForegroundColor Green
}
Write-Host '  Friends anywhere:      your playit.gg address' -ForegroundColor Green
Write-Host '                         (open the playit app to see it)' -ForegroundColor Green
Write-Host '===================================================' -ForegroundColor Green
