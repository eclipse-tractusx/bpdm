# INSTALL

BPDM offers Helm Charts, Dockerfiles and configuration files to support the installation process of the BPDM applications.
The following chapters show how to install the applications in different scenarios.

## Local Installation

### Requirements

* JAVA 21
* Maven (3.9 supported)
* Docker Engine (tested on 26.1.2)
* Docker Compose (tested on 2.27.0)

### Default Installation

BPDM services require a PostgreSQL database and Keycloak server to run.
Navigate to the root folder of the BPDM repository.
Then set up the necessary dependencies by using the provided Docker Compose file:

```console
docker compose -f docker/compose/dependencies/docker-compose.yml up -d 
```

This will run a Postgres and Keycloak with an initial realm already configured to be used by the BPDM services.
After this step, we can build and install the BPDM applications.

```console
mvn clean install
```

Each BPDM application can now be run separately.
We will run each application after another.
For this, we navigate into the application's subfolder and run the application with the spring-boot run goal.

```console
cd bpdm-orchestrator
mvn spring-boot:run
cd ../bpdm-cleaning-service-dummy
mvn spring-boot:run
cd ../bpdm-pool
mvn spring-boot:run
cd ../bpdm-gate
mvn spring-boot:run
```

After this you have full-fledged running BPDM system.
The Keycloak realm adheres to the configuration of the [Central-IDP](https://github.com/eclipse-tractusx/portal-iam/blob/main/docs/admin/technical-documentation/03.%20Clients.md#initial-clients-and-service-accounts).
For accessing the BPDM APIs you can choose one of the pre-existing clients over client authentication flow, notably sa-cl7-cx5 or sa-cl7-cx7 for access to Gate and Pool.
As a next step on how to pass business partner data and create golden records you can have a look at the [api documentation](docs/api/README.md) of this repository.

### Gate Configuration

#### Owned Gates

On default, the BPDM Gate supports a simple multi-tenancy feature.
Each company, identified by the BPNL of the accessing user, has a separate tenant of business partner data in one Gate.

Alternatively, a BPDM Gate can be configured to be single tenant only.
This allows access for users only from a single company identified by its BPNL.

```yaml
bpdm:
  bpn:
    owner-bpn-l: BPNLXXXXXXXXXX01
```
If it is set with a BPNL only users belonging to that company can access the Gate.
If it is not set or set to null a user from any company can use the Gate, albeit the user can only see its own company shared business partner data.

#### Manual Sharing

On default, the BPDM Gate automatically starts the golden record process for uploaded business partner data.
Alternatively, the Gate can be configured to not start the process automatically.
In this case business partner data can be uploaded and changed, before the user has to manually set the data to be ready to be shared to the golden record process.

```yaml
bpdm:
  tasks:
    creation:
      starts-as-ready: false
```

### Insecure Installation

You may want to perform a quick local installation in which security is not necessary.
Make sure to disable authentication requirements by using the provided `no-auth` profile when running the applications:

```console
mvn spring-boot:run -Dspring.profiles.active=no-auth
```

## Helm Charts

Installation of BPDM applications with the Helm Charts has the most software requirements but is the qickest way to set up a running system.

### Requirements

* kubectl (1.30 supported)
* Docker Engine (tested on 26.1.2)
* Minikube (tested on 1.33.0)
* Helm (tested on 3.14.4)


### Default Installation

Navigate to the projects root folder.
Then install a new release of BPDM on your default namespace via helm:

```console
helm install bpdm ./charts/bpdm
```
This will install the BPDM applications with its own Postgres and Keycloak in default values.

### Override Defaults

The easiest way to overrride default configuration of the BPDM Helm Chart is to provide a custom values file while deploying.

```bash
helm install bpdm --values path/to/values/file.yml ./charts/bpdm
```

The following sections provide example use cases for default overrides.


#### Overriding Postgres Passwords

If you want to change the default password of the given postgres database you can override the postgres configuration and the connection configuration of each BPDM app that uses the database:

```yaml
postgres:
  password: $PASSWORD
bpdm-gate:
  applicationSecrets:
    spring:
      datasource:
        password: $PASSWORD
bpdm-pool:
  applicationSecrets:
    spring:
      datasource:
        password: $PASSWORD
bpdm-orchestrator:
  applicationSecrets:
    spring:
      datasource:
        password: $PASSWORD
```

#### Overriding Central-IDP Secrets

You can use [seeding](https://github.com/eclipse-tractusx/portal-iam/tree/v3.0.1/charts/centralidp) for adding custom passwords to the Central-IDP dependency.
The new client clients secrets then need to be given to the connections of each BPDM app:

```yaml
bpdm-gate:
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $GATE_ORCH_CLIENT_SECRET
        pool:
          registration:
            client-secret: $GATE_POOL_CLIENT_SECRET
bpdm-pool:
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $POOL_ORCH_CLIENT_SECRET
bpdm-cleaning-service-dummy:
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $CLEANING_DUMMY_ORCH_CLIENT_SECRET
```

#### Insecure Installation

For non-production purposes you may want to install BPDM applications that are not authenticated.
All BPDM applications offer a Spring profile to quickly remove all authentication configuration for their APIs and client connections.
In this case you can also disable the Central-IDP dependency from being deployed.

```yaml
centralIdp:
  enabled: false
bpdm-gate:
  springProfiles:
    - no-auth
bpdm-pool:
  springProfiles:
    - no-auth
bpdm-cleaning-service-dummy:
  springProfiles:
    - no-auth
bpdm-orchestrator:
  springProfiles:
    - no-auth
```

You can also more fine-granulary remove authentication on APIs and BPDM client connections.
You can refer to the no-auth profile configurations (for example that of the [Gate](bpdm-gate/src/main/resources/application-no-auth.yml)) as a documentation.

#### Use External Dependencies

The BPDM Charts deploy their own PostgreSQL and Keycloak dependencies.
However, for production it is recommended to host dedicated Postgres and Keycloak instances with which the BPDM applications should connect to.

>Additional Requirements
>
> * Postgres (15.4.0 supported)
> * Keycloak (22.0.3 supported)

In this case, you can disable the dependencies and configure the connection to external systems in the application configuration.

```yaml
centralIdp:
  enabled: false
postgres:
  enabled: false
bpdm-gate:
  applicationConfig:
    bpdm:
      datasource:
        host: "http://remote-postgres"
      security:
        auth-server-url: "http://remote-centralIdp/auth"
bpdm-pool:
  applicationConfig:
    bpdm:
      datasource:
        host: "http://remote-postgres"
      security:
        auth-server-url: "http://remote-centralIdp/auth"
bpdm-orchestrator:
  applicationConfig:
    bpdm:
      datasource:
        host: "http://remote-postgres"
      security:
        auth-server-url: "http://remote-centralIdp/auth"
bpdm-cleaning-service-dummy:
  applicationConfig:
    bpdm:
      client:
        orchestrator:
          provider:
            issuer-uri: "http://remote-centralIdp/auth"
```

You can combine this configuration with the examples for overriding password and secrets to adapt BPDM's connection configuration to you wishes.

### Fine-granular Configuration

You can configure all BPDM applications over Helm values more fine-granulary via the `applicationConfig` and `applicationSecrets`.
Values under these groups are directly injected as application properties in the deployed containers.

As a reference of what can be changed have a look at the respective application properties files of each application:
- [BPDM Gate](bpdm-gate/src/main/resources/application.yml)
- [BPDM Pool](bpdm-pool/src/main/resources/application.yml)
- [BPDM Orchestrator](bpdm-orchestrator/src/main/resources/application.yml)
- [BPDM Cleaning Service Dummy](bpdm-cleaning-service-dummy/src/main/resources/application.yml)

## EDC Installation

This section shows how to make your BPDM Gate and Pool APIs available over an EDC.
This documentation assumes that you already have running BPDM and EDC deployments.
For deploying an EDC please consult the documentation on the [EDC repository](https://github.com/eclipse-tractusx/tractusx-edc).

### Requirements

* Running BPDM applications
* Running EDC (0.7.3 supported)

### Installation

The general idea of configuring data offers for BPDM is to assets which grant access to a portion of the BPDM APIs.
Which API resources are accessible over an asset is determined by the purposes defined in the BPDM framework agreement.
For some purposes you may need to access business partner output data from the BPDM Gate for example but won't have access to the input data.
Blueprints for such assets are documented in this [POSTMAN collection](docs/postman/EDC%20Provider%20Setup.postman_collection.json).
Accompanying the asset definitions are Policy and Contract Definition blueprints.
Except for a general Access Policy those blueprints are grouped by purpose.

After all assets, policies and contract definitions are configured a sharing company's EDC now can query its available assets and the contract under which they
are exposed.

### Creating Offers

Following are specific instructions on how to create offers to expose the BPDM APIs over EDC.
Note that the instructions reference definitions taken from the previously references Postman collection.

#### ReadAccessPoolForCatenaXMember

This offer allows a company to access the Pool for reading golden record data of all Catena-X members.

1. Create a policy of type `HasBusinessPartnerNumber` for the company's BPNL (if it not yet exists)
2. Create a policy of type `AcceptPurpose` with usage purpose for the pool (if not yet exists)
3. Create a technical user with role `BPDM Pool Consumer` and the company's BPN identity
4. Create a `ReadAccessPoolForCatenaXMember` asset with the created technical user for client credentials
5. Create a contract definition `ReadAccessPoolForCatenaXMember` referencing the created asset


#### FullAccessGateInputForSharingMember

This offer allows a company to access a Gate API for uploading business partner data and sending it to the golden record process.

1. Create a policy of type `HasBusinessPartnerNumber` for the company's BPNL (if it not already exists)
2. Create a policy of type `AcceptPurpose` with upload usage purpose for the gate (if it not already exists)
3. Create a technical user with role `BPDM Sharing Input Manager` and the company's BPN identity
4. Create a `FullAccessGateInputForSharingMember` asset with the created technical user for client credentials
5. Create a contract definition `FullAccessGateInputForSharingMember` referencing the created asset

#### ReadAccessGateInputForSharingMember

This offer allows a company to access a Gate API for reading business partner data that has been uploaded by the company.
This offer explicitly does **not** grant access to read the golden record output for the uploaded business partner data.

1. Create a policy of type `HasBusinessPartnerNumber` for the company's BPNL (if it not already exists)
2. Create a policy of type `AcceptPurpose` with download usage purpose for the gate (if it not already exists)
3. Create a technical user with role `BPDM Sharing Input Consumer` and the company's BPN identity
4. Create a `ReadAccessGateInputForSharingMember` asset with the created technical user for client credentials
5. Create a contract definition `ReadAccessGateInputForSharingMember` referencing the created asset

#### ReadAccessGateOutputForSharingMember

This offer allows a company to access a Gate API for reading the golden record output of business partner data previously shared to the golden record process.
This offer does **not** grant access to the uploaded business partner input data the golden record output is based on.

1. Create a policy of type `HasBusinessPartnerNumber` for the company's BPNL (if it not already exists)
2. Create a policy of type `AcceptPurpose` with download usage purpose for the gate (if it not already exists)
3. Create a technical user with role `BPDM Sharing Output Consumer` and the company's BPN identity
4. Create a `ReadAccessGateOutputForSharingMember` asset with the created technical user for client credentials
5. Create a contract definition `ReadAccessGateOutputForSharingMember` referencing the created asset


## Portal Configuration

This section explains how BPDM needs to operated on the [Tractus-X Portal](https://github.com/eclipse-tractusx/portal).

At the moment we assume that the Catena-X operator is also the golden record process provider.
This means the operator has the Admin role on the Portal.

The following instructions assume you are using the BPDM helm chart to deploy the BPDM services.

#### Deploy the initial golden record process components

1. Disable the own Central-IDP dependency
2. Set the authentication server to the Central-IDP instance used by the Portal
3. Override the default client secrets with the ones used in the Portal's Central-IDP
4. Expose the Pool over ingress on context path `pool` to make it available to the Portal
5. Expose the Portal Gate over ingress on context path `companies/test-company` to make it available to the Portal
6. Configure the Gate to have **no** owner as it needs to be used by multiple companies to manage their own sites and addresses

```yaml
# Helm Values Overwrite

centralidp:
  enabled: false
bpdm-pool:
  ingress:
    enabled: true
    annotations:
      nginx.ingress.kubernetes.io/rewrite-target: "/$2"
      nginx.ingress.kubernetes.io/use-regex: "true"
      nginx.ingress.kubernetes.io/x-forwarded-prefix: "/pool"
    hosts:
      - host: "bpdm-host-name"
        paths:
          - path: "/pool(/|$)(.*)"
            pathType: "ImplementationSpecific"
  applicationConfig:
    server:
      forward-headers-strategy: "FRAMEWORK"
    bpdm:
      security:
        auth-server-url: "http://central-idp-host-name/auth"
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $POOL_ORCH_CLIENT_SECRET
bpdm-gate:
  ingress:
    enabled: true
    annotations:
        nginx.ingress.kubernetes.io/rewrite-target: "/$2"
        nginx.ingress.kubernetes.io/use-regex: "true"
        nginx.ingress.kubernetes.io/x-forwarded-prefix: "/companies/test-company"
    hosts:
        - host: "bpdm-host-name"
          paths:
            - path: "/companies/test-company(/|$)(.*)"
              pathType: "ImplementationSpecific"
  applicationConfig:
    server:
      forward-headers-strategy: "FRAMEWORK"
    bpdm:
      bpn:
        owner-bpn-l: null
      tasks:
        creation:
          fromSharingMember:
            starts-as-ready: false
      security:
        auth-server-url: "http://central-idp-host-name/auth"
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $GATE_ORCH_CLIENT_SECRET
        pool:
          registration:
            client-secret: $GATE_POOL_CLIENT_SECRET
bpdm-orchestrator:
  applicationConfig:
    bpdm:
      security:
        auth-server-url: "http://central-idp-host-name/auth"
bpdm-cleaning-service-dummy:
  applicationConfig:
    bpdm:
      client:
        orchestrator:
          provider:
            issuer-uri: "https://central-idp-host-name/auth/realms/CX-Central"
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $CLEANING_DUMMY_ORCH_CLIENT_SECRET
```

#### Create BPDM marketplace apps

For giving companies access to the golden record process and/or Pool the operator needs to create appropriate EDC offers.
The EDC offers should provide access to the Pool API and Gate API.
Companies should not access the APIs directly over technical users but rather use the EDC offers.

The most important pre-requisite for creating such EDC offers are technical users which have the correct roles and BPN identities.
The Portal supports creating such technical users by creating marketplace apps that companies can subscribe to.
After the company subscribed to a service a technical user will be created automatically by the Portal containing the BPN identity of the subscribing company and the roles defined in the marketplace service.

For BPDM the operator needs to create 3 services:

1. Business Partner Members Pool App: Access to the Catena-X member golden records
2. Sharing Member Upload App: Upload company business partner data and share it to the golden record process
3. Sharing Member Download App: Download golden records for previously shared business partner data

When creating the app, the operator needs to specify with which role the technical user should be initialized after a company subscribes to it:

1. Business Partner Members Pool Service: BPDM Pool Consumer
2. Sharing Member Upload Service: BPDM Sharing Input Manager
3. Sharing Member Download Service: BPDM Sharing Output Consumer

#### Manage BPDM service subscriptions

When a company subscribes to a BPDM service the operator needs to [create an EDC](#edc-installation) offer based on the service using the created technical user for that subscription.
If the subscription is a sharing member service you additionally need to create and configure a new Gate for that sharing member:

1. Create a new technical user `BPDM Pool Sharing Consumer` in the Portal
2. Create a new technical user `BPDM Orchestrator Task Creator` in the Portal
3. Create a new Gate deployment (if it is a sharing member service)

The new Gate should be configured in the following way:

1. Disable all components except the Gate to only deploy a new Gate instance
2. Set the subscribing company's BPNL as the owner of that Gate
3. Set the authentication server to the Central-IDP instance used by the Portal
4. Set the Gate's Orchestrator and Pool client base-urls so that the Gate can find the existing components
5. Use the client credentials of the created technical users to configure the connection to the Orchestrator and Pool
6. Make sure the Gate is accessible to your EDC (either by local host name or ingress)
7. Optionally, configure whether uploaded business partner data should be automatically shared to the golden record process according to the subscribing companies wishes

```yaml
# Helm Values Overwrite

centralIdp:
  enabled: false
bpdm-pool:
  enabled: false
bpdm-orchestrator:
  enabled: false
bpdm-cleaning-service-dummy:
  enabled: false
bpdm-gate:
  applicationConfig:
    bpdm:
      bpn:
        owner-bpn-l: BPNLXXXXXXXXXXXX
      tasks:
        creation:
          fromSharingMember:
            starts-as-ready: false/true
      security:
        auth-server-url: "http://central-idp-host-name/auth"
      client:
        orchestrator:
          base-url: "orchestrator-api-url"
          registration:
            client-id: $BPDM_ORCHESTRATOR_TASK_CREATOR_CLIENT_ID
        pool:
          base-url: "pool-api-url"
          registration:
            client-id: $BPDM_POOL_SHARING_CONSUMER_CLIENT_ID
  applicationSecrets:
    bpdm:
      client:
        orchestrator:
          registration:
            client-secret: $BPDM_ORCHESTRATOR_TASK_CREATOR_CLIENT_SECRET
        pool:
          registration:
            client-secret: $BPDM_POOL_SHARING_CONSUMER_CLIENT_SECRET


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