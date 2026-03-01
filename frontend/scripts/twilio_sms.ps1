param(
    [string]$TargetPhone = "+916351753750",
    [string]$Body = "URGENT: Your bank account is locked. Click here: http://phish.scam"
)

# Load .env file
$envFile = Join-Path $PSScriptRoot "..\..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '=' -and $_ -notmatch '^#' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        Set-Item -Path "env:\$name" -Value $value.Trim()
    }
} else {
    Write-Warning ".env file not found at $envFile"
}

# Pull credentials passively out of global scope avoiding code-leakage.
$TwilioSid = $env:TWILIO_ACCOUNT_SID
$TwilioToken = $env:TWILIO_ACCOUNT_AUTH
$TwilioFrom = $env:TWILIO_PHONE_NUMBER

if ([string]::IsNullOrEmpty($TwilioSid) -or [string]::IsNullOrEmpty($TwilioToken) -or [string]::IsNullOrEmpty($TwilioFrom)) {
    Write-Error "Twilio credentials missing from .env file."
    exit 1
}

$pair = "$($TwilioSid):$($TwilioToken)"
$encoded = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($pair))

Invoke-RestMethod -Uri "https://api.twilio.com/2010-04-01/Accounts/$TwilioSid/Messages.json" `
    -Method Post `
    -Headers @{ Authorization = "Basic $encoded" } `
    -Body @{
        To = $TargetPhone
        From = $TwilioFrom
        Body = $Body
    }
Write-Host "✅ Distributed real-world SMS execution via Twilio Carrier payload." -ForegroundColor Green
