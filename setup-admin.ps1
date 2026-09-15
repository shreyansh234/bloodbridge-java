$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$settings = @{}
if (Test-Path .env) { Get-Content .env | ForEach-Object { if ($_ -match '^([^#=]+)=(.*)$') { $settings[$Matches[1]] = $Matches[2] } } }
$adminId = Read-Host "Admin ID [adminysss]"
if ([string]::IsNullOrWhiteSpace($adminId)) { $adminId = "adminysss" }
$securePassword = Read-Host "Choose admin password" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try { $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) } finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
if ([string]::IsNullOrEmpty($plainPassword)) { throw "Password cannot be empty" }
$salt = New-Object byte[] 16
$rng = [Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($salt)
$derive = [Security.Cryptography.Rfc2898DeriveBytes]::new($plainPassword, $salt, 600000, [Security.Cryptography.HashAlgorithmName]::SHA256)
$hash = [Convert]::ToBase64String($derive.GetBytes(32))
$derive.Dispose()
$plainPassword = $null
$settings['ADMIN_USERNAME'] = $adminId
$settings['ADMIN_PASSWORD_HASH'] = 'pbkdf2:600000:' + [Convert]::ToBase64String($salt) + ':' + $hash
foreach ($name in @('DB_PASSWORD','DB_ROOT_PASSWORD')) { if (!$settings.ContainsKey($name)) { $bytes=New-Object byte[] 24; $rng.GetBytes($bytes); $settings[$name]=[Convert]::ToBase64String($bytes) } }
$defaults = @{COOKIE_SECURE='false'; SITE_ORIGIN='http://localhost:8080'; FAMILY_ASSISTANCE_ENABLED='true'; RESEND_API_KEY=''; MAIL_FROM=''}
foreach ($key in $defaults.Keys) { if (!$settings.ContainsKey($key)) { $settings[$key]=$defaults[$key] } }
$lines = $settings.Keys | Sort-Object | ForEach-Object { $_ + '=' + $settings[$_] }
[IO.File]::WriteAllLines((Join-Path $PSScriptRoot '.env'), [string[]]$lines, (New-Object Text.UTF8Encoding($false)))
Write-Host "Admin configured. Only a salted password hash is saved."
