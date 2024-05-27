# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

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