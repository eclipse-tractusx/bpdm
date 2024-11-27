# Risks and Technical Debts

## Risks

**Dependency on third party service provider**
* Currently we are not flexible enough to easily change the third party service provider for golden record creation. Therefore the next step will be to introduce an own data persistence layer, getting more independent.
* ✔️Solved via "Simulator Application"

**Data Storage and anonymize concept**
* How to anonymize the relations between CX-Member and its belonging Business Partner?
* 💡 Idea: using kind of "ticket numbering"
* ✔️ Solved via ticketing.

**Accessability for SMEs**
* Uploading via CSV File. Does it requires an EDC?
* ⚠️Current State: Yes, is needed.

## Technical Debts

### Exposed technical users on Portal

Through the Portal's marketplace service and subscription process the subscribing company receive access to the created BPDM technical users.
This leads to the danger of companies bypassing the EDC offers and directly accessing the BPDM APIs.

Since this behaviour of creating technical users is an ingrained feature of the Portal there is no quick resolution to that mismatch.

#### Mitigation

As a mitigation the BPDM provider who is also the operator of the Central-IDP can decide to not use the automatic technical user creation process of the Portal.
As a result, when BPDM services are requested the operator needs to create technical users directly in the Central-IDP.
These hidden technical users can then be used to configure [EDC assets](../../INSTALL.md#edc-installation).

### Dummy Golden Record Process Restrictions

A golden record process which is implemented by using the BPDM Cleaning Service Dummy has some unique restrictions.

#### Categorization

The dummy golden record process can not effectively categorize the shared business partner address and determine the golden records affected on its own.
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

- Confidence Criteria: The dummy golden record service fills all confidence criteria with static dummy values.



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