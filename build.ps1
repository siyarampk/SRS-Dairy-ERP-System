# Builds and runs the Dairy ERP app (Windows). Logs everything to build.log.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
Start-Transcript -Path "$PSScriptRoot\build.log" -Force | Out-Null

try {
    Write-Host "=== Java version ==="
    cmd /c "java -version 2>&1" | Write-Host
    cmd /c "javac -version 2>&1" | Write-Host

    Write-Host "=== Cleaning out/ ==="
    if (Test-Path out) { Remove-Item -Recurse -Force out }
    New-Item -ItemType Directory -Force -Path out | Out-Null

    Write-Host "=== Collecting sources ==="
    $sources = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
    Write-Host ("Found {0} source files" -f $sources.Count)
    $sources | Set-Content -Path "$env:TEMP\sources.txt" -Encoding ASCII

    Write-Host "=== Compiling ==="
    & javac -encoding UTF-8 -cp "lib/*" -d out "@$env:TEMP\sources.txt"
    if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }
    Write-Host "Compile OK -> out/"

    Write-Host "=== Staging dist/lib ==="
    New-Item -ItemType Directory -Force -Path dist\lib | Out-Null
    Copy-Item -Path lib\*.jar -Destination dist\lib -Force

    Write-Host "=== Packaging dist/DairyERP.jar ==="
    Set-Content -Path dist\manifest.txt -Value "Main-Class: dairy.erp.Main`r`nClass-Path: lib/sqlite-jdbc-3.45.3.0.jar lib/slf4j-api-1.7.36.jar lib/slf4j-nop-1.7.36.jar`r`n" -Encoding ASCII
    & jar cfm dist\DairyERP.jar dist\manifest.txt -C out .
    if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }
    Remove-Item dist\manifest.txt -Force
    Get-Item dist\DairyERP.jar | Select-Object FullName, Length, LastWriteTime | Format-List | Write-Host

    Write-Host "BUILD SUCCESS"
}
catch {
    Write-Host "BUILD FAILED: $_"
    Stop-Transcript | Out-Null
    exit 1
}
Stop-Transcript | Out-Null
