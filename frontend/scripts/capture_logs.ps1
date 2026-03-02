$OutputFile = "cipher_execution_logs.txt"

Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "   Cipher SDK Log Capture Utility         " -ForegroundColor Cyan
Write-Host "==========================================" -ForegroundColor Cyan

Write-Host "Dumping full logcat from the device..." -ForegroundColor Yellow
# Run adb logcat to dump current buffer
adb logcat -d > full_dump.txt

Write-Host "Filtering for Cipher SMS Pipeline events..." -ForegroundColor Yellow
# Select strings that match our custom components, package name, or general app traces
$Patterns = "SmsReceiver|SmsProcessingWorker|EngagementWorker|SmsDeliveryReceiver|com.cipher.security|Retrofit|FATAL EXCEPTION"

# Do regex search and output to file
Select-String -Path full_dump.txt -Pattern $Patterns | Select-Object -ExpandProperty Line | Out-File $OutputFile -Encoding UTF8

# Cleanup
Remove-Item full_dump.txt

Write-Host ""
Write-Host "✅ Logs successfully extracted!" -ForegroundColor Green
Write-Host "File created at: $(Resolve-Path $OutputFile)" -ForegroundColor Green
Write-Host "Please share the contents of '$OutputFile' to debug the EngagementWorker flow." -ForegroundColor Magenta
Write-Host ""
