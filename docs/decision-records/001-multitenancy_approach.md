<!-- Template based on: https://adr.github.io/madr/ -->

<!-- # These are optional elements. Feel free to remove any of them.
# status: {proposed | rejected | accepted | deprecated | … | superseded by [ADR-0005](0005-example.md)}
# date: {YYYY-MM-DD when the decision was last updated}
# deciders: {list everyone involved in the decision}
# consulted: {list everyone whose opinions are sought (typically subject-matter experts); and with whom there is a two-way communication}
# informed: {list everyone who is kept up-to-date on progress; and with whom there is a one-way communication} -->
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->
# Use a multi gate deployment approach to realize multi-tenancy

* status: accepted
* date: 2023-06-01
* deciders: devs, architects
* consulted: ea, pca 

## Context and Problem Statement

In BPDM a wide range of CX Member share their business partner data with our system. It must be ensured that each CX Member has only access to its own data. That's why our system must realize some kind of multi-tenancy.

<!-- This is an optional element. Feel free to remove. -->
## Decision Drivers

* in the automotive industry there are requirements and standards like TISAX that high confidential business partner data must be stored in secure manner

## Considered Options

* Use one Gate and implement multi-tenancy within the code base and database
* Use multiple Gates so that every member will have its own Gate with database

## Decision Outcome

Chosen option: "Use multiple Gates so that every member will have its own Gate with database", because so far its the most easiest and secure way to realize multi-tenancy in context of a reference implementation. It also provides the highest flexibility regarding to possible upcoming requirements. For example perspectively Gates could be deployed in different regions or locations. Also data is stored by default in different databases which gives additional security by default.

<!-- This is an optional element. Feel free to remove. -->
### Consequences

* Good, because easier Identity and Access Management
* Good, because data separation by default
* Good, because better failure tolerance.
* Good, because flexibility in upcoming requirements.
* Bad, because we need a separate deployment and configuration for a new Gate when a new CX Member wants to use BPDM Service. As reference implementation this is fine, for production Usecases these deployments can be automated.

### Implications on EDC and Asset Configuration
* Even if there are multiple BPDM Gate instances there will be only one deployed EDC
* In fact, new EDC Assets and Configurations must be applied for each new Catena-X Member who subscribes BPDM Application Service
* In context of reference implementation it is done manually. For operationalization an Operator should automate this.

### Implications on SMEs
* To exchange business partner data accross legal entities and enabling contract negotiation, each SME needs to have its own EDC
* The EDC itself can be provided as offer by the operator or other "EDC as a Service" Service Provider
  
### Implications on Value-Added-Services
* Currently it is out-of-scope that BPDM provides a kind of list or routing mechanism about which Gates are available to consume. The team is evaluating the possibility getting this information based on Catena-X Portal registrations.
* In fact for reference implementation a customer who wants to subscribe a Value Added Services has to provide his Gate/EDC Endpoints
* The Value Added Services also have to ensure by its own to secure and separate the data of each customer 

<!-- This is an optional element. Feel free to remove. -->
## Pros and Cons of the Options

### Use one Gate and implement multi-tenancy within the code base and database

* Good, because only one deployment is required
* Good, because better cost saving, because only one database is used
<!-- use "neutral" if the given argument weights neither for good nor bad -->
* Bad, because higher implementation effort
* Bad, because unknown requirements in data separation. If data **must** be stored in different databases, all our efforts would be for nothing.
* … <!-- numbers of pros and cons can vary -->

### Use multiple Gates so that every member will have its own Gate with database

* Good, because easier Identity and Access Management
* Good, because data separation by default
* Good, because better failure tolerance.
* Good, because flexibility in upcoming requirements.
* Bad, because we need a separate deployment and configuration for a new Gate when a new CX Member wants to use BPDM Service. As reference implementation this is fine, for production Usecases these deployments can be automated.

<!-- This is an optional element. Feel free to remove. -->
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

