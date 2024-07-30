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