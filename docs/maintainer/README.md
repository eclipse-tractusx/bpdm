# Maintainer View

Documentation for the maintainers of this repository — the duties no single contribution covers.
Contributors do not need this view; see the [developer view](../developer/README.md) instead.

<!-- TOC -->
* [Maintainer View](#maintainer-view)
  * [Responsibilities](#responsibilities)
  * [Release Process](#release-process)
  * [End-to-End Testing](#end-to-end-testing)
  * [Association Environments](#association-environments)
  * [NOTICE](#notice)
<!-- TOC -->

## Responsibilities

| Area                    | What the maintainer owns                                                                                              |
|-------------------------|-----------------------------------------------------------------------------------------------------------------------|
| Contribution rules      | Reviewing pull requests against the [developer guides](../developer/README.md); keeping those guides current           |
| Release process         | Carrying the release check issue through the cycle, cutting release candidates and the release, passing the quality gate, opening the next development cycle |
| End-to-end testing      | Keeping the Jira/Xray test definitions current, running them against the INT deployment for every release, and linking the execution as release evidence |
| Association environments| Keeping the BPDM deployment on INT current; updating STABLE when an update is announced                                |

The release itself is not signed off here: the Catena-X release management team signs off the [release check issue](release-process.md#the-two-tracking-issues) in `eclipse-tractusx/sig-release`.

## Release Process

The [release process guide](release-process.md) walks through a release cycle step by step, from opening the release check issue to the release management team's sign-off.
The [Catena-X Release Guidelines](https://eclipse-tractusx.github.io/docs/release) are the binding authority for releasing a Tractus-X product and take precedence over anything documented here.

## End-to-End Testing

The [end-to-end testing guide](e2e-testing.md) describes how the release's system tests are managed in Jira/Xray, how the system tester is run against a deployed environment, and how the resulting test execution is reported back.

## Association Environments

The [environments guide](environments.md) describes the BPDM deployments on the Catena-X association environments — what runs where, how they are deployed via ArgoCD, and how their secrets are managed.
The deployment configuration itself lives on the [`environments` branch](https://github.com/eclipse-tractusx/bpdm/tree/environments).

---

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2026 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2026 SAP SE
- SPDX-FileCopyrightText: 2023,2026 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2026 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2026 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2026 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2026 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm
