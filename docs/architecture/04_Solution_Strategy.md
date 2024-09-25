# Solution Strategy (High Level Picture)
The following high level view gives a basic overview about the BPDM Components:

![cx_bpdm_highlevel](assets/cx_bpdm_highlevel.drawio.svg)


**BPDM Gate**
* The BPDM Gate provides the interfaces for Catena-X Members to manage their business partner data within Catena-X.
* Based on the network data a Golden Record Proposal is created.
* The BPDM Gate has its own persistence layer in which the business partner data of the Catena-X Members are stored.
* For the current reference implementation, multi-tenancy is realized via a 1:1 deployment for each Catena-X Member. This means that every Catena-X Member who shares his business partner data, has its own Gate and own persistence.

**BPDM Pool**
* The BPDM Pool is the central instance for business partner data within Catena-X.
* The BPDM Pool provides the interface and persistance for accessing Golden Record Data and the unique Business Partner Number.
* In comparison to the BPDM Gate, there is only one central instance of the BPDM Pool.

**BPN Issuer**
* Every participant in the Catena-X network shall have a unique Business Partner Number (BPN) according to the concept defined by the Catena-X BPN concept. The task of the BPN Generator is to issue such a BPN for a presented Business Partner data object. In that, the BPN Generator serves as the central issuing authority for BPNs within Catena-X.
* Technically, it constitutes a service that is available as a singleton within the network.
* Currently, creation of BPNs is part of the BPDM Pool implementation. After implementing the BPDM Orchestrator, it can be considered if it should be an independent component.

**BPDM Orchestrator**
* Intention of the BPDM Orchestrator is to provide a passive component that offers standardized APIs for the BPDM Gate, BPDM Pool and Data Curation and Enrichment Services to orchestrate the process of Golden Record Creation and handling the different states a business partner record can have during this process.

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