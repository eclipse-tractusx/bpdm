# Building Block View


## High-Level Architecture (Generic Endpoint)

![bpdm_current_architecture_Generic](assets/cx_bpdm_architecture_v3_3.drawio.svg)

**Simulator Service**
* To become more independent in testing the BPDM Application, a Simulator Service was developed.
* The Simulator Services supports the E2E Test Cases to validate the flow from BPDM Gate to BPDM Pool and back again.

**EDC Operator**
* The diagram above shows two EDCs on Operator side. This is only for visualization purpose. On a technical level there is only one EDC.

**SME**
* Currently there is no SME Application available

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2024 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2024 SAP SE
- SPDX-FileCopyrightText: 2023,2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2024 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2024 Robert Bosch GmbH
- SPDX-FileCopyrightText: 2023,2024 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2024 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm