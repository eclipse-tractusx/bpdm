# BPDM

## Project Description

This repository is part of the overarching Eclipse Tractus-X project.

BPDM is an acronym for business partner data management.
This project serves two main purposes:

1. Provide services for for querying and sharing business partner information
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
- [Bridge Dummy](bpdm-bridge-dummy): A legacy dummy implementation directly exchanging business partners between Gate and Pool

Subfolders for BPDM applications are easily recognizable by the `bpdm` prefix.

## Installation

Installation instructions for the BPDM services can be found in the following places:

1. [Local Installation](docs/OPERATOR_VIEW.md): Details how to install and configure the BPDM services on a host machine.
2. [Helm Installation](charts/bpdm/README.md): Explains how to use given Helm Charts to install the BPDM services on a kubernetes environment.

## Container images

BPDM provides container images for demonstration purposes.
The base image used, to build this demo application image is `eclipse-temurin:17-jre-alpine`

Docker Hub:

* [eclipse-temurin](https://hub.docker.com/_/eclipse-temurin)
* [17-jre-alpine](https://hub.docker.com/layers/library/eclipse-temurin/17-jre-alpine/images/sha256-02c04793fa49ad5cd193c961403223755f9209a67894622e05438598b32f210e?context=explore)

Source:

* [temurin-build](https://github.com/adoptium/temurin-build)
* [temurin docker repo info](https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin)

## Notice for Docker image

* [BPDM Pool](./bpdm-pool/DOCKER_NOTICE.md)
* [BPDM Gate](./bpdm-gate/DOCKER_NOTICE.md)
* [BPDM Orchestrator](./bpdm-orchestrator/DOCKER_NOTICE.md)
* [BPDM Cleaning Service Dummy](./bpdm-cleaning-service-dummy/DOCKER_NOTICE.md)
* [BPDM Bridge Dummy](./bpdm-bridge-dummy/DOCKER_NOTICE.md)

## GitHub Workflows

For releasing new Docker images of the BPDM Pool and Gate we use GitHub Actions/Workflows, by convention found in the `.github/workflows` folder.
On pushing to the main branch or creating a new Git tag the applications are containerized and pushed to the repository's GitHub Container Registry.
The containerization of the applications is based on the Dockerfiles found in the root folders of the Pool and Gate modules.
Released images are tagged according to the main branch or Git tag name.

In addition to the release of the applications' Docker images, there is also a workflow to release a corresponding Helm chart on Git tag creation.
Helm charts are released via the [helm/chart-releaser-action](https://github.com/helm/chart-releaser-action) and are stored in the `gh-pages` branch of the
repository.

Furthermore, apart from the release workflows there also exists code scanning workflows for quality assurance:

1. Before any release of Docker images GitHub executes unit and integration tests.
2. Periodically, workflows execute a KICS and Trivy scan to ensure quality standards of the Docker images and Helm charts.
3. For a more thorough security check the packaged applications are send to a VeraCode scan, which happens periodically and after a push to main

## Documentation

This README is just the gateway to more detailed documentation files that may be found in the [docs](docs) folder

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
