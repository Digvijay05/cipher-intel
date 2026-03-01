# Repository Security Hardening Guide

## 1. Core Secret Management Guidelines
- **Zero-Commit Policy**: Sensitive extensions precisely matching `.env`, `.pem`, `.jks`, `.keystore`, `.b64`, `.p12`, or `*.cert` are strictly forbidden from branching.
- **Environment Driven Injection**: Application configurations inherently must securely resolve through local `os.environ` OS variables, bypassing statically bundled config lists implicitly.
- **CI/CD Abstraction**: Leverage explicit GitHub Actions Secrets (or matching runner mechanisms) exclusively to distribute runtime states downstream.

## 2. Implemented Hardening Configurations
- **Pre-commit Scan Checks**: Commits explicitly mandate execution through a pre-commit verification hooking (e.g. `gitleaks` & `trufflehog`) identifying high-entropy signatures.
- **Strict `.gitignore` Boundaries**: Explicitly defining directories, ensuring localized configurations mapping to IDE tools or binaries ignore deployment trees.

## 3. Accidental Leak Remediation Workflow
1. **Never use `git rm` independently**: Purging conventionally leaves underlying metadata permanently inscribed in git history blobs.
2. **Engage `git filter-repo`**: Securely sever specific paths recursively manipulating commit trees.
3. **Mandatory Key Rotation**: Assumed compromise protocol must take place whenever keys touch upstream limits.
