# Operator View

Here you can find documentation about running, configuring and installing the BPDM services. Ensure you have at least 10 GB of free space before starting the installation.

## Running a BPDM application

### Prerequisites

* Maven
* JDK17
* Docker and Docker Compose
* Keycloak (with enabled `auth` profile)

Each BPDM application is a SpringBoot Kotlin software project managed by Maven.
To run the application first you need to install it from the parent pom. For that go to the root folder and then there are two options to run the project:

1. `mvn clean install -DskipTests` - This will install all the dependencies without need for test execution.
2. Install missing docker images used by the bridge-dummy tests `docker compose -f docker-compose.yml build`. After that run `mvn clean install`.

After installation, go to the subfolder of the application you would like to run and use the following command: `mvn spring-boot:run`

When running, the application may require a PostgreSQL database instance to be available to connect to. You can set up these dependencies manually, or you can
use the
provided [docker-compose file](../docker-compose.yml) which will start the necessary dependencies for you.

To use Docker Compose, navigate to the project's root directory and run the following command:

```bash
docker-compose up -d
```

Per default configuration the application expects postgres to run on `localhost` on port `5432`.
After the application runs you can use it's generated swagger documentation to access the API.
Caution: The default port of each application is different, thus making it possible to run them next to each other.
You can find and edit the default configuration application properties files in the `resources` folder.
For example, the REST API documentation for the Pool can be accessed at  <http://localhost:8080/ui/swagger-ui/index.html>.

## Configuring a BPDM application

The default configuration of the application is determined by the `application.yml` file in the `resources` folder.
Here you can find core application configuration such as Swagger documentation, server port, Actuator and Spring datasource (currently, developed against
PostgreSQL).

One notable configuration for applications are the BPDM clients configuration.
BPDM applications can typically connect to other BPDM applications to exchange business partner data information.
The default configuration for those clients to connect to other BPDM applications connects against the default ports of those applications.
BPDM applications connect to other BPDM applications on schedule by polling.
For that, the scheduling time needs to be configured since on default scheduling is turned off.

You can also run the project with Spring profiles that set property bundles which enable additional features of the application.
These are the available profiles:

1. auth: Each application with API has this profile. It enables authentication and authorization of the API.
2. client-auth: An application with a BPDM client has a profile to activate authentication and authorization for the access with that client.

In order to run the application with a specific profile you can use the appropriate maven flag `Dspring.profiles.active`.

For example, the command `mvn clean spring-boot:run -Dspring.profiles.active=auth` starts the application with additional `auth` configuration enabled.

The following sections detail the configuration properties for each profile.

### Auth

`application-auth.yml` enables authorization of endpoints and configures the connection to a Keycloak instance on which the authorization relies on.
The application expects the Keycloak to run on `localhost` on port `8180`.
However, as with the Spring datasource connection, the connection to the Keycloak can be freely configured.
The application uses the configured auth server URL to validate incoming tokens.

For authorization purposes the application checks incoming token's permissions with names as defined per application.
Note that the token needs to have these permissions in the client/resource level and not on the realm level.
Each application has its own set of available permissions. Please refer to the application's properties file for further details.
Each permission's default name can be overwritten if so needed.

This profile also enables/disables the oauth2 login form in the auto-generated Swagger documentation.
The Swaggger-UI offers several oauth2 authentication flows, including providing a Bearer token that will be passed to the BPDM API in the header.

### Client Auth

We already established before that BPDM applications have client configuration to connect to other BPDM applications.
If those other BPDM applications are authenticated (auth profile activated) you would need to configure an authentication logic for that connection as well.
Therefore, each BPDM application with a BPDM client also comes with a matching client auth profile.
Activating such a profile configures authentication that matches the default auth configuration of the BPDM application to connect to.

The actual name of the profile has the pattern `client-auth` where `client` is the name of the BPDM application to connect to.
For example, the BPDM pool has a profile called `orchestrator-auth`.

### Specific Application Configuration

For specific configuration options of a BPDM application please refer to the application's property files in the resource folder.
These property files count to the operation views documentation.

## Installation via Docker

Each BPDM application has an exemplary Dockerfile that can be used to install the BPDM application in a container.
Please refer to the [README](../README.md#container-images) for more information.

## Installation via Helm

This repository contains Helm files for deploying BPDM Applications to a Kubernetes environment.
See the [BPDM Chart](../charts/bpdm) for details.

## Post-Run Configuration

For the most part BPDM applications don't need to be further configured after they started.
The BPDM Pool is an exception to that rule.
The Pool offers endpoints for operators to regulate available metadata.
Metadata are supporting business partner information like legal forms, identifier types or administrative level 1 areas.
For example, by adding a range of legal forms operators are able to determine the available legal forms business partners in the Pool can have.

Typically, business partner data references metadata via technical keys.
If a technical key does not exist in the respective metadata the Pool rejects the record.

Administrative level 1 areas follows the ISO 3166-2 norm and is filled by default.
Such metadata does not need to be added by the operator.

The [use case Postman collection](postman/BPDM Tests.postman_collection.json) shows which metadata can be added and how that is done.
Refer to use cases for Operators(O):

- CL: Shows how to add available legal forms
- CIL: Shows how to add available identifier types used for legal entities
- CIA: Shows how to add available identifier types used for addresses

Please note that in the current implementation there are no endpoints to delete metadata.
Deletions would need to be done directly in the database.

## EDC Setup

While the BPDM applications currently do not connect to an EDC in order to access other APIs you may want to expose BPDM assets over an EDC.

This section details how to configure an EDC that provides access to the BPDM API for a company to share and query business partner data.
This documentation assumes that you already have running BPDM and EDC deployments.
For deploying an EDC please consult the documentation on the [EDC repository](https://github.com/eclipse-tractusx/tractusx-edc).

The general idea for using the EDC as a provider for BPDM data is to expose the BPDM Gate Endpoints each as an EDC asset.
A new asset needs to be created for each company and endpoint. As an example you can refer to the
provided [POSTMAN collection](postman/EDC_BPDM_Setup.postman_collection.json).
The collection shows examples on how to create endpoints as assets, a company policy and contract definition.

1. Asset Creation:
   In the BPDM view an EDC asset is a company-scoped endpoint address. Next to the id and description, the asset should have a company id property which is
   handy to quickly identify bundles of assets in case your EDC wants to expose assets for several BPDM Gate APIs at once. In addition to the address the asset
   should contain information on how the EDC can
   authenticate against the BPDM API. Finally, some endpoints may expect query parameters and/or bodies to be provided when accessing the asset. As a result,
   the assets should contain which of these additional resources are allowed as well as the actual method type of the endpoint.

2. Policy Creation:
   For each company that should be allowed to access the BPDM Gate the EDC needs to contain a policy that requires a consuming EDC to have the company's BPN.

3. Contract Definition Creation:
   For each company the EDC should have a contract definition assigning the company's access policy to the company's assets. The company's assets are identified
   by the ids of the assets.

After all assets, policies and contract definitions are configured a sharing company's EDC now can query its available assets and the contract under which they
are exposed.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- Source URL: https://github.com/eclipse-tractusx/bpdm