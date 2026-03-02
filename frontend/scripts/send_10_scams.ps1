$scamMessages = @(
    "Netflix: Your payment failed. Please update your billing information immediately to avoid service interruption at https://netflix-billing-update-secure.com",
    "USPS: Important! We tried to deliver your package but address is missing details. Update here: https://usps-tracking-auth.net",
    "Bank of America Security Alert: Unusual activity detected in your account. Reply 'YES' to confirm or verify here: https://bofa-verify-alert.com/login",
    "URGENT: Your PayPal account has been temporarily restricted due to suspicious logins. Secure it now: https://paypal-resolution-center.io",
    "Amazon: We've locked your account after a $849.99 purchase attempt from a new device. If this wasn't you, cancel it here: https://amazon-cancel-order.net",
    "IRS: You have a pending tax refund of $1,250. Click the link to claim your refund within 24 hours: https://irs-refund-claim.org",
    "Apple: Your Apple ID is going to expire today. Please prevent this by verifying your identity at https://apple-id-verify.con",
    "AT&T: Your access to online banking is blocked. Reactivate your access by verifying your details: https://att-security-auth.net",
    "Walmart Survey: Congrats! You've been chosen to win a free $500 Gift Card. Claim your prize here: https://walmart-rewards-winner.com",
    "Notice: Your auto insurance is about to expire, leading to fines. Renew immediately with our discounted partners: https://cheap-auto-insurance.net"
)

$targetPhone = "+916351753750"
$twilioScript = ".\scripts\twilio_sms.ps1"

Write-Host "Starting to send 10 scam messages via Twilio to $targetPhone..." -ForegroundColor Cyan

foreach ($msg in $scamMessages) {
    Write-Host "`nSending message:" -ForegroundColor Yellow
    Write-Host $msg

    # Execute the existing Twilio script with the current message
    powershell -File $twilioScript -Body $msg -TargetPhone $targetPhone

    # Add a small delay between messages to avoid potential rate limits
    Start-Sleep -Seconds 20
}

Write-Host "`nAll 10 messages have been sent." -ForegroundColor Green
