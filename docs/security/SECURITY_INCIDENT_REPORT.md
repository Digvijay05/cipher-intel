# Security Incident Report: Android Keystore Leak

## 1. Incident Overview
- **Incident Date**: 2026-03-01
- **Severity**: Critical
- **Status**: Preparing Remediation
- **Description**: Android application signing keys (`keystore.jks` and `keystore.b64`) were inadvertently committed to the project's source code repository, exposing sensitive cryptographic material within the git history.

## 2. Blast Radius & Impact Analysis
- **Exposure Level**: High. Keystore secrets exist in raw git blobs across potentially multiple commits.
- **Known Risks**: A malicious actor obtaining the `.jks` file could cryptographically sign unauthorized backdoored APKs mapping perfectly to our build footprint, breaching user data securely and bypassing standard verification.
- **Affected Artifacts**: All historical Android release builds utilizing the compromised keys are theoretically poisoned unless proactively revoked at the console level.

## 3. Remediation Actions
1. **Repository Freeze**: Execute a formal freeze to clear commits syncing against local states.
2. **Key Rotation**: Compromised keystore effectively revoked. A clean keystore must be provisioned and hydrated identically entirely in isolated external CI vaults.
3. **Commit History Purge**: Systematically extract the binary trees utilizing `git filter-repo`, ensuring no trace remains inside `.git/objects`.
4. **Environment Hardening**: Configure `.gitignore` definitions, add static PR secret validation hooks, and update documentation directives.

## 4. Root Cause 
Secret keys were stored locally within the application package directory `frontend/app` and inadvertently slipped past initial `.gitignore` tracking.

## 5. Preventative Measures 
- Static automated secret-scanning workflows deployed in CI.
- Rigid pre-commit policies analyzing high-entropy payload patterns.
- Revamped `CONTRIBUTING.md` standards demanding OS Environment Variable structures exclusively.
