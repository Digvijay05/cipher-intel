# Key Rotation Log

| Date | Component | Reason | Target Identifier | Executed By | Status |
|---|---|---|---|---|---|
| 2026-03-01 | Android Keystore | Pushed to Git History | `frontend/app/keystore.jks` | Security Team | Pending Rewrite |
| 2026-03-01 | Base64 CI String | Pushed to Git History | `frontend/app/keystore.b64` | Security Team | Pending Rewrite |

## Authorized Procedure
1. Existing keystores designated formally as breached and rotated.
2. Clean `release.jks` generated exclusively server-side using RSA/2048 guidelines.
3. Key aliases, IDs, and passwords transposed explicitly to CI/CD encrypted remote secret variables.
4. Active notification submitted to Google Play Console outlining an 'App Signing Key Upgrade' overriding the compromised artifacts.
