# Risks and Technical Debts

## Risks

**Dependency on third party service provider**
* Originally the golden record creation could not be changed to a different third party service provider without effort.
* ✔️ Solved: the BPDM Orchestrator holds the golden record tasks and their data, and a refinement service integrates by reserving a step over a standardized API. The Cleaning Service Dummy is such a service and lets the stack run without any provider under contract.

**Data Storage and anonymize concept**
* How to anonymize the relations between CX-Member and its belonging Business Partner?
* 💡 Idea: using kind of "ticket numbering"
* ✔️ Solved via the golden record tasks in the Orchestrator: a task carries business partner data, not the identity of the Gate that created it.

**Accessability for SMEs**
* An SME has no master data management system to share data from.
* ✔️ Solved for the upload itself: the Gate accepts a CSV file over its partner upload endpoints and offers a template for it.
* ⚠️ Reaching the Gate across legal entities still requires an EDC, so an SME needs one - either its own or one offered as a service.

## Technical Debts

### Dummy Golden Record Process Restrictions

A golden record process which is implemented by using the BPDM Cleaning Service Dummy has some unique restrictions.

#### Categorization

The dummy golden record process can not effectively categorize the shared business partner address and determine the golden records affected on its own.
It goes by what the record itself states: the address type and whether site information is present decide which golden record type the task expresses, and the dummy processes that type.
Therefore, in order to reliably share business partner data which is known to be of a certain type - for example it is known that it contains a legal address - this information has to be provided with the business partner input.

The dummy service has the following behaviour for categorizing business partner data to be created or updated:

| Has Site Information | Address Type                  | Golden Record Result                           |
|----------------------|-------------------------------|------------------------------------------------|
| No                   | NULL                          | Legal Entity                                   |
| Yes                  | NULL                          | Site with legal address as site main address   |
| -                    | Legal Address                 | Legal Entity                                   |
| -                    | Legal And Site Main Address   | Site with legal address as site main address   |
| -                    | Site Main Address             | Site                                           |
| No                   | Additional Address            | Additional Address of Legal Entity             |
| Yes                  | Additional Address            | Additional Address of Site                     |


#### Cleaning Data

The dummy service does not clean or correct incorrect data.
This means typos or incorrect address data or names are not corrected.

This is especially important for references to metadata information in the Pool.
Legal Forms, identifier types and administrative areas need to be referenced by their technical key/ISO code as listed in the metadata Pool endpoints.

#### Duplication Check

The dummy service matches business partner data by BPN or name - if no BPN has been provided.
If a BPN has been provided the dummy service expects that the referenced golden record already exists and will fail to process the data if the golden record with that BPN can not be found.
If no BPN has been provided business partners are matched by name.
The name has to be case-sensitively exactly match.
If no business partner can be matched by name it will be created (only in case no BPN has been provided).

#### Data Provisioning


- Missing Parents: If the golden record process determines a record to be a new additional address it may be necessary to also create its golden record parents - legal entity and site.
  If a site or legal entity parent have to be created, the dummy service uses the additional address values for the legal and site main address.
  Likewise, if a legal entity parent has to be created for a site the legal address information is taken from the site main address.
- Confidence Criteria: The dummy golden record service fills the confidence criteria with static dummy values, except for the flag stating that the data was shared by its owner, which it derives from the record's owning company.

#### Business Partner Relations

The dummy service reserves relation tasks and resolves them with exactly the content it received.
It performs no duplication check, no validation and no correction on a relation.
A golden record process that is supposed to refine relations therefore requires an actual refinement service.

#### Declared Additional Sites

A sharing member can state the further sites an address belongs to.
The Pool takes that list together with the record's own site as the address's complete site membership and unlinks whatever it leaves out.
Consolidating the records that share one address into that complete list needs the whole stream of records over time, which the dummy service does not have - it is handed one record per task and keeps no ledger of earlier ones.
It therefore passes the sharing member's statement through unchanged, which means an address named by several records ends up with the sites of whichever record was refined last.
A refinement service meant for production has to keep that ledger.

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