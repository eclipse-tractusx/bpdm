<!-- Template based on: https://adr.github.io/madr/ -->

<!-- These are optional elements. Feel free to remove any of them. -->
* status: proposed
* date: 2023-09-04
* deciders: catena-x bpdm team
<!-- * consulted: {list everyone whose opinions are sought (typically subject-matter experts); and with whom there is a two-way communication} -->
<!-- * informed: {list everyone who is kept up-to-date on progress; and with whom there is a one-way communication} -->
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->
# Recommended usage scenarios of an EDC enabled communication in Business Partner Data Management Solution 

## Context and Problem Statement

Again and again the discussion arises in which scenarios third party applications (also often called value-added-services (VAS)) must use EDC enabled communication and in which scenarios no EDC is needed. In this document we want to outpoint some scenarios and give guidance for it.

> :warning: NOTE: <p>
> In the following diagrams the EDC component might be added multiple times within the same operating environment. This does not mean that multiple instances of EDC are used. It should only make more transparent when data or API calls takes place via EDC. It's on conceptual level, not on logical or physical. It's up to you how many instances of EDC you are operating.

### TLDR;

> EDC enabled communication must always be used when business data get exchanged between the systems of different legal entities!

> For reference implementations you should always assume that the value-added-service will be operated by a different operating environment than the operating environment of the core Business Partner Data Management Solution! That means the reference implementation must support EDC enabled communication between itself and the Business Partner Data Management Solution!

## Scenario 1.1: External web application/service that only visualizes data based on gate data and/or pool data

### Description
In this scenario a third party service provider offers a value added services that implements a web dashboard to visualize processed data based on bpdm gate data and/or pool data and presenting it via this dashboard to the customer who owns the bpdm gate data.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.

> No EDC is needed for presenting the visualization via a web frontend to the customer.

![External web application that only visualizes data based on gate data](../assets/edc_usage_1_1.drawio.svg)

## Scenario 1.2: Internal web application that only visualizes data based on gate data and/or pool data

### Description
In this scenario the operating environment itself operates a web application that implements a web dashboard to visualize processed data based on bpdm gate data and/or pool data and presenting it via this dashboard to the customer who owns the bpdm gate data.

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> No EDC enabled communication is needed for the backend service, processing gate and/or pool data, since every component is operated by the same legal entity, the operating environment.

> No EDC is needed for presenting the visualization via a web frontend to the customer.
 

![Internal web app that only visualizes data based on gate data](../assets/edc_usage_1_2.drawio.svg)

## Scenario 2.1: External web application/service that provides enriched data based on gate data and/or pool data

### Description
In this scenario a third party service provider offers a value added services that implements an interface for exchanging data between its own backend system and the system of the customer. This means that business data get exchanged between the systems of two different legal entities. 

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.

> EDC enabled communication is needed between the value-added-service backend and the customer system.

![External web application that only visualizes data based on gate data](../assets/edc_usage_2_1.drawio.svg)

## Scenario 2.2: Internal web application/service that provides enriched data based on gate data and/or pool data

### Description
In this scenario the operating environment itself operates a backend service or value added service that processes bpdm gate and/or pool data and implements an interface for exchanging data between its own backend system and the system of the customer. This means that business data get exchanged between the systems of two different legal entities. 

> EDC enabled communication is needed between the Master Data Management System of the Sharing Member and the bpdm gate operated by the Operating Environment.

> EDC enabled communication is needed between the value-added-service backend and the customer system.

> No EDC enabled communication is needed between the bpdm gate and the backend service that processes the data.

> No EDC enabled communication is needed between the bpdm pool and the backend service that processes the data.


![Internal web application/service that provides enriched data based on gate data and/or pool data](../assets/edc_usage_2_2.drawio.svg)


## More Information

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- SPDX-FileCopyrightText: 2023,2023 ZF Friedrichshafen AG
- SPDX-FileCopyrightText: 2023,2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
- SPDX-FileCopyrightText: 2023,2023 Mercedes Benz Group
- SPDX-FileCopyrightText: 2023,2023 Schaeffler AG
- SPDX-FileCopyrightText: 2023,2023 Contributors to the Eclipse Foundation
- Source URL: https://github.com/eclipse-tractusx/bpdm

