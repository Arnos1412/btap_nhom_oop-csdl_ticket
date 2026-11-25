# Cau hinh SQL Server de ung dung Java ket noi qua JDBC. Chay bang PowerShell "Run as Administrator".
#  1. Bat giao thuc TCP/IP, co dinh cong 1433, khoi dong lai dich vu SQL Server (MSSQLSERVER).
#  2. Tao login SQL 'datve_app' (mat khau ngau nhien) co quyen tren database DatVeXemPhim.
#  3. Ghi src/main/resources/db.properties voi app.mode=jdbc.
# Can chay truoc cac script 01_tao_bang.sql, 02_du_lieu_mau.sql, 03_procedure_trigger.sql.
$ErrorActionPreference = 'Stop'

$instanceId = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL').MSSQLSERVER
$tcp = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$instanceId\MSSQLServer\SuperSocketNetLib\Tcp"
Set-ItemProperty $tcp -Name Enabled -Value 1
Set-ItemProperty "$tcp\IPAll" -Name TcpPort -Value '1433'
Set-ItemProperty "$tcp\IPAll" -Name TcpDynamicPorts -Value ''
Write-Host 'Da bat TCP/IP cong 1433. Dang khoi dong lai SQL Server...'
Restart-Service MSSQLSERVER -Force
Start-Sleep -Seconds 5

$chu = (48..57) + (65..90) + (97..122)
$pw = 'DatVe@' + (-join ($chu | Get-Random -Count 12 | ForEach-Object { [char]$_ }))
$sql = @"
IF SUSER_ID('datve_app') IS NULL
    CREATE LOGIN datve_app WITH PASSWORD = '$pw', CHECK_POLICY = OFF, DEFAULT_DATABASE = DatVeXemPhim;
ELSE
    ALTER LOGIN datve_app WITH PASSWORD = '$pw';
USE DatVeXemPhim;
IF USER_ID('datve_app') IS NULL CREATE USER datve_app FOR LOGIN datve_app;
ALTER ROLE db_owner ADD MEMBER datve_app;
"@
sqlcmd -S localhost -E -C -b -Q $sql
if ($LASTEXITCODE -ne 0) { throw 'Tao login datve_app that bai.' }

$props = Join-Path $PSScriptRoot '..\src\main\resources\db.properties'
@"
# Tao boi database/setup_ket_noi.ps1 - KHONG commit file nay (da co trong .gitignore)
app.mode=jdbc
db.url=jdbc:sqlserver://localhost:1433;databaseName=DatVeXemPhim;encrypt=true;trustServerCertificate=true
db.user=datve_app
db.password=$pw
"@ | Set-Content -Encoding ascii $props

sqlcmd -S "tcp:localhost,1433" -U datve_app -P $pw -C -b -Q "SELECT DB_NAME() AS KetNoiTCP_OK"
Write-Host "Xong. Da ghi $props"
