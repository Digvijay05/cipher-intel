$BaseUrl = "https://ai-honeypot-api-kkl5.onrender.com"

# Load .env file for API Key
$envFile = Join-Path $PSScriptRoot "..\..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | Where-Object { $_ -match '=' -and $_ -notmatch '^#' } | ForEach-Object {
        $name, $value = $_.Split('=', 2)
        Set-Item -Path "env:\$name" -Value $value.Trim()
    }
}
$ApiKey = $env:CIPHER_API_KEY

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host " CIPHER Backend Readiness Check " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

# 1. Check Feature Flags
Write-Host "`n[1/2] Checking /api/v1/feature-flags..." -ForegroundColor Yellow
try {
    $flags = Invoke-RestMethod -Uri "$BaseUrl/api/v1/feature-flags" -Method Get -TimeoutSec 60
    Write-Host "Response: $(($flags | ConvertTo-Json -Compress))"
    if ($flags.engagement_enabled -eq $true) {
        Write-Host "✅ Feature Flags: OK (Engagement Enabled)" -ForegroundColor Green
    } else {
        Write-Host "❌ Feature Flags: Engagement is DISABLED. Check backend configuration." -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Failed to reach feature-flags endpoint. Render might be sleeping or down." -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# 2. Check /analyze (Warmup & Logic Check)
Write-Host "`n[2/2] Warming up /api/v1/analyze (Wakes up ML models)..." -ForegroundColor Yellow
$analyzeBody = @{
    sessionId = "test-warmup-123"
    message = @{
        sender = "+15550001234"
        text = "URGENT: Your SBI bank account number is blocked. Important validation required. Send OTP to login credentials. Click here to verify: http://test.xyz/"
        timestamp = 1770005528731
    }
    conversationHistory = @()
} | ConvertTo-Json -Depth 5 -Compress

$headers = @{
    "X-API-Key" = $ApiKey
    "Content-Type" = "application/json"
}

try {
    $analyze = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analyze" -Method Post -Body $analyzeBody -Headers $headers -TimeoutSec 120
    Write-Host "Response: $(($analyze | ConvertTo-Json -Depth 5 -Compress))"
    
    if ($analyze.is_threat -eq $true -and $analyze.confidence_score -gt 0.8) {
        Write-Host "✅ Analyze Endpoint: OK (Threat Detected Correctly with confidence $($analyze.confidence_score))" -ForegroundColor Green
    } else {
        Write-Host "⚠️ Analyze Endpoint: Responded, but threat classification was unexpected." -ForegroundColor DarkYellow
    }
} catch {
    Write-Host "❌ Failed to reach analyze endpoint." -ForegroundColor Red
    if ($_.ErrorDetails) {
        Write-Host "Error Details: $($_.ErrorDetails.Message)" -ForegroundColor Red
    }
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host "`n==========================================" -ForegroundColor Cyan
Write-Host " Readiness Check Complete." -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan
