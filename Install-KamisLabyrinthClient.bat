@echo off
setlocal
set "KAMI_INSTALLER=%~f0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -Command "$p = $env:KAMI_INSTALLER; $s = Get-Content -LiteralPath $p -Raw; $m = '# POWERSHELL_PAYLOAD_START'; $i = $s.LastIndexOf($m); if($i -lt 0) { throw 'Installer payload not found.' }; $tmp = Join-Path $env:TEMP ('install-kamis-labyrinth-client-' + [guid]::NewGuid() + '.ps1'); Set-Content -LiteralPath $tmp -Value $s.Substring($i + $m.Length) -Encoding UTF8; & $tmp; $ec = if($global:LASTEXITCODE -is [int]) { $global:LASTEXITCODE } else { 0 }; Remove-Item -LiteralPath $tmp -Force -ErrorAction SilentlyContinue; exit $ec"
exit /b %ERRORLEVEL%

# POWERSHELL_PAYLOAD_START
$ErrorActionPreference = 'Stop'

$appName = "kami's labyrinth Client"
$desktopDir = [Environment]::GetFolderPath('DesktopDirectory')
$installDir = Join-Path $desktopDir 'KamisLabyrinthClient'
$releaseBase = 'https://github.com/cosgroup-glitch/CoSHHClient/releases/latest/download'
$manifestUrl = "$releaseBase/update.json"
$javaUrl = 'https://adoptium.net/temurin/releases/?version=21'

function Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Wait-To-Close {
    Write-Host ""
    Read-Host 'Press Enter to close'
}

function Resolve-Java {
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if($cmd -and $cmd.Source) {
        return $cmd.Source
    }

    $candidates = @()
    if($env:JAVA_HOME) {
        $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe')
    }

    $roots = @(
        (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
        (Join-Path $env:ProgramFiles 'Java'),
        (Join-Path $env:ProgramFiles 'Microsoft'),
        (Join-Path ${env:ProgramFiles(x86)} 'Java')
    )

    foreach($root in $roots) {
        if($root -and (Test-Path -LiteralPath $root)) {
            $candidates += Get-ChildItem -LiteralPath $root -Directory -ErrorAction SilentlyContinue |
                ForEach-Object { Join-Path $_.FullName 'bin\java.exe' }
        }
    }

    $candidates += @(
        (Join-Path $env:ProgramFiles 'Java\latest\bin\java.exe'),
        (Join-Path ${env:ProgramFiles(x86)} 'Java\latest\bin\java.exe')
    )

    foreach($candidate in $candidates) {
        if($candidate -and (Test-Path -LiteralPath $candidate)) {
            return $candidate
        }
    }

    return $null
}

function Resolve-DownloadUrl($base, $relativeOrAbsolute) {
    try {
        return ([Uri]::new($relativeOrAbsolute)).AbsoluteUri
    } catch {
        return ([Uri]::new([Uri]$base, $relativeOrAbsolute)).AbsoluteUri
    }
}

function Get-Sha256($path) {
    return (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()
}

function Create-Shortcut($path, $target, $workingDirectory, $iconPath) {
    $parent = Split-Path -Parent $path
    if(!(Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $shell = New-Object -ComObject WScript.Shell
    $shortcut = $shell.CreateShortcut($path)
    $shortcut.TargetPath = $target
    $shortcut.WorkingDirectory = $workingDirectory
    $shortcut.Description = $appName
    if($iconPath -and (Test-Path -LiteralPath $iconPath)) {
        $shortcut.IconLocation = $iconPath
    }
    $shortcut.Save()
}

try {
    Write-Host "$appName installer" -ForegroundColor Green

    $javaPath = Resolve-Java
    if(!$javaPath) {
        Write-Host ""
        Write-Host 'Java was not found. Install Java 21 or newer, then run this installer again.' -ForegroundColor Yellow
        Write-Host "Opening Java download page: $javaUrl"
        Start-Process $javaUrl
        Wait-To-Close
        exit 1
    }
    Write-Host "Using Java: $javaPath" -ForegroundColor DarkGray

    $tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ('kami-install-' + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    $manifestPath = Join-Path $tempRoot 'update.json'
    $zipPath = Join-Path $tempRoot 'KamisLabyrinthClient.zip'
    $stageDir = Join-Path $tempRoot 'stage'

    Step 'Downloading latest release manifest'
    Invoke-WebRequest -UseBasicParsing -Uri $manifestUrl -OutFile $manifestPath
    $manifest = Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
    if(!$manifest.version -or !$manifest.url) {
        throw 'Release manifest is missing version or url.'
    }

    $zipUrl = Resolve-DownloadUrl $manifestUrl $manifest.url
    Step "Downloading $appName $($manifest.version)"
    Invoke-WebRequest -UseBasicParsing -Uri $zipUrl -OutFile $zipPath

    if($manifest.sha256) {
        Step 'Verifying download'
        $actual = Get-Sha256 $zipPath
        if($actual -ne $manifest.sha256.ToLowerInvariant()) {
            throw "Checksum failed. Expected $($manifest.sha256), got $actual."
        }
    }

    Step "Installing to $installDir"
    if(!(Test-Path -LiteralPath $installDir)) {
        New-Item -ItemType Directory -Path $installDir -Force | Out-Null
    }
    New-Item -ItemType Directory -Path $stageDir -Force | Out-Null
    Expand-Archive -Path $zipPath -DestinationPath $stageDir -Force

    $sourceDir = $stageDir
    $childDirs = @(Get-ChildItem -LiteralPath $stageDir -Directory)
    if(($childDirs.Count -eq 1) -and (Test-Path -LiteralPath (Join-Path $childDirs[0].FullName 'bin'))) {
        $sourceDir = $childDirs[0].FullName
    }

    Copy-Item -Path (Join-Path $sourceDir '*') -Destination $installDir -Recurse -Force

    $launcher = Join-Path $installDir 'KamisLabyrinthClient.bat'
    if(!(Test-Path -LiteralPath $launcher)) {
        $launcher = Join-Path $installDir 'run-kami-bin.bat'
    }
    if(!(Test-Path -LiteralPath $launcher)) {
        throw 'Installed launcher was not found.'
    }

    Step 'Creating shortcuts'
    $iconPath = Join-Path $installDir 'KamisLabyrinthClient.ico'
    $desktopShortcut = Join-Path $desktopDir "$appName.lnk"
    $startMenuDir = Join-Path ([Environment]::GetFolderPath('Programs')) $appName
    $startMenuShortcut = Join-Path $startMenuDir "$appName.lnk"
    Create-Shortcut $desktopShortcut $launcher $installDir $iconPath
    Create-Shortcut $startMenuShortcut $launcher $installDir $iconPath

    Step 'Starting client'
    Start-Process -FilePath $launcher -WorkingDirectory $installDir

    Write-Host ""
    Write-Host "$appName $($manifest.version) installed." -ForegroundColor Green
    Write-Host "Installed at: $installDir"
    Write-Host 'Future client updates will be downloaded automatically on startup.'
} catch {
    Write-Host ""
    Write-Host "Install failed: $($_.Exception.Message)" -ForegroundColor Red
    Wait-To-Close
    exit 1
} finally {
    if($tempRoot -and (Test-Path -LiteralPath $tempRoot)) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
    }
}
