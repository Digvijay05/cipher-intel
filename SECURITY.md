# Security Policy — CIPHER

## Reporting Vulnerabilities

If you discover a security vulnerability, please report it privately via GitHub Security Advisories or email the maintainer directly. **Do not open a public issue.**

## Secret Management Rules

### Never Commit
- `.env` files (use `.env.example` with placeholders)
- `*.jks` / `*.keystore` files
- `local.properties`
- API keys, tokens, passwords in source code

### Where Secrets Live
| Environment | Location |
|---|---|
| Local dev | `.env` (gitignored) |
| Android local | `local.properties` (gitignored) |
| CI/CD | GitHub Actions Secrets |
| Production | Render environment variables |

### Android
- Use `BuildConfig.API_KEY` and `BuildConfig.BASE_URL` — never inline strings
- Gradle reads from `System.getenv()` with safe fallbacks for local dev
- Release builds must NOT contain debug URLs

### Backend
- All secrets loaded via `pydantic-settings` from environment
- No `os.getenv()` outside `app/config/settings.py`
- Stack traces disabled in production (`LOG_LEVEL=WARNING`)

### Tests
- Use synthetic test keys (`"test-key"`) — never real credentials
- Load test scripts read from `CIPHER_API_KEY` env var

## Pre-Commit Checks

Install gitleaks locally:
```bash
# macOS
brew install gitleaks

# Windows (scoop)
scoop install gitleaks

# Scan before pushing
gitleaks detect --source . --config .gitleaks.toml
```
