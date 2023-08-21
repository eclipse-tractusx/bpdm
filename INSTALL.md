# Installation Instructions

This file contains information on how to configure and run the BPDM applications.

## BPDM Pool

BPDM Pool is a SpringBoot Kotlin software project managed by Maven.

To run the project first you need to install it from the parent pom, go to the root folder and run `mvn clean install`

Then depending on which module you want to start go to the module subfolder you like to run and use the following command: `mvn spring-boot:run`

### Prerequisites

* Maven
* JDK17
* Docker and Docker Compose
* Keycloak 17.0.0 (with enabled `auth` profile)

When running, the project requires a PostgreSQL database and an OpenSearch instance to be available to connect to. You can set up these dependencies manually, or you can use the provided `docker-compose` file which will start the necessary dependencies for you.

To use Docker Compose, navigate to the project directory and run the following command:

```bash
docker-compose up
```

Per default configuration the application expects postgres to run on `localhost` on port `5432`.
Opensearch needs to run on `localhost` on port `9200` on default.

You can find and edit the default configuration for the Pool in the `application.properties`,  `application-auth.properties`
files in the `resources` folder.

The REST API documentation can be accessed at <http://localhost:8080/ui/swagger-ui/index.html>.

### Profiles

The default configuration of the application is determined by the `application.properties` file.
Here you can find core application configuration such as Swagger documentation, BPN generation and Actuator.
Furthermore, here you can find the configuration for the connection to the Spring datasource (currently, developed against PostgreSQL) and Opensearch.

You can also run the project with Spring profiles to enable additional components on top of the default configuration.
Currently, the BPDM Pool offers the profile `auth`.
In order to run the application with a specific profile you can use the appropriate maven flag `Dspring.profiles.active`.

For example, the command `mvn clean spring-boot:run -Dspring.profiles.active=auth` starts the application with additional `auth` configuration enabled.

The following sections detail the configuration properties for each profile.

### Auth

`application-auth.properties` enables authorization of endpoints and configures the connection to a Keycloak instance on which the authorization relies on.
The application expects the Keycloak to run on `localhost` on port `8180`.
However, as with the Spring datasource and Opensearch connections, the connection to the Keycloak can be freely configured.
The application uses the configured auth server URL to validate incoming tokens.

For authorization purposes the application checks incoming token's permissions:

* add_company_data: For endpoints creating or updating business partner records
* view_company_data: For read-only endpoints of business partner data

The BPDM Pool looks for these permissions in the client/resource and not on the realm level.

This profile also enables/disables the login form in the auto-generated Swagger documentation.
The Swagger login uses the client specified in the property `springdoc.swagger-ui.oauth.client-id`.

### Helm Deployment

This repository contains Helm files for deploying the BPDM Pool to a Kubernetes environment.
See the [BPDM Pool Helm README](/charts/bpdm/charts/bpdm-pool/README.md) for details.

## BPDM Gate

BPDM is a SpringBoot Kotlin software project managed by Maven and can be run with the following command: `mvn clean spring-boot:run`

### Prerequisites

* Maven
* JDK17
* Connection to BPDM Pool API
* Keycloak 17.0.0 (with enabled `auth` or `pool-auth` profile)

The Gate also requires a connection to a BPDM Pool instance which is expected at `localhost` with port `8080` on default configuration.

You can find and edit the default configuration for the Gate in the `application.properties`,  `application-auth.properties`
and  `application-pool-auth.properties` files in the `resources` folder.

The REST API documentation can be accessed at <http://localhost:8081/ui/swagger-ui/index.html>.

### Profiles

The default configuration of the application is determined by the `application.properties` file.
Here you can find core application configuration such as Swagger documentation and BPDM Pool connection.

You can also run the project with Spring profiles to enable additional components on top of the default configuration.
Currently, the BPDM Gate offers the profiles `auth` and `auth-pool`.
In order to run the application with a specific profile you can use the appropriate maven flag `Dspring.profiles.active`.

For example, the command `mvn clean spring-boot:run -Dspring.profiles.active=auth` starts the application with additional `auth` configuration enabled.
You can also run several profiles at once, of course: `mvn clean spring-boot:run -Dspring.profiles.active=auth,auth-pool`.

The following sections detail the configuration properties for each profile.

### Auth

`application-auth.properties` enables authorization of endpoints and configures the connection to a Keycloak instance on which the authorization relies on.
The application expects the Keycloak to run on `localhost` on port `8180` and needs a client secret has to be submitted via environment
variable `BPDM_KEYCLOAK_SECRET`.
But keep in mind that the connection to the Keycloak can be freely configured.
The application uses the configured auth server URL to validate incoming tokens.

For authorization purposes the application checks incoming token's permissions:

* change_company_data: For endpoints adding or updating business partner data
* view_company_data: For endpoints reading the original unrefined business partner data
* view_shared_data: For endpoints reading the business partner data which has been cleaned and refined through the sharing process

The BPDM Pool looks for these permissions in the client/resource and not on the realm level.

This profile also enables/disables the login form in the auto-generated Swagger documentation.
The Swagger login uses the client specified in the property `springdoc.swagger-ui.oauth.client-id`.

### Pool-Auth

On default configuration, the BPDM Gate expects the API of the BPDM Pool to be accessible without authorization requirements.
In case the Pool instance to connect to has authorization activated, you need to activate this profile.
The file `application-pool-auth.properties` configures the oAuth2 client for connecting to a secured BPDM Pool.
Per default, the client will try to acquire a token via client credentials flow and expects the environment variable `BPDM_KEYCLOAK_SECRET` to contain the
secret for the client.

### Helm Deployment

This repository contains Helm files for deploying the BPDM Gate to a Kubernetes environment.
See the [BPDM Gate Helm README](/charts/bpdm/charts/bpdm-gate/README.md) for details.

## BPDM Bridge Dummy

BPDM is a SpringBoot Kotlin software project managed by Maven and can be run with the following command: `mvn clean spring-boot:run`

### Prerequisites

* Maven
* JDK17
* Keycloak 17.0.0 (with enabled `auth` profile)

You can find and edit the default configuration for the Bridge dummy in the `application.properties`,  `application-auth.properties` files in the `resources` folder.

The REST API documentation can be accessed at <http://localhost:8083/ui/swagger-ui/index.html>.

### Profiles

The default configuration of the application is determined by the `application.properties` file.
Here you can find core application configuration such as Swagger documentation.

You can also run the project with Spring profiles to enable additional components on top of the default configuration.
Currently, the BPDM Bridge dummy offers the profiles `auth`.
In order to run the application with a specific profile you can use the appropriate maven flag `Dspring.profiles.active`.

For example, the command `mvn clean spring-boot:run -Dspring.profiles.active=auth` starts the application with additional `auth` configuration enabled.

The following sections detail the configuration properties for each profile.

### Auth

`application-auth.properties` enables authorization of endpoints and configures the connection to a Keycloak instance on which the authorization relies on.
The application expects the Keycloak to run on `localhost` on port `8180` and needs a client secret has to be submitted via environment variable `BPDM_KEYCLOAK_SECRET`.
But keep in mind that the connection to the Keycloak can be freely configured.
The application uses the configured auth server URL to validate incoming tokens.

This profile also enables/disables the login form in the auto-generated Swagger documentation.
The Swagger login uses the client specified in the property `springdoc.swagger-ui.oauth.client-id`.

### Helm Deployment

This repository contains Helm files for deploying the BPDM Bridge Dummy to a Kubernetes environment.
See the [BPDM Bridge Dummy Helm README](/charts/bpdm/charts/bpdm-bridge-dummy/README.md) for details.

## NOTICE

This work is licensed under the [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

- SPDX-License-Identifier: Apache-2.0
- Source URL: https://github.com/eclipse-tractusx/bpdm
