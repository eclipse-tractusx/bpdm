# Changelog

All notable changes to this project will be documented in this file.

The format is based on Keep a Changelog (https://keepachangelog.com/en/1.0.0/),

## [5.0.1] - 2024-02-23

### Changed

- set default memory limit and request to 512 Mi

## [5.0.0] - 2024-02-10

### Changed

- Update application version to 5.0.0
- increase container's default groupid to 10001
- container is now executed with read-only root file systems
- update copyright for 2024

## [4.1.0] - 2023-11-03

### Changed

- Update application version to 4.1.0

## [4.0.1] - 2023-08-28

### Changed

- Update application version to 4.0.1

## [4.0.0] - 2023-08-18

### Changed

- Update application version to 4.0.0
- add missing license headers to ingress templates
- change default registry for image to dockerhub

### Added

- postgres chart dependency for persistence

## [3.3.0] - 2023-03-17

### Changed

- Update application version to 3.2.0

## [3.2.0] - 2023-03-16

### Changed

- Startup, Readiness and Liveness probes can now be fully configured over the values

## [3.1.0] - 2023-03-08

### Changed

- Update application version to 3.1.0

## [3.0.6] - 2023-02-24

### Changed

- Update application version to 3.0.3

## [3.0.5] - 2023-02-16

### Changed

- Update application version to 3.0.2

### Fixed

- fixed bug causing missing apiVersion on Ingress resource
- fixed port of startup probe
- fixed liveness probe endpoint

## [3.0.4] - 2022-01-27

### Added

- LICENSE file
- README file
- Copyright headers
- CHANGELOG file

## [3.0.3] - 2022-01-25

### Changed

- Update application version to 3.0.1

## [3.0.2] - 2022-01-23

## Changed

- Image now being pulled from catenax-ng/tx-bpdm by default.

## [3.0.1] - 2022-01-20

## Changed

- Update application version to 3.0.0

## Notice

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
