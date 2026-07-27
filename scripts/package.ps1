[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$targetRoot = [System.IO.Path]::GetFullPath((Join-Path $projectRoot 'target'))
$stagingRoot = [System.IO.Path]::GetFullPath((Join-Path $targetRoot 'jpackage-input'))
$packageRoot = [System.IO.Path]::GetFullPath((Join-Path $targetRoot 'package'))

if (-not $stagingRoot.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to stage outside the project target directory.'
}
if (-not $packageRoot.StartsWith($targetRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
    throw 'Refusing to package outside the project target directory.'
}

Push-Location $projectRoot
try {
    & (Join-Path $projectRoot 'mvnw.cmd') -B -ntp clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Maven verification failed with exit code $LASTEXITCODE."
    }

    $jar = @(Get-ChildItem -LiteralPath $targetRoot -Filter 'jdoor-assist-*-all.jar')
    if ($jar.Count -ne 1) {
        throw "Expected exactly one shaded JAR, found $($jar.Count)."
    }
    $appVersion = $jar.BaseName -replace '^jdoor-assist-', '' -replace '-all$', ''
    if ($appVersion -notmatch '^\d+(?:\.\d+){0,2}$') {
        throw "Cannot derive a jpackage-compatible version from $($jar.Name)."
    }

    New-Item -ItemType Directory -Force -Path $stagingRoot | Out-Null
    New-Item -ItemType Directory -Force -Path $packageRoot | Out-Null
    Copy-Item -LiteralPath $jar.FullName -Destination $stagingRoot

    $jpackage = $null
    if ($env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (Test-Path -LiteralPath $candidate) {
            $jpackage = $candidate
        }
    }
    if (-not $jpackage) {
        $jpackage = (Get-Command jpackage -ErrorAction Stop).Source
    }

    & $jpackage `
        --type app-image `
        --name 'JDoor Assist' `
        --description 'Consent-first encrypted remote assistance' `
        --vendor 'Djenis Ejupi and contributors' `
        --app-version $appVersion `
        --dest $packageRoot `
        --input $stagingRoot `
        --main-jar $jar.Name `
        --main-class 'com.jdoor.JDoorApplication'
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage failed with exit code $LASTEXITCODE."
    }

    Write-Output "Created app image: $(Join-Path $packageRoot 'JDoor Assist')"
} finally {
    Pop-Location
}
