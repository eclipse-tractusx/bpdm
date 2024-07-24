# System Scope and Context


## Business Context

The following figure depicts the business context setup for BPDM:

![bpdm_business_context](assets/cx_bpdm_context_business.drawio.svg)

The following are the various components of the business context setup:

**Master Data Management (Catena-X Member)**
* A backend system that's operated by a company which is participating in the Catena-X Ecosystem and consuming digital services or data assets.

**Small-Medium-Enterprises (SME) (Catena-X Member)**
* A SME company that's participating in the Catena-X Ecosystem and consuming digital services or data assets.

**Catena-X Portal/Marketplace (CX Portal)**
* The Portal which provides an entry point for the Catena-X Members, to discover Apps that are offered in Catena-X.

**Value Added Services**
* Value Added Services can be provided be either the Operator itself or by an external App/Service Provider. The Value Added Services provide data or service offers based on Catena-X Network data.
* There are several value added services that can be offered in context of business partner data. For example a Fraud Prevention Dashboard/API, Country Risk Scoring and so on.

**Catena-X Operative Environment for BPDM**
* Within Catena-X there will be only one central operation environment that operates the BPDM Application. This operative environment provides the services and data for other operation environment or applications which needs to consume business partner data or golden record data.

**Catena-X BPDM Application**
* The BPDM Application which offers services to Catena-X Members, Catena-X Use Cases and Catena-X BPDM Value Added Services for consuming and processing business partner data as well as Golden Record Information and BPN Numbers.

**Curation & Enrichment Services**
* To offer the BPDM and Golden Record Services, Catena-X uses services from external third party service providers. These can either be operated by the operator itself or external companies that have a contract with the operator.

## Technical Context

The technical context setup including deployment is depicted in the following figure:
![cx_bpdm_deployment_context](assets/cx_bpdm_context_technical.drawio.svg)

* The BPDM Application follows a microservice approach to separate the different components of the system.
* Within Catena-X there will be only one central operation environment that operates the BPDM Application. This operation environment provides the services and data for other operation environment or applications which needs to consume business partner data or golden record data.

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