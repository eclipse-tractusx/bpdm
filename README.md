# BPDM

## Project Description

This repository is part of the overarching Eclipse Tractus-X project.

BPDM is an acronym for business partner data management.
This project serves two main purposes:

1. Provide services for querying and sharing business partner information
2. Establish an infrastructure for realising the Golden Record process which turns business partner information from sharing members to Golden Records, that is
   cleaned and enriched business partner data uniquely identified by a business partner number (BPN)

The following sections give an overview of this repository's structure.

## BPDM Applications

Heart of this project is the source code for the BPDM applications.
Together, those applications provide the infrastucture for the Golden Record process.
The BPDM solution contains the following applications:

- [Pool](bpdm-pool): The single source of truth for Golden Records and BPNs
- [Gate](bpdm-gate): Holds business partner data from sharing members and allows them to share the data to the golden record process
- [Orchestrator](bpdm-orchestrator): Facilitates business partner data between Gate, Pool and external cleaning services
- [Cleaning Service Dummy](bpdm-cleaning-service-dummy): A dummy implementation of a cleaning service, responsible for processing business partner data and
  turning it one step closer to a Golden Record

Subfolders for BPDM applications are easily recognizable by the `bpdm` prefix.

## Installation

Please consult the [INSTALL](INSTALL.md) documentation file for in-depth installation instructions.

## Usage

BPDM is an application environment designed to be interacted with over APIs.
Therefore, please consult the [api](docs/api/README.md) documentation for getting to know how to use BPDM.

## Documentation

This README is just the gateway to more detailed documentation files that may be found in the [docs](docs) folder.

Also, each of the BPDM applications has its own README file, which can be found in the respective subfolder:

- [BPDM Pool](bpdm-pool/README.md)
- [BPDM Gate](bpdm-gate/README.md)
- [BPDM Orchestrator](bpdm-orchestrator/README.md)
- [BPDM Cleaning Service Dummy](bpdm-cleaning-service-dummy/README.md)

## Docker Notice

Below you can find information to the used Docker images in this application:

- [BPDM Pool](docker/pool/DOCKER_NOTICE.md)
- [BPDM Gate](docker/gate/DOCKER_NOTICE.md)
- [BPDM Orchestrator](docker/orchestrator/DOCKER_NOTICE.md)
- [BPDM Cleaning Service Dummy](docker/cleaning-service-dummy/DOCKER_NOTICE.md)

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024,2025 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024,2025 SAP SE
- SPDX-FileCopyrightText: 2023,2024,2025 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024,2025 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024,2025 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024,2025 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024,2025 Contributors to the Eclipse Foundation
- Source URL: <https://github.com/eclipse-tractusx/bpdm>
