$adb = "adb"
$action = "com.cipher.security.DEBUG_SMS"
$component = "com.cipher.security/.receiver.SmsReceiver"
$from = "+916351753750"
$body = "Dear Customer,
Your bank account has been temporarily suspended due to suspicious activity. Failure to verify your details within 2 hours will result in permanent closure.
Click here to update KYC:
http://secure-bank-verification-update.com
 Customer Security Team"
# $bodyBase64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($body))
Write-Host "Sending debug SMS via ADB..."

# 1. Convert payload strings to Base64 in PowerShell to avoid Windows CLI escaping hell
$Bytes = [System.Text.Encoding]::UTF8.GetBytes($body)
$Base64Body = [Convert]::ToBase64String($Bytes)

# 2. Construct a multi-line shell script that decodes the body and runs the command locally on Android 
$AdbCmd = @"
DECODED_BODY=`$(echo '$Base64Body' | base64 -d)
am broadcast -a $action -n $component --es sender '$from' --es body "`$DECODED_BODY" --ei subscriptionId -1
"@

# 3. Pipe to adb shell's standard input!
$AdbCmd | adb shell

Write-Host "Done. Check logcat for 'SmsReceiver' and 'EngagementWorker' logs."
