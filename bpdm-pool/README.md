# BPDM Pool

The BPDM Pool is a core service with a standardized API used within data spaces to provide a centralized repository for business partner data (Golden Records) as a single source of truth, enabling Data Space Participants to access them for a legally secure identification of other Data Space Participants, invitation management and the general data exchange in a data space. Each Golden Record (legal entities, sites and addresses) in the BPDM Pool has the BPN as a unique identifier.

The Pool offers an [API](../bpdm-pool-api) to query business partner data records (Golden Records) by BPN, other identifiers or by text search.

The BPN Issuing Service is a core service that issues and assigns the globally unique, stable and interoperable Business Partner Number (BPN) to all organizations and organization parts (legal entities, sites and addresses) of the supply chain, with or about which data is exchanged in the data space. The Business Partner Number for legal entities (BPNL) additionally is the one and only legally secure identifier for the Data Space Participants, required to conclude legally binding data exchange contracts.

Note that the BPN Issuing Service is currently not a separately deployable service, but a part of the BPDM Pool.

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
