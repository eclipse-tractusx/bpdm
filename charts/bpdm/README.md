# bpdm

![Version: 5.0.2](https://img.shields.io/badge/Version-5.0.2-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 6.0.2](https://img.shields.io/badge/AppVersion-6.0.2-informational?style=flat-square)

A Helm chart for Kubernetes that deploys the BPDM applications

**Homepage:** <https://github.com/eclipse-tractusx/bpdm>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Nico Koprowski |  |  |
| Fabio D. Mota |  |  |

## Source Code

* <https://github.com/eclipse-tractusx/bpdm>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| file://./charts/bpdm-cleaning-service-dummy | bpdm-cleaning-service-dummy(bpdm-cleaning-service-dummy) | 3.0.2 |
| file://./charts/bpdm-common | bpdm-common | 1.0.1 |
| file://./charts/bpdm-gate | bpdm-gate(bpdm-gate) | 6.0.2 |
| file://./charts/bpdm-orchestrator | bpdm-orchestrator(bpdm-orchestrator) | 3.0.2 |
| file://./charts/bpdm-pool | bpdm-pool(bpdm-pool) | 7.0.2 |
| https://charts.bitnami.com/bitnami | keycloak(keycloak) | 19.3.0 |
| https://charts.bitnami.com/bitnami | postgres(postgresql) | 12.12.10 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| bpdm-cleaning-service-dummy.applicationConfig.bpdm.client.orchestrator.provider.issuer-uri | string | `"http://bpdm-keycloak/realms/CX-Central"` |  |
| bpdm-cleaning-service-dummy.applicationSecrets.bpdm.client.orchestrator.registration.client-secret | string | `"dummy_orch_client_secret"` |  |
| bpdm-cleaning-service-dummy.enabled | bool | `true` |  |
| bpdm-gate.applicationConfig.bpdm.security.auth-server-url | string | `"http://bpdm-keycloak"` |  |
| bpdm-gate.applicationSecrets.bpdm.client.orchestrator.registration.client-secret | string | `"gate_orch_client_secret"` |  |
| bpdm-gate.applicationSecrets.bpdm.client.pool.registration.client-secret | string | `"gate_pool_client_secret"` |  |
| bpdm-gate.enabled | bool | `true` |  |
| bpdm-gate.postgres.enabled | bool | `false` |  |
| bpdm-gate.postgres.fullnameOverride | string | `"bpdm-postgres"` |  |
| bpdm-orchestrator.applicationConfig.bpdm.security.auth-server-url | string | `"http://bpdm-keycloak"` |  |
| bpdm-orchestrator.enabled | bool | `true` |  |
| bpdm-pool.applicationConfig.bpdm.security.auth-server-url | string | `"http://bpdm-keycloak"` |  |
| bpdm-pool.applicationSecrets.bpdm.client.orchestrator.registration.client-secret | string | `"pool_orch_client_secret"` |  |
| bpdm-pool.enabled | bool | `true` |  |
| bpdm-pool.postgres.enabled | bool | `false` |  |
| bpdm-pool.postgres.fullnameOverride | string | `"bpdm-postgres"` |  |
| keycloak.auth.adminPassword | string | `"admin"` |  |
| keycloak.auth.adminUser | string | `"admin"` |  |
| keycloak.bpdm.realm.clientSecrets.cleaningDummyOrchestrator | string | `"dummy_orch_client_secret"` |  |
| keycloak.bpdm.realm.clientSecrets.gateOrchestrator | string | `"gate_orch_client_secret"` |  |
| keycloak.bpdm.realm.clientSecrets.gatePool | string | `"gate_pool_client_secret"` |  |
| keycloak.bpdm.realm.clientSecrets.poolOrchestrator | string | `"pool_orch_client_secret"` |  |
| keycloak.enabled | bool | `true` |  |
| keycloak.externalDatabase.database | string | `"bpdm"` |  |
| keycloak.externalDatabase.host | string | `"bpdm-postgres"` |  |
| keycloak.externalDatabase.password | string | `"bpdm"` |  |
| keycloak.externalDatabase.user | string | `"bpdm"` |  |
| keycloak.extraEnvVars[0].name | string | `"KEYCLOAK_EXTRA_ARGS"` |  |
| keycloak.extraEnvVars[0].value | string | `"--import-realm"` |  |
| keycloak.extraVolumeMounts[0].mountPath | string | `"/opt/bitnami/keycloak/data/import"` |  |
| keycloak.extraVolumeMounts[0].name | string | `"import"` |  |
| keycloak.extraVolumeMounts[0].readOnly | bool | `true` |  |
| keycloak.extraVolumes[0].name | string | `"import"` |  |
| keycloak.extraVolumes[0].secret.items[0].key | string | `"Cx-Central.json"` |  |
| keycloak.extraVolumes[0].secret.items[0].path | string | `"Cx-Central.json"` |  |
| keycloak.extraVolumes[0].secret.secretName | string | `"bpdm-keycloak-realm"` |  |
| keycloak.fullnameOverride | string | `"bpdm-keycloak"` |  |
| keycloak.livenessProbe.initialDelaySeconds | int | `0` |  |
| keycloak.postgresql.enabled | bool | `false` |  |
| keycloak.production | bool | `false` |  |
| keycloak.readinessProbe.initialDelaySeconds | int | `0` |  |
| keycloak.resources.limits.cpu | string | `"500m"` |  |
| keycloak.resources.limits.memory | string | `"512Mi"` |  |
| keycloak.resources.requests.cpu | string | `"100m"` |  |
| keycloak.resources.requests.memory | string | `"512Mi"` |  |
| keycloak.startupProbe.enabled | bool | `true` |  |
| keycloak.startupProbe.failureThreshold | int | `40` |  |
| keycloak.startupProbe.initialDelaySeconds | int | `60` |  |
| keycloak.startupProbe.periodSeconds | int | `30` |  |
| postgres.auth.database | string | `"bpdm"` |  |
| postgres.auth.password | string | `"bpdm"` |  |
| postgres.auth.username | string | `"bpdm"` |  |
| postgres.enabled | bool | `true` |  |
| postgres.fullnameOverride | string | `"bpdm-postgres"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.13.1](https://github.com/norwoodj/helm-docs/releases/v1.13.1)
