# Trivy Image Scan Report

## Target

- Image: `consentledger-backend:latest`
- Command:

```bash
trivy image --severity HIGH,CRITICAL --format table consentledger-backend:latest
```

## Current status

Pending local execution. Run the command after building the backend image.

## Review notes

- Treat `CRITICAL` findings as release blockers unless there is a documented exception.
- Prioritize base image and framework dependency upgrades for recurring findings.
- Re-run the scan after changing the Dockerfile or dependency lock files.

## Evidence template

Paste the scan summary below after running Trivy:

```text
Total: 0 (HIGH: 0, CRITICAL: 0)
```
