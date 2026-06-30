# ConsentLedger Kubernetes Hardening Checklist

## Workload security

- [x] Backend runs with `runAsNonRoot`, non-zero UID/GID, and `seccompProfile: RuntimeDefault`.
- [x] Backend drops Linux capabilities and disables privilege escalation.
- [x] Backend uses a read-only root filesystem with writable `/tmp` mounted as `emptyDir`.
- [x] CPU and memory requests/limits are configured for backend and PostgreSQL.
- [x] Readiness and liveness probes are configured for backend and PostgreSQL.
- [x] Service account token automount is disabled for the backend workload.

## Configuration and secrets

- [x] Runtime configuration is separated into `ConfigMap`.
- [x] Database credentials and API key are separated into `Secret`.
- [ ] Replace sample secret values before deployment.
- [ ] Use Sealed Secrets, External Secrets, or a cloud secret manager for production.

## Network security

- [x] Default ingress is denied in the `consentledger` namespace.
- [x] PostgreSQL accepts traffic only from backend pods.
- [x] Backend ingress is limited to the ingress controller namespace.
- [x] Backend egress is limited to PostgreSQL and cluster DNS.
- [ ] Confirm the local CNI enforces `NetworkPolicy` before relying on it.

## Cluster checks

- [ ] Run Trivy image scan and save the output in `security/trivy-report.md`.
- [ ] Run kube-bench and save the output in `security/kube-bench-report.md`.
- [ ] Review `kubectl describe`, `kubectl logs`, and namespace events after deployment.
- [ ] Enable Kubernetes audit logging in managed or production clusters.

## Production follow-up

- [ ] Pin images by immutable tag or digest instead of `latest`.
- [ ] Configure TLS for Ingress.
- [ ] Add backup and restore procedure for PostgreSQL PVC data.
- [ ] Add PodDisruptionBudget for the backend after validating replica behavior.
- [ ] Use managed PostgreSQL for production-grade availability and patching.
