# bpdm

![Version: 7.1.0](https://img.shields.io/badge/Version-7.1.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 7.5.0](https://img.shields.io/badge/AppVersion-7.5.0-informational?style=flat-square)

Deploys the full BPDM application stack as a single release and configures it end to
end, showcasing how a golden record process is realized for a single sharing member.

**Homepage:** <https://github.com/eclipse-tractusx/bpdm>

## Maintainers

| Name | Email | Url |
| ---- | ------ | --- |
| Nico Koprowski |  |  |

## Source Code

* <https://github.com/eclipse-tractusx/bpdm>

## Requirements

| Repository | Name | Version |
|------------|------|---------|
|  | bpdm-cleaning-service-dummy(bpdm-cleaning-service-dummy) | 5.1.0 |
|  | bpdm-common | 2.0.0 |
|  | bpdm-gate(bpdm-gate) | 8.1.0 |
|  | bpdm-orchestrator(bpdm-orchestrator) | 5.1.0 |
|  | bpdm-pool(bpdm-pool) | 9.1.0 |
| oci://registry-1.docker.io/cloudpirates | keycloak | 0.21.10 |
| oci://registry-1.docker.io/cloudpirates | postgres(postgres) | 0.11.0 |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| bpdm-cleaning-service-dummy.enabled | bool | `true` |  |
| bpdm-cleaning-service-dummy.externalApplicationConfig[0] | string | `"{{ include \"bpdm.cleaningDummyKeycloakConfig.name\" . }}"` |  |
| bpdm-cleaning-service-dummy.externalApplicationConfig[1] | string | `"{{ include \"bpdm.clientUrlConfig.name\" . }}"` |  |
| bpdm-gate.enabled | bool | `true` |  |
| bpdm-gate.externalApplicationConfig[0] | string | `"{{ include \"bpdm.postgresConnectionConfig.name\" . }}"` |  |
| bpdm-gate.externalApplicationConfig[1] | string | `"{{ include \"bpdm.gateKeycloakConfig.name\" . }}"` |  |
| bpdm-gate.externalApplicationConfig[2] | string | `"{{ include \"bpdm.clientUrlConfig.name\" . }}"` |  |
| bpdm-orchestrator.enabled | bool | `true` |  |
| bpdm-orchestrator.externalApplicationConfig[0] | string | `"{{ include \"bpdm.postgresConnectionConfig.name\" . }}"` |  |
| bpdm-orchestrator.externalApplicationConfig[1] | string | `"{{ include \"bpdm.orchestratorKeycloakConfig.name\" . }}"` |  |
| bpdm-pool.enabled | bool | `true` |  |
| bpdm-pool.externalApplicationConfig[0] | string | `"{{ include \"bpdm.postgresConnectionConfig.name\" . }}"` |  |
| bpdm-pool.externalApplicationConfig[1] | string | `"{{ include \"bpdm.poolKeycloakConfig.name\" . }}"` |  |
| bpdm-pool.externalApplicationConfig[2] | string | `"{{ include \"bpdm.clientUrlConfig.name\" . }}"` |  |
| bpdmRealm.clients.admin.secret | string | `""` |  |
| bpdmRealm.clients.cleaningDummy.secret | string | `""` |  |
| bpdmRealm.clients.gate.secret | string | `""` |  |
| bpdmRealm.clients.gateInputConsumer.secret | string | `""` |  |
| bpdmRealm.clients.gateInputManager.secret | string | `""` |  |
| bpdmRealm.clients.gateOutputConsumer.secret | string | `""` |  |
| bpdmRealm.clients.orchestrator.secret | string | `""` |  |
| bpdmRealm.clients.participant.secret | string | `""` |  |
| bpdmRealm.clients.pool.secret | string | `""` |  |
| bpdmRealm.clients.refinerClean.secret | string | `""` |  |
| bpdmRealm.clients.refinerCleanAndSync.secret | string | `""` |  |
| bpdmRealm.clients.refinerPoolSync.secret | string | `""` |  |
| bpdmRealm.clients.sharingMember.secret | string | `""` |  |
| bpdmRealm.clients.taskCreator.secret | string | `""` |  |
| keycloak.enabled | bool | `true` |  |
| keycloak.extraVolumeMounts[0].mountPath | string | `"/opt/keycloak/data/import"` |  |
| keycloak.extraVolumeMounts[0].name | string | `"realm-config"` |  |
| keycloak.extraVolumeMounts[0].readOnly | bool | `true` |  |
| keycloak.extraVolumes[0].name | string | `"realm-config"` |  |
| keycloak.extraVolumes[0].secret.secretName | string | `"{{ include \"bpdm.realmConfig.name\" . }}"` |  |
| keycloak.keycloak.production | bool | `false` |  |
| keycloak.mariadb.enabled | bool | `false` |  |
| keycloak.nameOverride | string | `"bpdm-keycloak"` |  |
| keycloak.postgres.enabled | bool | `true` |  |
| keycloak.realm.import | bool | `true` |  |
| keycloak.service.httpPort | int | `80` |  |
| postgres.customUser.database | string | `"bpdm"` |  |
| postgres.customUser.existingSecret | string | `"{{ include \"bpdm.postgresConnectionConfig.name\" . }}"` |  |
| postgres.customUser.name | string | `"bpdm"` |  |
| postgres.customUser.secretKeys.database | string | `"database"` |  |
| postgres.customUser.secretKeys.name | string | `"username"` |  |
| postgres.customUser.secretKeys.password | string | `"password"` |  |
| postgres.enabled | bool | `true` |  |
| postgres.nameOverride | string | `"bpdm-postgres"` |  |
| tests.enabled | bool | `false` |  |
| tests.filterTags | string | `""` |  |
| tests.image.pullPolicy | string | `"IfNotPresent"` |  |
| tests.image.registry | string | `"docker.io"` |  |
| tests.image.repository | string | `"tractusx/bpdm-system-tester"` |  |
| tests.image.tag | string | `""` |  |
| tests.securityContext.allowPrivilegeEscalation | bool | `false` |  |
| tests.securityContext.capabilities.drop[0] | string | `"ALL"` |  |
| tests.securityContext.readOnlyRootFilesystem | bool | `true` |  |
| tests.securityContext.runAsGroup | int | `10001` |  |
| tests.securityContext.runAsNonRoot | bool | `true` |  |
| tests.securityContext.runAsUser | int | `10001` |  |
| tests.securityContext.seccompProfile.type | string | `"RuntimeDefault"` |  |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.14.2](https://github.com/norwoodj/helm-docs/releases/v1.14.2)
