# BPDM

## Project Description

This repository is part of the overarching Eclipse Tractus-X project.

BPDM is an acronym for business partner data management.
This project provides core services for querying, adding and changing business partner base information in the Eclipse Tractus-X landscape.

Currently, BPDM consists of the Pool and Gate services.

## BPDM Pool

The BPDM Pool is the single source of truth in Eclipse Tractus-X for business partner base information such as addresses and official identifiers.
Each record in the Pool has a unique identifier with which it can be referenced across the entire Eclipse Tractus-X landscape, the business partner number.
Business partner records are divided into legal entities, sites and partner addresses.
Self-explanatory, a legal entity record represents the legal entity information about a business partner.
A site may represent legal entity's plant or campus which is big enough to contain several contact/delivery addresses.
Finally, an address partner is a location of legal entity or site with a single contact/delivery address.
A legal entity may have several sites and address partner.
Further, a site may have several address partners.

The Pool offers an API to query these business partner records by BPN, other identifier or by text search.

## BPDM Gate

The BPDM Gate offers an API for Eclipse Tractus-X members to share their own business partner data with Eclipse Tractus-X. Such members are called sharing
members.
Via the Gate service they can add their own business partner records but also retrieve cleaned and enhanced data back in return over the sharing process.
Shared business partner records that have successfully gone through the sharing process end up in the BPDM Pool and will receive a BPN there (or merge with an
existing record).

## Installation

For installation instructions for the BPDM applications please refer to the [INSTALL](INSTALL.md) file.

## Container images

This application provides container images for demonstration purposes.
The base image used, to build this demo application image is `eclipse-temurin:17-jre-alpine`

Docker Hub:

* [eclipse-temurin](https://hub.docker.com/_/eclipse-temurin)
* [17-jre-alpine](https://hub.docker.com/layers/library/eclipse-temurin/17-jre-alpine/images/sha256-02c04793fa49ad5cd193c961403223755f9209a67894622e05438598b32f210e?context=explore)

Source:

* [temurin-build](https://github.com/adoptium/temurin-build)
* [temurin docker repo info](https://github.com/docker-library/repo-info/tree/master/repos/eclipse-temurin)

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

## License Check

Licenses of all maven dependencies need to be approved by eclipse.
The Eclipse Dash License Tool can be used to check the license approval status of dependencies and to request reviews by the intellectual property team.

Generate summary of dependencies and their approval status:

```bash
mvn org.eclipse.dash:license-tool-plugin:license-check -Ddash.summary=DEPENDENCIES
```

Automatically create IP Team review requests:

```bash
mvn org.eclipse.dash:license-tool-plugin:license-check -Ddash.iplab.token=<token>
```

Check the [Eclipse Dash License Tool documentation](https://github.com/eclipse/dash-licenses) for more detailed information.