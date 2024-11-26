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

### Central-IDP

As of now the [Central-IDP](https://github.com/eclipse-tractusx/portal-iam) in release 3.1.0 is not fully compatible with the standard configuration of the BPDM [rights and role concepts](08_Crosscutting_Concepts.md#authentication--autorization).
Therefore, the standard configuration of the BPDM deployment will **not** work with Central-IDP out of the box.
BPDM standard configuration needs to be adapted to be compatible.
This can be done over the application.yml files locally or in case of a Helm deployment these configuration overwrites can be placed in the `applicationConfig` values of the BPDM components (see [INSTALL](../../INSTALL.md) instructions).
[Here](#helm-configuration) a full Helm configuration for the needed adaptions is given.


#### Pool and Gate Clients

Client names for the Pool and Gate need to be adapted in the BPDM configuration in order to match with the Central-IDP configuration.

```yaml
# Pool application.yml
bpdm:
  security:
    client-id: "Cl7-CX-BPDM"
```

```yaml
# Gate application.yml
bpdm:
  security:
    client-id: "Cl16-CX-BPDMGate"
```

#### Orchestrator Authorization

The Central-IDP does not contain the roles and permissions for the BPDM Orchestrator as described in the [rights and role concepts chapter](08_Crosscutting_Concepts.md#authentication--autorization) and has no dedicated client for the Orchestrator component.

The configuration of the BPDM Orchestrator can be adapted to compensate for the mismatch.

```yaml
# Orchestrator application.yml
bpdm:
  security:
    client-id: "Cl7-CX-BPDM"
    permissions:
      createTask: "write_partner"
      readTask: "write_partner"
      reservation:
        clean: "write_partner"
        cleanAndSync: "write_partner"
        poolSync: "write_partner"
      result:
        clean: "write_partner"
        cleanAndSync: "write_partner"
        poolSync: "write_partner"
```

With this configuration the Orchestrator looks into the permissions of the Pool receiving a bearer token.
Furthermore, the permission overwrites let the Orchestrator only be accessed by services that already have direct write permissions to the Pool, saying:

If you can directly write into the Pool you are also allowed to manage tasks which lead to Pool updates.

The permissions to access the Orchestrator endpoints are stricter in this way than they have to be with dedicated Orchestrator permissions.


#### Service Accounts

BPDM components need service accounts with the necessary permissions to connect with each other in an authenticated deployment.
As of now, the Central-IDP does not feature dedicated service accounts for single BPDM components but rather has one overall BPDM Admin service account.
This one service account needs to be used by the BPDM components in order to connect to each other and realise the golden record process. 

```yaml
# Pool application.yml
bpdm:
    client:
        orchestrator:
          registration:
            client-id: "sa-cl7-cx-5"
            client-secret: "*****"
```

```yaml
# Gate application.yml
bpdm:
    client:
        orchestrator:
          registration:
            client-id: "sa-cl7-cx-5"
            client-secret: "*****"
        pool:
          registration:
            client-id: "sa-cl7-cx-5"
            client-secret: "*****"
```

```yaml
# Cleaning Service dummy application.yml
bpdm:
    client:
        orchestrator:
          registration:
            client-id: "sa-cl7-cx-5"
            client-secret: "*****"
        pool:
          registration:
            client-id: "sa-cl7-cx-5"
            client-secret: "*****"
```

#### Helm Configuration

Here is a full helm values overwrite file containing all adaptions needed for the configuring the BPDM deployment to be compatible with the Central-IDP:

```yaml
# BPDM values overwrite
keycloak:
  enabled: false
  
bpdm-pool:
  applicationConfig:
    bpdm:
      security:
        auth-server-url: "http://central-idp-host-name/auth"
        client-id: "Cl7-CX-BPDM"
      client:
        orchestrator:
          registration:
            client-id: "sa-cl7-cx-5"       
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: "*****"

bpdm-gate:
  applicationConfig:
    bpdm:
      security:
        auth-server-url: "http://central-idp-host-name/auth"
        client-id: "Cl7-CX-BPDM"
      client:
        orchestrator:
          registration:
            client-id: "sa-cl7-cx-5"
        pool:
          registration:
            client-id: "sa-cl7-cx-5"
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: "*****"
        pool:
          registration:
            client-secret: "*****"
            
bpdm-orchestrator:
  applicationConfig:
    bpdm:
      security:
        auth-server-url: "http://central-idp-host-name/auth"
        client-id: "Cl7-CX-BPDM"
        permissions:
          createTask: "write_partner"
          readTask: "write_partner"
          reservation:
            clean: "write_partner"
            cleanAndSync: "write_partner"
            poolSync: "write_partner"
          result:
            clean: "write_partner"
            cleanAndSync: "write_partner"
            poolSync: "write_partner"

bpdm-cleaning-service-dummy:
  applicationConfig:
    bpdm:
      client:
        orchestrator:
          provider:
            issuer-uri: "https://central-idp-host-name/auth/realms/CX-Central"
          registration:
            client-id: "sa-cl7-cx-5"
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: "*****"
```

#### Long-term mitigation

Note that the mitigating BPDM configuration above has a major drawback.
All systems directly integrating into the BPDM components need to be absolutely trusted as they receive write privileges on the pool.

The long-term resolution is to align the BPDM authorization concept with the Central-IDP configuration.
A proposal for this has already been submitted [here](https://github.com/eclipse-tractusx/portal-iam/issues/154).

Another step in this direction is also using the Central-IDP as a dependency directly for the BPDM Helm charts as proposed [here](https://github.com/eclipse-tractusx/bpdm/issues/994).

### Partner Network shows all golden records

Currently, the Portal's Partner Network page shows all business partners even though the intention is to only show Catena-X members.

#### Mitigation

In order to resolve this issue a different endpoint needs to be integrated: `POST v6/members/legal-entities/search`

Additionally, Central-IDP and the Portal need to make sure that the Portal users with the portal role `Cx-User` receive only the BPDM role `Pool Cx Member` as described in the [roles and rights concept](08_Crosscutting_Concepts.md#authentication--autorization).

### Managing own company data error

When trying to manage own company data on the Portal's UI we are currently experiencing an error for being unauthorized.

This renders the Portal user with the role `Service Manager` unable to manage their own data.

#### Mitigation

The Portal can mitigate this issue by making sure that the portal user with role `Service Manager` obtains the role `Gate Admin` with permissions as defined in the [roles and rights concept](08_Crosscutting_Concepts.md#authentication--autorization).

### Exposed technical users

Through the Portal's marketplace service and subscription process the subscribing company receive access to the created BPDM technical users.
This leads to the danger of companies bypassing the EDC offers and directly accessing the BPDM APIs.

Since this behaviour of creating technical users is an ingrained feature of the Portal there is no quick resolution to that mismatch.

#### Mitigation

As a mitigation the BPDM provider who is also the operator of the Central-IDP can decide to not use the automatic tehcnical user creation process of the Portal.
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