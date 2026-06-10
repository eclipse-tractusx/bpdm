# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

## [7.0.0] - 2026-06-10

### Added

- The chart now generates and wires all connection and credential Secrets on install: the bundled Postgres connection (database user and password), the per-service Keycloak client configuration, the in-cluster client URLs and the Keycloak realm import. The stack comes up pre-wired without having to configure passwords, client secrets or service URLs by hand. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- OAuth client secrets and the bundled Postgres password are auto-generated when left empty and preserved across upgrades. Pin a known value by setting `bpdmRealm.clients.<client>.secret` or `postgres.customUser.password`. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- Post-install notes describing how to reach the deployed applications (Swagger UI), the Keycloak admin console, and how to run the bundled end-to-end smoke test. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- The bundled end-to-end smoke test (`helm test`) is wired through the umbrella-generated config Secrets like the applications: base-urls come from the client-url-config Secret (now also carrying the Gate base-url) and credentials from a dedicated tester Keycloak config Secret that authenticates as `BPDM_ADMIN` against Gate, Pool and Orchestrator. Toggle it via `tests.enabled` (off by default). [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)

### Breaking

- Replaced Bitnami Postgres dependency with CloudPirates Postgres dependency [#1625](https://github.com/eclipse-tractusx/bpdm/issues/1625)
- The BPDM applications now connect to the bundled Postgres with a dedicated custom user scoped to the BPDM database instead of the Postgres admin user. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- Restructured `values.yaml`: the per-service `applicationConfig`/`applicationSecrets` blocks and the inline database and auth-server connection settings were replaced by umbrella-generated config Secrets referenced through each service's `externalApplicationConfig`. Customizations made under the removed keys must be migrated. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- Renamed the top-level `clients` value to `bpdmRealm.clients`; its `secret` fields now default to empty (auto-generated) instead of placeholder values. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- The bundled Keycloak now runs its own database (`keycloak.postgres.enabled: true`) instead of sharing the BPDM Postgres database; the previous `keycloak.database` settings were removed. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- The bundled Postgres and Keycloak are now addressed via `nameOverride`, so their in-cluster service names follow the `<release-name>-bpdm-postgres` / `<release-name>-bpdm-keycloak` pattern instead of the fixed `bpdm-postgres` / `bpdm-keycloak` names. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)

### Changed

- Increase appversion to 7.4.0-rc2
- Update BPDM Gate Chart to version 8.0.0
- Update BPDM Pool Chart to version 9.0.0
- Update BPDM Orchestrator Chart to version 5.0.0
- Update BPDM Cleaning Service Dummy Chart to version 5.0.0
- Fixed the bundled Postgres toggle: the dependency condition `postgre.enabled` was corrected to `postgres.enabled`, so setting `postgres.enabled: false` now actually disables the bundled database. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- Updated the bundled Keycloak dependency from 0.19.8 to 0.21.10. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)
- Expanded the chart description with an overview of the golden record showcase and a quick-start guide. [#1708](https://github.com/eclipse-tractusx/bpdm/issues/1708)

## [6.3.0] - 2026-03-6

- Increase appversion to 7.3.0

## [6.2.0] - 2025-12-1

- Increase appversion to 7.2.0

## [6.1.0] - 2025-09-30

### Changed

- fix vulnerability in deployments by providing read-only volume mounts
- migrated Bitnami chart dependencies to the BitnamiLegacy repository.

## [6.0.0] -  2025-06-16

### Changed

- Increase appversion to 7.0.0
- update BPDM Pool Chart to version 8.0.0
- update BPDM Gate Chart to version 7.0.0
- update BPDM Orchestrator Chart to version 4.0.0
- update BPDM Cleaning Service Dummy Chart to version 4.0.0

## [5.3.0] -  2025-03-06

### Changed

- Increase appversion to 6.3.0
- update BPDM Pool Chart to version 7.3.0
- update BPDM Gate Chart to version 6.3.0
- update BPDM Orchestrator Chart to version 3.3.0
- update BPDM Cleaning Service Dummy Chart to version 3.3.0
- update BPDM Bridge Chart to version 3.3.0
- update Central-IDP dependency to 4.0.0 [#1145](https://github.com/eclipse-tractusx/bpdm/pull/1145)
- Add missing seccomp profiles for BPDM application containers [#1152](https://github.com/eclipse-tractusx/bpdm/issues/1152)
- Add missing resource limits to the initcontainer [##1154](https://github.com/eclipse-tractusx/bpdm/issues/1154)

## [5.2.0] -  2024-11-28

### Changed

- Increase appversion to 6.2.0
- update BPDM Pool Chart to version 7.2.0
- update BPDM Gate Chart to version 6.2.0
- update BPDM Orchestrator Chart to version 3.2.0
- update BPDM Cleaning Service Dummy Chart to version 3.2.0
- update BPDM Bridge Chart to version 3.2.0
- Replace Keycloak dependency with Central-IDP dependency [#994](https://github.com/eclipse-tractusx/bpdm/issues/994)
- Fix bug on disabling Postgres and Central-IDP dependencies. Dependencies are now directly referenced by fullnameOverrides [#1086](https://github.com/eclipse-tractusx/bpdm/issues/1086)
- Add missing security context to startupDelay init containers in BPDM deployments [#1089](https://github.com/eclipse-tractusx/bpdm/pull/1089)

## [5.1.0] -  2024-07-15

### Changed

- Increase appversion to 6.1.0
- update BPDM Pool Chart to version 7.1.0
- update BPDM Gate Chart to version 6.1.0
- update BPDM Orchestrator Chart to version 3.1.0
- update BPDM Cleaning Service Dummy Chart to version 3.1.0
- update BPDM Bridge Chart to version 3.1.0

## [5.0.1] -  2024-05-27

### Changed

- Increase appversion to 6.0.1
- Default pull policy from 'Always' to 'IfNotPresent'
- Fixes BPDM applications not connecting with each other when authenticated
- update BPDM Pool Chart to version 7.0.1
- update BPDM Gate Chart to version 6.0.1
- update BPDM Orchestrator Chart to version 3.0.1
- update BPDM Cleaning Service Dummy Chart to version 3.0.1
- update BPDM Bridge Chart to version 3.0.1

## [5.0.0] - 2024-05-15

### Major Breaking

- Postgres Chart dependency version from 14 to 15: When using the Postgres deployment managed by the BPDM Chart please mind to migrate your data as this update is not backwards compatible

### Added

- Keycloak dependency loading a default Cx-Central realm
- BPDM common library chart containing utility template functionality

### Changed

- update BPDM Pool Chart to version 7.0.0
- update BPDM Gate Chart to version 6.0.0
- update BPDM Orchestrator Chart to version 3.0.0
- update BPDM Cleaning Service Dummy Chart to version 3.0.0
- update BPDM Bridge Chart to version 3.0.0
- support BPDM application version 6.0.0 (Please consult the app's release and changelog for the result impact)
- Postgresql dependency now has a fullnameOverride on default

### Removed

- BPDM Bridge Helm Chart

## [4.0.4] - 2024-03-29

### Changed

- update BPDM Pool Chart to version 6.0.4
- update BPDM Gate Chart to version 5.0.4
- update BPDM Orchestrator Chart to version 2.0.4
- update BPDM Cleaning Service Dummy Chart to version 2.0.4
- update BPDM Bridge Chart to version 2.0.4
- remove fullNameOverride from postgres

## [4.0.3] - 2024-03-12

### Changed

- update BPDM Pool Chart to version 6.0.3
- update BPDM Gate Chart to version 5.0.3
- update BPDM Orchestrator Chart to version 2.0.3
- update BPDM Cleaning Service Dummy Chart to version 2.0.3
- update BPDM Bridge Chart to version 2.0.3

## [4.0.2] - 2024-03-01

### Changed

- update BPDM Pool Chart to version 6.0.2
- update BPDM Gate Chart to version 5.0.2
- update BPDM Orchestrator Chart to version 2.0.2
- update BPDM Cleaning Service Dummy Chart to version 2.0.2
- update BPDM Bridge Chart to version 2.0.2

### Removed

- postgres fullNameOverride

## [4.0.1] - 2024-02-23

### Changed

- update BPDM Pool Chart to version 6.0.1
- update BPDM Gate Chart to version 5.0.1
- update BPDM Orchestrator Chart to version 2.0.1
- update BPDM Cleaning Service Dummy Chart to version 2.0.1
- update BPDM Bridge Chart to version 2.0.1
  removed mentions of Opensearch from README

## [4.0.0] - 2024-02-10

### Changed

- update application version to 5.0.0
- update BPDM Pool Chart to version 6.0.0
- update BPDM Gate Chart to version 5.0.0
- update BPDM Orchestrator Chart to version 2.0.0
- update BPDM Cleaning Service Dummy Chart to version 2.0.0
- update BPDM Bridge Chart to version 2.0.0
- update copyright for 2024

## [3.1.2] - 2023-11-16

### Changed

- update BPDM Pool dependency to 5.1.1
- remove Opensearch dependency

## [3.1.1] - 2023-11-10

### Changed

- update application version to 4.1.0

## [3.1.0] - 2023-11-03

### Added

- Subchart BPDM Cleaning Service Dummy
- Subchart BPDM Orchestrator

### Changed

- increase to app version 4.1.0

## [3.0.4] - 2023-08-28

### Changed

- update application version to 4.0.1