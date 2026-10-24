param([switch]$Ejecutar)

$ErrorActionPreference = 'Stop'
$projectPath = Join-Path $PSScriptRoot 'tinyuml-0.13_02'

try {
    # Reutiliza los JDK instalados por IntelliJ si Java no esta en el PATH.
    $jdkCandidates = @($env:JAVA_HOME)
    $javacCommand = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($javacCommand) {
        $jdkCandidates += Split-Path (Split-Path $javacCommand.Source)
    }
    $jdkCandidates += Get-ChildItem (Join-Path $env:USERPROFILE '.jdks') -Directory -ErrorAction SilentlyContinue |
        Sort-Object @{Expression = { $_.Name -match '17' }; Descending = $true}, Name |
        Select-Object -ExpandProperty FullName
    $jdkPath = $jdkCandidates | Where-Object {
        $_ -and (Test-Path (Join-Path $_ 'bin\javac.exe'))
    } | Select-Object -First 1
    if (-not $jdkPath) { throw 'No se encontro un JDK. Instala un JDK y configura JAVA_HOME.' }
    $env:JAVA_HOME = $jdkPath

    $mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    $mavenPath = if ($mavenCommand) { $mavenCommand.Source } else { $null }
    if (-not $mavenPath) {
        $mavenPath = Get-ChildItem (Join-Path $env:USERPROFILE '.m2\wrapper\dists') -Filter mvn.cmd -Recurse -File -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $mavenPath) {
        $mavenPath = Get-ChildItem (Join-Path $env:ProgramFiles 'JetBrains\IntelliJ IDEA*\plugins\maven\lib\maven3\bin\mvn.cmd') -File -ErrorAction SilentlyContinue |
            Select-Object -First 1 -ExpandProperty FullName
    }
    if (-not $mavenPath) { throw 'No se encontro Maven. Instala Maven y agrega su carpeta bin al PATH.' }

    Write-Host "Java: $jdkPath"
    Write-Host "Maven: $mavenPath"
    $env:MAVEN_PROJECTBASEDIR = $projectPath
    Push-Location $projectPath
    try {
        & $mavenPath -B clean package
        if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
        if ($Ejecutar) {
            & (Join-Path $jdkPath 'bin\java.exe') -jar (Join-Path $projectPath 'target\tinyuml-0.13_02.jar')
            exit $LASTEXITCODE
        }
    } finally {
        Pop-Location
    }
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}
