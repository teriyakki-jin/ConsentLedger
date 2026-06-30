# kube-bench Cluster Security Report

## Target

- Cluster: local `kind` or `minikube`
- Benchmark: CIS Kubernetes Benchmark
- Command:

```bash
kube-bench run
```

For a kind cluster, kube-bench can also be run as a Kubernetes Job:

```bash
kubectl apply -f https://raw.githubusercontent.com/aquasecurity/kube-bench/main/job.yaml
kubectl logs job/kube-bench
```

## Current status

Pending local execution after cluster creation.

## Review notes

- Record failed items and classify them as local-cluster limitation, accepted risk, or required fix.
- For managed Kubernetes such as EKS, distinguish control-plane checks from worker-node checks.
- Keep remediation evidence with the command output used for verification.

## Evidence template

Paste the kube-bench summary below after running the scan:

```text
== Summary ==
0 checks PASS
0 checks FAIL
0 checks WARN
0 checks INFO
```
