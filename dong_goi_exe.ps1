# Dong goi ung dung thanh file exe (kem san Java, may khac khong can cai JDK).
# Chay tai thu muc goc du an:   powershell -ExecutionPolicy Bypass -File dong_goi_exe.ps1
# Ket qua: dist\DatVeXemPhim\DatVeXemPhim.exe  (gui ca THU MUC dist\DatVeXemPhim, khong gui rieng file exe)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

# Tim jpackage (co san trong JDK 14+)
$jpackage = $null
if ($env:JAVA_HOME) { $jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe' }
if (-not $jpackage -or -not (Test-Path $jpackage)) {
    $jpackage = (Get-Command jpackage -ErrorAction SilentlyContinue).Source
}
if (-not $jpackage) { throw 'Khong tim thay jpackage. Dat JAVA_HOME tro toi JDK 17+.' }

Write-Host '1/4 Build jar (mvn package)...'
& mvn -q package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'mvn package that bai.' }

Write-Host '2/4 Chuan bi thu muc dong goi...'
$thuMucVao = Join-Path $PSScriptRoot 'target\exe-input'
if (Test-Path $thuMucVao) { Remove-Item -Recurse -Force $thuMucVao }
New-Item -ItemType Directory -Force -Path "$thuMucVao\database" | Out-Null
$jar = Join-Path $thuMucVao 'DatVeXemPhim.jar'
Copy-Item 'target\datvexemphim-1.0-SNAPSHOT.jar' $jar

# Bo db.properties (chua mat khau cua may nay) ra khoi jar: ban exe tu cau hinh rieng tren tung may.
Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($jar, 'Update')
try {
    $entry = $zip.GetEntry('db.properties')
    if ($entry) { $entry.Delete() }
} finally { $zip.Dispose() }

Copy-Item 'database\01_tao_bang.sql', 'database\02_du_lieu_mau.sql', 'database\03_procedure_trigger.sql', `
          'database\tu_cau_hinh.ps1' "$thuMucVao\database"

Write-Host '3/4 Tao exe bang jpackage (mat 1-2 phut)...'
$dist = Join-Path $PSScriptRoot 'dist'
if (Test-Path "$dist\DatVeXemPhim") { Remove-Item -Recurse -Force "$dist\DatVeXemPhim" }
& $jpackage --type app-image `
    --name DatVeXemPhim `
    --app-version 1.0 `
    --input $thuMucVao `
    --main-jar DatVeXemPhim.jar `
    --main-class com.nhom.datve.Main `
    --java-options '-Ddatve.exe=true' `
    --java-options '-Ddatve.appdir=$APPDIR' `
    --dest $dist
if ($LASTEXITCODE -ne 0) { throw 'jpackage that bai.' }

Write-Host "4/4 XONG: $dist\DatVeXemPhim\DatVeXemPhim.exe"
