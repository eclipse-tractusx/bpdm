# bpdm-pool

![Version: 8.1.0-SNAPSHOT](https://img.shields.io/badge/Version-8.1.0--SNAPSHOT-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 7.1.0-SNAPSHOT](https://img.shields.io/badge/AppVersion-7.1.0--SNAPSHOT-informational?style=flat-square)

A Helm chart for deploying the BPDM pool service

**Homepage:** <https://eclipse-tractusx.github.io/docs/kits/Business%20Partner%20Kit/Adoption%20View>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Nico Koprowski |  |  |
| Sujit Karne |  |  |

## Source Code

* <https://github.com/eclipse-tractusx/bpdm>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| file://../bpdm-common | bpdm-common | 1.0.5 |
| https://charts.bitnami.com/bitnami | postgres(postgresql) | 12.12.10 |
| https://eclipse-tractusx.github.io/charts/dev | centralidp(centralidp) | 4.2.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions[0].key | string | `"app.kubernetes.io/name"` |  |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.labelSelector.matchExpressions[0].operator | string | `"DoesNotExist"` |  |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].podAffinityTerm.topologyKey | string | `"kubernetes.io/hostname"` |  |
| affinity.podAntiAffinity.preferredDuringSchedulingIgnoredDuringExecution[0].weight | int | `100` |  |
| applicationConfig | string | `nil` |  |
| applicationSecrets.spring.dataSource.password | string | `"bpdm"` |  |
| autoscaling.enabled | bool | `false` |  |
| centralidp.enabled | bool | `true` |  |
| configMountSecurity.readOnly | bool | `true` |  |
| configMountSecurity.recursiveReadOnly | string | `"Enabled"` |  |
| fullnameOverride | string | `nil` |  |
| image.pullPolicy | string | `"IfNotPresent"` |  |
| image.registry | string | `"docker.io"` |  |
| image.repository | string | `"tractusx/bpdm-pool"` |  |
| image.tag | string | `""` |  |
| imagePullSecrets | list | `[]` |  |
| ingress.annotations | object | `{}` |  |
| ingress.enabled | bool | `false` |  |
| ingress.hosts | list | `[]` |  |
| ingress.tls | list | `[]` |  |
| livenessProbe.failureThreshold | int | `5` |  |
| livenessProbe.httpGet.path | string | `"/actuator/health/liveness"` |  |
| livenessProbe.httpGet.port | int | `8080` |  |
| livenessProbe.httpGet.scheme | string | `"HTTP"` |  |
| livenessProbe.initialDelaySeconds | int | `5` |  |
| livenessProbe.periodSeconds | int | `10` |  |
| livenessProbe.successThreshold | int | `1` |  |
| livenessProbe.timeoutSeconds | int | `1` |  |
| nameOverride | string | `nil` |  |
| nodeSelector | object | `{}` |  |
| podAnnotations | object | `{}` |  |
| postgres.auth.database | string | `"bpdm"` |  |
| postgres.auth.password | string | `"bpdm"` |  |
| postgres.auth.username | string | `"bpdm"` |  |
| postgres.enabled | bool | `false` |  |
| readinessProbe.failureThreshold | int | `5` |  |
| readinessProbe.httpGet.path | string | `"/actuator/health/readiness"` |  |
| readinessProbe.httpGet.port | int | `8080` |  |
| readinessProbe.httpGet.scheme | string | `"HTTP"` |  |
| readinessProbe.initialDelaySeconds | int | `5` |  |
| readinessProbe.periodSeconds | int | `10` |  |
| readinessProbe.successThreshold | int | `1` |  |
| readinessProbe.timeoutSeconds | int | `1` |  |
| replicaCount | int | `1` |  |
| resources.limits.cpu | string | `"1000m"` |  |
| resources.limits.memory | string | `"1Gi"` |  |
| resources.requests.cpu | string | `"100m"` |  |
| resources.requests.memory | string | `"1Gi"` |  |
| securityContext.allowPrivilegeEscalation | bool | `false` |  |
| securityContext.capabilities.drop[0] | string | `"ALL"` |  |
| securityContext.readOnlyRootFilesystem | bool | `true` |  |
| securityContext.runAsGroup | int | `10001` |  |
| securityContext.runAsNonRoot | bool | `true` |  |
| securityContext.runAsUser | int | `10001` |  |
| securityContext.seccompProfile.type | string | `"RuntimeDefault"` |  |
| service.port | int | `80` |  |
| service.targetPort | int | `8080` |  |
| service.type | string | `"ClusterIP"` |  |
| springProfiles | list | `[]` |  |
| startupDelaySeconds | int | `90` |  |
| startupProbe.failureThreshold | int | `40` |  |
| startupProbe.httpGet.path | string | `"/actuator/health/readiness"` |  |
| startupProbe.httpGet.port | int | `8080` |  |
| startupProbe.httpGet.scheme | string | `"HTTP"` |  |
| startupProbe.initialDelaySeconds | int | `30` |  |
| startupProbe.periodSeconds | int | `5` |  |
| startupProbe.successThreshold | int | `1` |  |
| startupProbe.timeoutSeconds | int | `1` |  |
| tolerations | list | `[]` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)
