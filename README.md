Spring 4.1 compliant app that demonstrates the authentication and authorization of a user via JWT

## Deployment

The Kubernetes deployment lives in its own ops repository:
[user_mgmt_gitops](https://github.com/Luca-Guettinger/user_mgmt_gitops) — Helm chart plus the
ArgoCD Application that keeps the cluster in sync with it. This repo only builds and publishes
the container images.

Three images are built and pushed to GHCR on every push to `main`:
`user-mgmt-backend` (this Gradle project), `auth-portal` (`auth_portal-main/`) and
`module-service` (`module_service/`). Only the first two are wired into the GitOps repo;
the module service is published but not yet deployed from here.
